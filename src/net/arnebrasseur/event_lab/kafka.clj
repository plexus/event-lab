(ns net.arnebrasseur.event-lab.kafka
  (:require
   [charred.api :as charred]
   [clojure.core.protocols :as clojure-proto]
   [clojure.datafy :refer [datafy]])
  (:import
   (java.time Duration)
   (org.apache.kafka.clients.consumer ConsumerRecord KafkaConsumer)
   (org.apache.kafka.clients.producer KafkaProducer ProducerRecord)))

;; Run kafka inside docker:
;; $ just kafka

;; List consumer groups:
;; $ just kafka-ls

;; Describe group:
;; $ just kafka-desc test

;; Kafka tl;dr:

;; Kafka broker arranges transactions between producers and consumers
;; Kafka cluster = collection of one or more brokers (three or more for replication)
;; Kafka Topic = persisted immutable stream of event (event stream at rest)
;; Consumer Group = consumers that work together to process a topic in parallel (doing same "logical job")
;; Each topic is stored in one or more *Partitions* (comparable to shards)
;; - Part of the persistent log for a topic that is stored on a broker
;; - Can be replicated across brokers
;;   - Configurable topic replication factor
;;   - For each partition a leader is elected (rest = followers)
;; - Configurable how many partitions a topic has, min=1
;; - number of partitions in the topic determines the maximum number of consumers in a single group that consumes that partition
;;   - #partitions <= #consumer-group
;;   - Partition can not be processed by more than one consumer in a given group
;;   - But many groups can consume a topic (and thus partition)
;; - Order is only guaranteed within a partition
;; - Which events goes to which partition controllable by "event-key"
;; Kafka maintains offset per [partition, consumer-group] pair
;; - last successfully processed message
;; - allows picking up work later
;; - stored in internal topic __consumer_offsets-<partition-id>

(extend-protocol clojure-proto/Datafiable
  ConsumerRecord
  (datafy [r]
    {
     ;; (String) The topic this record is received from (never null)
     :topic (.topic r)
     ;; (int) The partition of the topic this record is received from.
     ;; Incrementing integer, unique within the cluster (not just within the
     ;; topic)
     :partition (.partition r)
     ;; (long) The offset of this record in the corresponding Kafka partition. Incrementing integer.
     :offset (.offset r)
     ;; (long) The timestamp of this record, in milliseconds elapsed since unix epoch.
     :timestamp (.timestamp r)
     ;; The key of the record, if one exists (null is allowed)
     :key (.key r)
     ;; The record contents (blob)
     :value (.value r)
     ;; The headers of the record - similar to HTTP headers, can contain things
     ;; like span/trace ids. Any "metadata" that is useful for routing messages.
     :headers (.headers r)
     ;; Optional leader epoch of the record (may be empty for legacy record formats)
     ;; Incremented by the controller each time it elects a replica to be the new leader
     ;; :leaderEpoch (.leaderEpoch r)

     ;; (Enum) The timestamp type - NO_TIMESTAMP_TIME / CREATE_TIME / LOG_APPEND_TIME
     ;;:timestampType (.timestampType r)

     ;; The length of the serialized uncompressed key in bytes
     ;;:serializedValueSize (.serializedValueSize r)

     ;; The length of the serialized uncompressed value in bytes
     ;;:serializedKeySize (.serializedKeySize r)
     }))

(def props
  {"bootstrap.servers" "localhost:9092"
   ;; consumer group
   "group.id" "test"
   "default.topic" "events"
   "max.poll.records" "10"
   ;; commit consumer offset = last message processed for given partition
   "enable.auto.commit" "true"
   "auto.commit.interval.ms" "1000"
   "key.serializer" "org.apache.kafka.common.serialization.StringSerializer"
   "value.serializer" "org.apache.kafka.common.serialization.StringSerializer"
   "key.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
   "value.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"})

(def props {
            "bootstrap.servers" "cojrkvfb4g10p5c9aitg.any.eu-central-1.mpx.prd.cloud.redpanda.com:9092"
            "security.protocol" "SASL_SSL"
            "sasl.mechanism" "SCRAM-SHA-512"
            "sasl.jaas.config" (str "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"" username "\" password=\"" password "\";")

            "group.id" "arne-test"
            "default.topic" "prod.events.user-actions"
            "max.poll.records" "10"
            "enable.auto.commit" "true"
            "auto.commit.interval.ms" "1000"

            "key.serializer" "org.apache.kafka.common.serialization.StringSerializer"
            "value.serializer" "org.apache.kafka.common.serialization.StringSerializer"
            "key.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
            "value.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
            })

(def consumer (KafkaConsumer. props))
(.subscribe consumer ["magic-topic"])

(defonce all-records (atom []))

(.start
 (Thread.
  #(while true
     (doseq [record (.poll consumer (Duration/ofMillis 1000))]
       (print ".") (flush)
       (let [r (datafy record)]
         (if (= "stop" (:value r))
           (throw (Exception. "stop requested"))))
       (swap! all-records conj record)
       #_
       (prn (datafy record))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(dotimes [i 100]
  (.send producer (ProducerRecord. "events" (str (random-uuid)) (str "{\"bar\": " i "}") #_(str i))))
(def producer (KafkaProducer. props))

(dotimes [i 100]
  @(.send producer (ProducerRecord. "prod.events.user-actions"
                                    (str (random-uuid))
                                    (charred/write-json-str
                                     {:type "arne_test"
                                      :id (str "msg-" i)}))))

@(.send producer (ProducerRecord. "prod.events.user-actions"
                                  (str (random-uuid))
                                  (charred/write-json-str
                                   {:ts (System/nanoTime)
                                    :type "arne_test"}))
        )

;; (.close producer)

(ns net.arnebrasseur.event-lab.kafka
  (:require
   [charred.api :as charred]
   [clojure.core.protocols :as clojure-proto]
   [clojure.datafy :refer [datafy]])
  (:import
   (java.time Duration)
   (org.apache.kafka.clients.consumer ConsumerRecord KafkaConsumer)
   (org.apache.kafka.clients.producer KafkaProducer ProducerRecord)))

(def password "...")

(def props
  {"bootstrap.servers" "cojrkvfb4g10p5c9aitg.any.eu-central-1.mpx.prd.cloud.redpanda.com:9092"
   "security.protocol" "SASL_SSL"
   "sasl.mechanism" "SCRAM-SHA-512"
   "sasl.jaas.config" (str "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"" "gcloud-ingestor" "\" password=\""  password "\";")

   "group.id" "arne-test"
   "default.topic" "prod.events.user-actions"
   "max.poll.records" "10"
   "enable.auto.commit" "true"
   "auto.commit.interval.ms" "1000"

   "key.serializer" "org.apache.kafka.common.serialization.StringSerializer"
   "value.serializer" "org.apache.kafka.common.serialization.StringSerializer"
   "key.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
   "value.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"})

(def producer (KafkaProducer. props))

(dotimes [i 100]
  @(.send producer (ProducerRecord. "prod.events.user-actions"
                                    (str (random-uuid))
                                    (charred/write-json-str
                                     {:type "arne_test"
                                      :id (str "msg-" i)}))))
