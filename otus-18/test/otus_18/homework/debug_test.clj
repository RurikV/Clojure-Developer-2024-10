(ns otus-18.homework.debug-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [otus-18.homework.migrations :as migrations]))

(def test-db-spec
  {:dbtype "postgresql"
   :dbname "pokemon_test_db"
   :host "localhost"
   :port 5432
   :user "postgres"
   :password "pwd"})

(defn setup-db [f]
  (migrations/migrate test-db-spec)
  (f)
  (migrations/rollback-all test-db-spec))

(use-fixtures :once setup-db)

(deftest test-insert-return-keys
  (testing "Testing the structure of returned keys from insert!"
    (let [datasource (jdbc/get-datasource {:dbtype "postgresql"
                                          :dbname "pokemon_test_db"
                                          :host "localhost"
                                          :port 5432
                                          :user "postgres"
                                          :password "pwd"})
          result (try
                   (sql/insert! datasource :types
                               {:name "test-type"
                                :localized_name "test-localized"}
                               {:return-keys true})
                   (catch Exception e
                     {:error (.getMessage e)}))]
      (println "Result type:" (type result))
      (println "Result keys:" (keys result))
      (println "Full result:" result)
      (is true))))
