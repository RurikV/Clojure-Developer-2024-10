(ns otus-18.homework.db-load-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [otus-18.homework.db :as db]
            [otus-18.homework.migrations :as migrations]))

;; Test database configuration
(def test-db-spec
  {:dbtype "postgresql"
   :dbname "pokemon_test_db"
   :host "localhost"
   :port 5432
   :user "postgres"
   :password "pwd"})

;; Test datasource
(def test-datasource (jdbc/get-datasource test-db-spec))

;; Setup and teardown fixtures
(defn setup-db [f]
  ;; Apply migrations to test database
  (migrations/migrate test-db-spec)
  (f)
  ;; Rollback all migrations
  (migrations/rollback-all test-db-spec))

(use-fixtures :once setup-db)

;; Helper function to clear all tables
(defn clear-tables []
  (jdbc/execute! test-datasource ["DELETE FROM pokemon_types"])
  (jdbc/execute! test-datasource ["DELETE FROM pokemons"])
  (jdbc/execute! test-datasource ["DELETE FROM types"]))

;; Test for loading Pokemon data
(deftest test-load-pokemons-to-db
  (testing "Loading Pokemon data to database"
    (clear-tables)
    (with-redefs [db/datasource test-datasource
                  ;; Mock the get-pokemons function to return test data
                  otus-18.homework.pokemons/get-pokemons (fn [& _]
                                                          {"pikachu" ["Electric"]
                                                           "charizard" ["Fire" "Flying"]})
                  ;; Mock the get-pokemon-types function to return test data
                  db/get-pokemon-types (fn [pokemon-name]
                                        (case pokemon-name
                                          "pikachu" ["electric"]
                                          "charizard" ["fire" "flying"]
                                          []))]
      ;; Load the Pokemon data
      (db/load-pokemons-to-db! :limit 2 :lang "en")
      
      ;; Verify that the data was loaded correctly
      (let [pikachu (db/get-pokemon-by-name "pikachu")
            charizard (db/get-pokemon-by-name "charizard")
            electric-type (db/get-type-by-name "electric")
            fire-type (db/get-type-by-name "fire")
            flying-type (db/get-type-by-name "flying")
            
            ;; Get the Pokemon-Type associations
            pikachu-types (sql/query test-datasource
                                    ["SELECT t.name FROM types t
                                      JOIN pokemon_types pt ON t.id = pt.type_id
                                      WHERE pt.pokemon_id = ?"
                                     (:pokemons/id pikachu)])
            charizard-types (sql/query test-datasource
                                      ["SELECT t.name FROM types t
                                        JOIN pokemon_types pt ON t.id = pt.type_id
                                        WHERE pt.pokemon_id = ?"
                                       (:pokemons/id charizard)])]
        
        ;; Verify that the Pokemon were saved
        (is (= "pikachu" (:pokemons/name pikachu)))
        (is (= "charizard" (:pokemons/name charizard)))
        
        ;; Verify that the types were saved
        (is (= "electric" (:types/name electric-type)))
        (is (= "fire" (:types/name fire-type)))
        (is (= "flying" (:types/name flying-type)))
        
        ;; Verify that the Pokemon-Type associations were saved
        (is (= 1 (count pikachu-types)))
        (is (= "electric" (:types/name (first pikachu-types))))
        
        (is (= 2 (count charizard-types)))
        (is (some #(= "fire" (:types/name %)) charizard-types))
        (is (some #(= "flying" (:types/name %)) charizard-types))))))