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
