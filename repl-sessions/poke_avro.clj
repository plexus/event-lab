(ns poke-avro
  (:require
   [clojure.java.io :as io])
  (:import
   org.apache.avro.Schema
   org.apache.avro.Schema$Parser
   org.apache.avro.file.DataFileReader
   org.apache.avro.file.DataFileWriter
   org.apache.avro.generic.GenericData$Record
   org.apache.avro.generic.GenericDatumReader
   org.apache.avro.generic.GenericDatumWriter
   org.apache.avro.generic.GenericRecord))

(def schema
  "{\"namespace\": \"example.avro\",
 \"type\": \"record\",
 \"name\": \"User\",
 \"fields\": [
     {\"name\": \"name\", \"type\": \"string\"},
     {\"name\": \"favorite_number\",  \"type\": [\"int\", \"null\"]},
     {\"name\": \"favorite_color\", \"type\": [\"string\", \"null\"]}
 ]
}")

(def schema-parsed
  (.parse (Schema$Parser.) schema))

(with-open [writer (DataFileWriter. (GenericDatumWriter. schema-parsed))]
  (.create writer schema-parsed (io/file "/tmp/test.avro"))
  (.append
   writer
   (doto (GenericData$Record. schema-parsed)
     (.put "name" "Arne")
     (.put "favorite_number" (int 3))
     )
   ))


(let [record (.next
              (DataFileReader. (io/file "/tmp/test.avro") (GenericDatumReader. schema-parsed)))]
  (reduce
   (fn [acc field]
     (let [pos (.pos field)
           name (.name field)]
       (assoc acc name (.get record pos))))
   {}
   (.getFields schema-parsed)))
;; => {"name" #object[org.apache.avro.util.Utf8 0x89d5fde "Arne"],
;;     "favorite_number" 3,
;;     "favorite_color" nil}
