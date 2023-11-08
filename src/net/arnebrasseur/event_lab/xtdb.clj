(ns net.arnebrasseur.event-lab.xtdb
  (:require
   [clojure.java.io :as io]
   [xtdb.api :as xt]))

;; Index, documents, tx-log all in memory

(def node (xt/start-node {}))

;; Rocksdb
;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; [dixit docs] RocksDB is often used as the data store for XTDB’s query
;; indices, but can also be used as a transaction log and/or document store in
;; single node clusters.
;; https://v1-docs.xtdb.com/storage/rocksdb/

(def node (xt/start-node
           {:xtdb/index-store
            {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                        :db-dir (io/file "/tmp/event_lab_rocksdb/index")}}

            :xtdb/document-store
            {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                        :db-dir (io/file "/tmp/event_lab_rocksdb/documents")}}

            :xtdb/tx-log
            {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                        :db-dir (io/file "/tmp/event_lab_rocksdb/tx-log")}}}))

;; See docs about memory configuration considerations.


;; JDBC
;;;;;;;;;;;;;;;;;;;;;;;;;
;; [dixit docs] XTDB nodes can use JDBC databases to store their transaction
;; logs and/or document stores.
;; https://v1-docs.xtdb.com/storage/jdbc/

;; Prerequisites:
;; $ just postgres

(require '[net.arnebrasseur.event-lab.postgres :as pg])

(pg/recreate-db! "xtdb-tx-log")
(pg/recreate-db! "xtdb-documents")

(def node (xt/start-node
           {:xtdb/tx-log
            {:xtdb/module 'xtdb.jdbc/->tx-log
             :connection-pool {:dialect {:xtdb/module 'xtdb.jdbc.psql/->dialect}
                               :pool-opts {}
                               :db-spec {:jdbcUrl "jdbc:pgsql://localhost:5432/xtdb-tx-log?user=postgres"}}
             :poll-sleep-duration (java.time.Duration/ofSeconds 1)}

            :xtdb/document-store
            {:xtdb/module 'xtdb.jdbc/->document-store
             :connection-pool {:dialect {:xtdb/module 'xtdb.jdbc.psql/->dialect}
                               :pool-opts {  }
                               :db-spec {:jdbcUrl "jdbc:pgsql://localhost:5432/xtdb-documents?user=postgres"} }}

            ;; JDBC can't be index-store, could be in memory, or e.g. RocksDB, LMDB
            #_:xtdb/index-store
            #_{:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                          :db-dir (io/file "/tmp/event_lab_pg+rocks/index")}}}))

;; see docs for how to share a single HikariCP connection pool for tx-log and doc-store


;; Kafka
;;;;;;;;;;;;;;;;;;;;;;;;;
;; [dixit docs] When using XTDB at scale it is recommended to use multiple XTDB
;; nodes connected via a Kafka cluster. Use of multiple nodes provides availability
;; and Kafka itself provides strong fault-tolerance guarantees. Kafka can be used
;; for XTDB’s transaction log and document store components.

;; [dixit docs] Kafka’s document store requires a copy of the documents kept
;; locally for random access - these can be stored in a KV store like RocksDB or
;; LMDB.
;;
;; For this reason, unless you want to keep both transactions and documents on
;; Kafka (e.g. for high write throughput, or for architectural simplicity), we now
;; recommend a different document store implementation - JDBC or S3, for example.
;;
;; (The Kafka transaction log does not have this requirement)

;; -> seems Kafka is mainly recommended for the tx-log

;; Prerequisites:
;; $ just kafka
;; $ just postgres

(require 'net.arnebrasseur.event-lab.postgres)

(net.arnebrasseur.event-lab.postgres/recreate-db! "jdbc:pgsql://localhost:5432/postgres?user=postgres" "xtdb-kafka-documents")

(def node
  (xt/start-node
   {:xtdb/tx-log {:xtdb/module 'xtdb.kafka/->tx-log
                  :kafka-config {:bootstrap-servers "localhost:9092"}
                  :tx-topic-opts {:topic-name "xtdb-tx-log"}
                  :poll-wait-duration (java.time.Duration/ofSeconds 1)}

    :xtdb/document-store
    {:xtdb/module 'xtdb.jdbc/->document-store
     :connection-pool {:dialect {:xtdb/module 'xtdb.jdbc.psql/->dialect}
                       :pool-opts {  }
                       :db-spec {:jdbcUrl "jdbc:pgsql://localhost:5432/xtdb-kafka-documents?user=postgres"} }}

    :xtdb/index-store
    {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                :db-dir (io/file "/tmp/event_lab_kafka+rocks/index")}}}))
