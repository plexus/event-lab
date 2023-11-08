(ns net.arnebrasseur.event-lab.kafka
  (:require
   [clojure.core.protocols :as clojure-proto]
   [clojure.datafy :refer [datafy]])
  (:import
   (java.time Duration)
   (org.apache.kafka.clients.consumer ConsumerRecord KafkaConsumer)
   (org.apache.kafka.clients.producer KafkaProducer ProducerRecord)))

;; docker run -it --name kafka-zkless -p 9092:9092 -e LOG_DIR=/tmp/logs quay.io/strimzi/kafka:latest-kafka-3.6.0-amd64 /bin/sh -c 'export CLUSTER_ID=$(bin/kafka-storage.sh random-uuid) && bin/kafka-storage.sh format -t $CLUSTER_ID -c config/kraft/server.properties && bin/kafka-server-start.sh config/kraft/server.properties'

;; docker exec $CONTAINER bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
;; docker exec $CONTAINER bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group test --describe


(extend-protocol clojure-proto/Datafiable
  ConsumerRecord
  (datafy [r]
    {;;:serializedValueSize (.serializedValueSize r)
     :partition (.partition r)
     :offset (.offset r)
     :timestamp (.timestamp r)
     ;;:timestampType (.timestampType r)
     :key (.key r)
     :value (.value r)
     :headers (.headers r)
     :leaderEpoch (.leaderEpoch r)
     :topic (.topic r)
     ;;:serializedKeySize (.serializedKeySize r)
     }))

(def props
   {"bootstrap.servers" "localhost:9092"
    "group.id" "test"
    "default.topic" "magic-topic"
    "enable.auto.commit" "true"
    "max.poll.records" "10"
    "auto.commit.interval.ms" "1000"
    "key.serializer" "org.apache.kafka.common.serialization.StringSerializer"
    "value.serializer" "org.apache.kafka.common.serialization.StringSerializer"
    "key.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
    "value.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"})

(def consumer (KafkaConsumer. props))
(.subscribe consumer ["my-topic" "magic-topic"])

(.start
 (Thread.
  #(while true
     (doseq [record (.poll consumer (Duration/ofMillis 1000))]
       (print ".")
       (let [r (datafy record)]
         (if (= "stop" (:value r))
           (throw (Exception.))))
       (def rrr record)
       (prn (datafy record)))
     )))

(seq
      (.headers rrr))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def producer (KafkaProducer. props))

(dotimes [i 100]
  (.send producer (ProducerRecord. "magic-topic" (str (random-uuid)) (str i))))


;; (.close producer)
