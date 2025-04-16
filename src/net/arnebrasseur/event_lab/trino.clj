(ns net.arnebrasseur.event-lab.trino
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [honey.sql :as sql]
   [honey.sql.pg-ops :as pg]
   [next.jdbc.quoted :refer [postgres] :rename {postgres identifier}]
   [charred.api :as charred]))

(def TRINO_URL "jdbc:trino://localhost:9999/iceberg/events?user=user")

(def *ds* (jdbc/get-datasource TRINO_URL))

(defn format-qry [qry] (cond-> qry (string? qry) vector (map? qry) sql/format))
(defn exec! [qry] (jdbc/execute! *ds* (doto (format-qry qry) prn)))

(exec! "SELECT * FROM table_name")
