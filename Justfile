set dotenv-load

export KAFKA_HOME := "downloads/kafka_" + env_var('KAFKA_SCALA_VERSION') + "-" + env_var('KAFKA_VERSION')

# Run PostgreSQL in a container with authorization disabled. Connect with user
# `postgres` and any (or blank) password
postgres:
  docker run --name event-lab-postgres -e POSTGRES_HOST_AUTH_METHOD=trust -p 5432:5432 postgres

# Get a `psql` REPL into the running postgres container
psql *args='postgres':
  docker exec -it event-lab-postgres psql -U postgres {{args}}

# Download Kafka if it's not there already
kafka-install:
  #!/bin/sh -x
  if [ -d "${KAFKA_HOME}" ] ; then exit 0 ; fi
  mkdir -p downloads
  cd downloads && curl -sL "https://downloads.apache.org/kafka/${KAFKA_VERSION}/kafka_${KAFKA_SCALA_VERSION}-${KAFKA_VERSION}.tgz" | tar xzf -

# Initializes Kafka's storage directory if it doesn't exist already
kafka-init-storage:
  if [ ! -d "data/kafka_logs" ] ; then "${KAFKA_HOME}/bin/kafka-storage.sh" format -t "$(${KAFKA_HOME}/bin/kafka-storage.sh random-uuid)" -c config/server.properties ; fi

# Run Kafka from release tarball, downloads and initializes as necessary
kafka:
  just kafka-install
  just kafka-init-storage
  bash -x "${KAFKA_HOME}/bin/kafka-server-start.sh" config/server.properties

# Run Kafdrop (Web UI), downloads as necessary
kafdrop:
  #!/bin/sh -x
  export KAFDROP_JAR="downloads/kafdrop-${KAFDROP_VERSION}.jar"
  if [ ! -f "$KAFDROP_JAR" ]; then
  curl -sL "https://github.com/obsidiandynamics/kafdrop/releases/download/${KAFDROP_VERSION}/kafdrop-${KAFDROP_VERSION}.jar" > "$KAFDROP_JAR"
  fi
  java --add-opens=java.base/sun.nio.ch=ALL-UNNAMED -jar "$KAFDROP_JAR"

# Run Kafka inside docker
kafka-docker:
  docker run -it --name event-lab-kafka-zkless -p 9092:9092 -e LOG_DIR=/tmp/logs quay.io/strimzi/kafka:latest-kafka-3.6.0-amd64 /bin/sh -c 'export CLUSTER_ID=$(bin/kafka-storage.sh random-uuid) && bin/kafka-storage.sh format -t $CLUSTER_ID -c config/kraft/server.properties && bin/kafka-server-start.sh config/kraft/server.properties'

kafka-docker-ls:
  docker exec event-lab-kafka-zkless bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

kafka-docker-desc group:
  docker exec event-lab-kafka-zkless bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group {{group}} --describe

local-s3:
  # S3_ACCESS_KEY=usr S3_SECRET_KEY=password s4cmd --endpoint-url=http://127.0.0.1:9000 --verbose ls
  docker run -p 9000:9000 -p 9001:9001 -v ./data/s3:/data \
    -e MINIO_ROOT_USER=usr -e MINIO_ROOT_PASSWORD=password \
    quay.io/minio/minio server /data --console-address ":9001"

nessie-install:
 #!/bin/bash
 [[ -f "downloads/nessie-quarkus-${NESSIE_VERSION}-runner.jar" ]] || curl -L "https://github.com/projectnessie/nessie/releases/download/nessie-${NESSIE_VERSION}/nessie-quarkus-${NESSIE_VERSION}-runner.jar" -o "downloads/nessie-quarkus-${NESSIE_VERSION}-runner.jar"
 [[ -f "downloads/nessie-cli-${NESSIE_VERSION}.jar" ]] || curl -L "https://github.com/projectnessie/nessie/releases/download/nessie-${NESSIE_VERSION}/nessie-cli-${NESSIE_VERSION}.jar" -o "downloads/nessie-cli-${NESSIE_VERSION}.jar"

nessie:
  just nessie-install
  java -Dquarkus.config.locations=config/nessie.properties -jar "downloads/nessie-quarkus-${NESSIE_VERSION}-runner.jar"

nessie-pg:
  docker run -i -e POSTGRES_PASSWORD=nessie -p 5432:5432 -v ./data/pg-nessie:/var/lib/postgresql/data -v ./nessie/init_postgres.sql:/docker-entrypoint-initdb.d/init.sql postgres:latest

portainer:
  docker run -p 8888:8000 -p 9443:9443 --restart=always --privileged -v /var/run/docker.sock:/var/run/docker.sock portainer/portainer-ce:2.23.0

# Requires java 17 or 21
iceberg-install:
  #!/bin/bash
  [[ -d downloads/iceberg ]] || git clone https://github.com/apache/iceberg.git --single-branch --branch=main --depth=1 downloads/iceberg
  pushd downloads/iceberg
  ./gradlew -x test -x integrationTest clean build
  popd
  mkdir -p downloads/kafka-connect-plugins
  cd downloads/kafka-connect-plugins
  unzip ../iceberg/kafka-connect/kafka-connect-runtime/build/distributions/iceberg-kafka-connect-runtime-1.9.0-SNAPSHOT.zip

iceberg-create-sink:
  curl --data @config/kafka_connect_iceberg_sink.json -H'Content-Type: application/json' -v http://localhost:8083/connectors

iceberg-delete-sink:
  curl -X DELETE -H'Content-Type: application/json' -v http://localhost:8083/connectors/events-sink

kafka-connect:
  downloads/kafka_${KAFKA_SCALA_VERSION}-${KAFKA_VERSION}/bin/connect-distributed.sh config/kafka_connect_distributed.properties
