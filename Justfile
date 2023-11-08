postgres:
  docker run --name event-lab-postgres -e POSTGRES_HOST_AUTH_METHOD=trust -p 5432:5432 postgres

psql *args='postgres':
  docker exec -it event-lab-postgres psql -U postgres {{args}}

kafka:
  docker run -it --name event-lab-kafka-zkless -p 9092:9092 -e LOG_DIR=/tmp/logs quay.io/strimzi/kafka:latest-kafka-3.6.0-amd64 /bin/sh -c 'export CLUSTER_ID=$(bin/kafka-storage.sh random-uuid) && bin/kafka-storage.sh format -t $CLUSTER_ID -c config/kraft/server.properties && bin/kafka-server-start.sh config/kraft/server.properties'

kafka-ls:
  docker exec event-lab-kafka-zkless bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

kafka-desc group:
  docker exec event-lab-kafka-zkless bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group {{group}} --describe
