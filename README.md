# event-lab

"Lab" repository for experimenting with evented architectures and storage
approaches.

Start a REPL:

```clj
bin/launchpad --emacs
```

Start one or more docker containers:

```clj
just postgres
just kafka
```

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

# LICENSE

&copy; Copyright 2023 Arne Brasseur

Distributed under the terms of the MIT license, see the `LICENSE` file.
