(ns otus-18.homework.query-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [otus-18.homework.db :as db]
            [otus-18.homework.query :as query]
            [otus-18.homework.migrations :as migrations]
            [honey.sql :as hsql]))

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
        water (sql/insert! test-datasource :types
                           {:name "water"
                            :localized_name "みず"}
                           {:return-keys true})
        ;; Insert test pokemons
        pikachu (sql/insert! test-datasource :pokemons
                             {:name "pikachu"
                              :url "https://pokeapi.co/api/v2/pokemon/pikachu"}
                             {:return-keys true})
        charizard (sql/insert! test-datasource :pokemons
                               {:name "charizard"
                                :url "https://pokeapi.co/api/v2/pokemon/charizard"}
                               {:return-keys true})
        blastoise (sql/insert! test-datasource :pokemons
                               {:name "blastoise"
                                :url "https://pokeapi.co/api/v2/pokemon/blastoise"}
                               {:return-keys true})]
    ;; Associate pokemons with types
    (sql/insert! test-datasource :pokemon_types
                 {:pokemon_id (:pokemons/id pikachu)
                  :type_id (:types/id electric)})
    (sql/insert! test-datasource :pokemon_types
                 {:pokemon_id (:pokemons/id charizard)
                  :type_id (:types/id fire)})
    (sql/insert! test-datasource :pokemon_types
                 {:pokemon_id (:pokemons/id blastoise)
                  :type_id (:types/id water)})
    ;; Add a second type to charizard
    (sql/insert! test-datasource :pokemon_types
                 {:pokemon_id (:pokemons/id charizard)
                  :type_id (:types/id water)})))

;; Tests for query constructor
(deftest test-build-query
  (testing "Building a query with type-name parameter"
    (let [query (query/build-query :type-name "electric")
          sql-map (hsql/format query)
          expected-sql ["SELECT p.name, p.url FROM pokemons AS p INNER JOIN pokemon_types AS pt ON p.id = pt.pokemon_id INNER JOIN types AS t ON pt.type_id = t.id WHERE t.name = ? ORDER BY name ASC LIMIT ? OFFSET ?" "electric" 10 0]]
      (is (= expected-sql sql-map)))))

(deftest test-get-all-types
  (testing "Getting all Pokemon types"
    (insert-test-data)
    (let [result (with-redefs [db/datasource test-datasource]
                   (query/get-all-types))]
      (is (= 3 (count result)))
      (is (some #(= "electric" (:types/name %)) result))
      (is (some #(= "fire" (:types/name %)) result))
      (is (some #(= "water" (:types/name %)) result)))))

(deftest test-get-pokemons-by-type
  (testing "Getting Pokemon by type"
    (insert-test-data)
    (let [result (with-redefs [db/datasource test-datasource]
                   (query/get-pokemons-by-type "electric"))]
      (is (= 1 (count result)))
      (is (= "pikachu" (:p/name (first result)))))))

(deftest test-get-pokemon-count-by-type
  (testing "Getting Pokemon count by type"
    (insert-test-data)
    (let [result (with-redefs [db/datasource test-datasource]
                   (query/get-pokemon-count-by-type))]
      (is (= 3 (count result)))
      (is (some #(and (= "electric" (:t/name %)) (= 1 (:count %))) result))
      (is (some #(and (= "fire" (:t/name %)) (= 1 (:count %))) result))
      (is (some #(and (= "water" (:t/name %)) (= 2 (:count %))) result)))))

(deftest test-get-pokemons-with-multiple-types
  (testing "Getting Pokemon with multiple types"
    (insert-test-data)
    (let [result (with-redefs [db/datasource test-datasource]
                   (query/get-pokemons-with-multiple-types))]
      (is (= 1 (count result)))
      (is (= "charizard" (:p/name (first result))))
      (is (= 2 (:type_count (first result)))))))

(deftest test-custom-query
  (testing "Building a custom query"
    (let [query (query/custom-query
                 :select [:p.name :t.name]
                 :from [[:pokemons :p]]
                 :join [[:pokemon_types :pt] [:= :p.id :pt.pokemon_id]
                        [:types :t] [:= :pt.type_id :t.id]]
                 :where [:= :t.name "electric"]
                 :order-by [:p.name]
                 :limit 5)
          sql-map (hsql/format query)
          expected-sql ["SELECT p.name, t.name FROM pokemons AS p INNER JOIN pokemon_types AS pt ON p.id = pt.pokemon_id INNER JOIN types AS t ON pt.type_id = t.id WHERE t.name = ? ORDER BY p.name ASC LIMIT ? OFFSET ?" "electric" 5 0]]
      (is (= expected-sql sql-map)))))
