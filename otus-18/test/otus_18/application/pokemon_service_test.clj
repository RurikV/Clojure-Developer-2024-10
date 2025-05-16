(ns otus-18.application.pokemon-service-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [integrant.core :as ig]
            [otus-18.application.pokemon-service :as service]
            [otus-18.domain.pokemon :as domain]
            [otus-18.infrastructure.api.pokemon-api :as api]
            [otus-18.infrastructure.db.pokemon-repository :as repo]
            [otus-18.boundary.api.pokemon-api-adapter]
            [otus-18.boundary.db.pokemon-repository-adapter]))

;; Test system configuration
(def config
  {:otus-18.infrastructure.api/pokemon-api-mock
   {:mock-data {"pikachu" ["electric"]
                "charizard" ["fire" "flying"]}}

   :otus-18.boundary.api/adapter
   {:api-client (ig/ref :otus-18.infrastructure.api/pokemon-api-mock)}

   :otus-18.domain/repository
   {:mock-data {:pokemons {"pikachu" {:name "pikachu" :url "https://pokeapi.co/api/v2/pokemon/pikachu"}
                          "charizard" {:name "charizard" :url "https://pokeapi.co/api/v2/pokemon/charizard"}}
               :types {"electric" {:name "electric" :localized_name "Electric"}
                       "fire" {:name "fire" :localized_name "Fire"}
                       "flying" {:name "flying" :localized_name "Flying"}}
               :pokemon-types {"pikachu" ["electric"]
                              "charizard" ["fire" "flying"]}}}

   :otus-18.application/service
   {:repository (ig/ref :otus-18.domain/repository)
    :api-adapter (ig/ref :otus-18.boundary.api/adapter)}})

;; Test system state
(def system nil)

;; Setup and teardown fixtures
(defn setup-system [f]
  (alter-var-root #'system (constantly (ig/init config)))
  (f)
  (alter-var-root #'system (constantly (ig/halt! system))))

(use-fixtures :once setup-system)

;; Tests
(deftest test-get-pokemon
  (testing "Getting a Pokemon by name"
    (let [service (get system :otus-18.application/service)
          result (service/get-pokemon service "pikachu")]
      (is (= "pikachu" (:name result)))
      (is (= "https://pokeapi.co/api/v2/pokemon/pikachu" (:url result)))
      (is (= 1 (count (:types result))))
      (is (= "electric" (:name (first (:types result))))))))

(deftest test-get-pokemons-by-type
  (testing "Getting Pokemons by type"
    (let [service (get system :otus-18.application/service)
          result (service/get-pokemons-by-type service "fire")]
      (is (= 1 (count result)))
      (is (= "charizard" (:name (first result)))))))

(deftest test-get-pokemons-with-multiple-types
  (testing "Getting Pokemons with multiple types"
    (let [service (get system :otus-18.application/service)
          result (service/get-pokemons-with-multiple-types service)]
      (is (= 1 (count result)))
      (is (= "charizard" (:name (first result))))
      (is (= 2 (count (:types (first result))))))))

(deftest test-load-pokemons-to-db
  (testing "Loading Pokemon data to database"
    (let [service (get system :otus-18.application/service)
          result (service/load-pokemons-to-db! service 10 "en")]
      (is (= :success (:status result))))))
