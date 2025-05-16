(ns otus-18.homework.db-test
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

;; Helper function to insert test data
(defn insert-test-data []
  (clear-tables)
  ;; Insert test types
  (let [electric (sql/insert! test-datasource :types
                              {:name "electric"
                               :localized_name "でんき"}
                              {:return-keys true})
        fire (sql/insert! test-datasource :types
                          {:name "fire"
                           :localized_name "ほのお"}
                          {:return-keys true})
        ;; Insert test pokemons
        pikachu (sql/insert! test-datasource :pokemons
                             {:name "pikachu"
                              :url "https://pokeapi.co/api/v2/pokemon/pikachu"}
                             {:return-keys true})
        charizard (sql/insert! test-datasource :pokemons
                               {:name "charizard"
                                :url "https://pokeapi.co/api/v2/pokemon/charizard"}
                               {:return-keys true})]
    ;; Associate pokemons with types
    (sql/insert! test-datasource :pokemon_types
                 {:pokemon_id (:pokemons/id pikachu)
                  :type_id (:types/id electric)})
    (sql/insert! test-datasource :pokemon_types
                 {:pokemon_id (:pokemons/id charizard)
                  :type_id (:types/id fire)})))

;; Tests for database operations
(deftest test-save-type
  (testing "Saving a type to the database"
    (clear-tables)
    (let [type-name "water"
          localized-name "みず"
          result (with-redefs [db/datasource test-datasource]
                   (db/save-type! type-name localized-name))]
      (is (= type-name (:types/name result)))
      (is (= localized-name (:types/localized_name result))))))

(deftest test-get-type-by-name
  (testing "Getting a type by name"
    (insert-test-data)
    (let [type-name "electric"
          result (with-redefs [db/datasource test-datasource]
                   (db/get-type-by-name type-name))]
      (is (= type-name (:types/name result)))
      (is (= "でんき" (:types/localized_name result))))))

(deftest test-save-pokemon
  (testing "Saving a pokemon to the database"
    (clear-tables)
    (let [pokemon-name "bulbasaur"
          pokemon-url "https://pokeapi.co/api/v2/pokemon/bulbasaur"
          result (with-redefs [db/datasource test-datasource]
                   (db/save-pokemon! pokemon-name pokemon-url))]
      (is (= pokemon-name (:pokemons/name result)))
      (is (= pokemon-url (:pokemons/url result))))))

(deftest test-get-pokemon-by-name
  (testing "Getting a pokemon by name"
    (insert-test-data)
    (let [pokemon-name "pikachu"
          result (with-redefs [db/datasource test-datasource]
                   (db/get-pokemon-by-name pokemon-name))]
      (is (= pokemon-name (:pokemons/name result)))
      (is (= "https://pokeapi.co/api/v2/pokemon/pikachu" (:pokemons/url result))))))
