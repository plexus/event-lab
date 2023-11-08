(ns net.arnebrasseur.event-lab.postgres
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [honey.sql :as sql]
   [honey.sql.pg-ops :as pg]
   [next.jdbc.quoted :refer [postgres] :rename {postgres identifier}]
   [charred.api :as charred]))

;; Run postgres through docker:
;; $ just postgres
;; $ just psql


(def URL_ADMIN "jdbc:pgsql://localhost:5432/postgres?user=postgres")
(def URL_EVENT "jdbc:pgsql://localhost:5432/event-lab?user=postgres")
(def ^:dynamic *ds* "JDBC datasource" nil)

;;; REPL-friendly helpers for poking at the DB

(defn recreate-db!
  ([db-name]
   (recreate-db! URL_ADMIN db-name))
  ([connect-url db-name]
   (let [ds (jdbc/get-datasource connect-url)]
     (jdbc/execute! ds [(str "DROP DATABASE IF EXISTS " (identifier db-name))])
     (jdbc/execute! ds [(str "CREATE DATABASE " (identifier db-name))]))))

(defn format-qry [qry] (cond-> qry (string? qry) vector (map? qry) sql/format))
(defn exec! [qry] (jdbc/execute! *ds* (doto (format-qry qry) prn)))

(defn insert-event [event]
  {:insert-into [:event-log]
   :values [{:uuid (:uuid event (random-uuid))
             :event (charred/write-json-str (dissoc event :uuid))}]})

(def insert-event! (comp exec! insert-event))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Create database
(recreate-db! URL "event-lab")
(alter-var-root #'*ds* (constantly (jdbc/get-datasource URL_EVENT)))

;; Create table for events
(exec! {:drop-table [:if-exists :event-log]})
(exec! {:create-table :event-log
        :with-columns
        [[:uuid :uuid [:not nil]]
         [:timestamp :timestamp :default [:raw "(NOW() at time zone 'utc')"]]
         [:event :jsonb :default "{}"]]})

;; Get some values in or out
(insert-event! {:type :user-sign-up
                :username "coasterafficionado47"
                :full-name "True McIlveen"})

(exec! {:select [[:*]]
        :from [:event-log]
        })

(exec! {:select [[[:-> :event "type"]]]
        :from [:event-log]
        })
