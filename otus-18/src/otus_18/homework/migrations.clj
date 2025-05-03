(ns otus-18.homework.migrations
  (:require [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]))

(defn load-config
  "Load the migration configuration"
  [db-spec]
  {:datastore  (jdbc/sql-database db-spec)
   :migrations (jdbc/load-resources "migrations")})

(defn migrate
  "Apply all pending migrations"
  [db-spec]
  (repl/migrate (load-config db-spec)))

(defn rollback
  "Rollback the last applied migration"
  [db-spec]
  (repl/rollback (load-config db-spec)))

(defn rollback-all
  "Rollback all migrations by repeatedly calling rollback.
   This is a simple implementation that assumes there won't be more than 100 migrations."
  [db-spec]
  (let [config (load-config db-spec)]
    (dotimes [_ 100]
      (try
        (repl/rollback config)
        (catch Exception _
          (println "All migrations have been rolled back")
          (reduced nil))))))
