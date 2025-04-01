(ns net.arnebrasseur.event-lab.nessie
  (:import
   (java.net URI)
   (org.apache.iceberg PartitionSpec Schema SortOrder)
   (org.apache.iceberg.catalog TableIdentifier)
   (org.apache.iceberg.nessie NessieCatalog)
   (org.apache.iceberg.types Types Types$LongType Types$NestedField Types$StringType Types$TimestampType)
   (org.projectnessie.client NessieClientBuilder)
   (org.projectnessie.client.api NessieApiV2)))

(def api (-> (NessieClientBuilder/createClientBuilder nil nil)
             (.withUri (URI/create "http://localhost:19120/api/v2") )
             (.build NessieApiV2)))


(def catalog (NessieCatalog.))

(.initialize catalog "nessie"
             {"uri" "http://localhost:19120/api/v2"
              "s3.endpoint" "http://127.0.0.1:9000"
              "s3.path-style-access" "true"
              "s3.access-key-id" "usr"
              "s3.secret-access-key" "password"
              "s3.region" "us-west-1"
              "io-impl" "org.apache.iceberg.aws.s3.S3FileIO"
              "ref" "main"
              "warehouse" "s3://bucket/warehouse"
              "catalog-impl" "org.apache.iceberg.nessie.NessieCatalog"})


;; api.getAllReferences()
;; .stream()
;; .map(Reference::getName)
;; .forEach(System.out::println);
;; (NessieCatalog.)


(System/setProperty "aws.region" "us-west-1")

(.createTable
 catalog
 (TableIdentifier/of (into-array String ["db_name" "table_name"]))
 (Schema.
  [(Types$NestedField/required 1 "id" (Types$LongType/get))
   (Types$NestedField/optional 2 "name" (Types$StringType/get))
   (Types$NestedField/optional 3 "created_at" (Types$TimestampType/withZone))])
 (PartitionSpec/unpartitioned)
 #_(SortOrder/unsorted))
