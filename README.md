# event-lab

"Lab" repository for experimenting with evented architectures and storage
approaches.

Start a REPL:

```clj
bin/launchpad --emacs
```

Start one or more services:

- `just postgres` - Runs PostgreSQL via Docker, with authorization disables (AUTH_METHOD=trust). Use user=postgres, pwd=(blank) (or any password)
- `just kafka` - Downloads and installs Kafka from release tarball
- `just kafka-docker` - Runs Kafka via Docker
- `just kafdrop` - Runs the Kafdrop web admin UI for Kafka (at [localhost:9000](https://localhost:9000))

When running Kafka from release tarball (`just kafka`) it

- downloads and runs from `download/kafka_<scala-version>-<kafka-version>`
- uses the configuration file at `config/server.properties`
- stores its data under `data/kafka_logs`

Namespaces:

```clj
;; Persistence
net.arnebrasseur.event-lab.kafka    ;; start a kafka consumer and producer
net.arnebrasseur.event-lab.postgres ;; create PostgreSQL table with JSONB column, store/fetch events
net.arnebrasseur.event-lab.xtdb     ;; various ways to run XTDB, in-memory/rocksdb/jdbc/kafka and permutations thereof
```

CLI access:

```
just psql <database>      # SQL REPL
just kafka-ls             # list consumer groups
just kafka-desc <group>   # describe consumer group
```

## Kafka Iceberg Setup

```
just tmux-panes minio kafka kafka-connect kafka-ui nessie-pg nessie
```

- [Portainer](https://localhost:9443)
- [Kafka UI](http://localhost:8080/)
- [Minio](http://localhost:9000/)
- [Nessie](http://localhost:19120)

# LICENSE

&copy; Copyright 2023-2025 Arne Brasseur

Distributed under the terms of the MIT license, see the `LICENSE` file.
