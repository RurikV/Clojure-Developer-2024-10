(ns otus-18.infrastructure.api.pokemon-api
  (:require [integrant.core :as ig]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.core.async :as a :refer [<! <!! >! >!! go go-loop chan close! alts! alts!! timeout]]))

;; API client for Pokemon operations
(defprotocol PokemonApiClient
  "Protocol for Pokemon API client operations"
  (get-pokemons [this limit lang] "Get Pokemon data with their types")
  (get-pokemon-types [this pokemon-name] "Get the types of a Pokemon by name"))

;; Helper functions
(defn fetch-json
  "Fetches JSON data from the given URL and parses it"
  [url]
  (-> (http/get url {:as :json})
      :body))

(defn extract-type-name [pokemon-type lang]
  (->> (:names pokemon-type)
       (filter (fn [type-name] (= lang (-> type-name :language :name))))
       (first)
       :name))

;; Implementation of the Pokemon API client
(defrecord PokemonApiClientImpl [base-url pokemons-url type-path]
  PokemonApiClient

  (get-pokemons [this limit lang]
    (let [pokemon-list-url (str pokemons-url "?limit=" limit)
          result-chan (chan)
          types-map-chan (chan)]

      ;; First, fetch all types to create a reference map
      (go
        (let [types-response (fetch-json type-path)
              types-map (atom {})]
          ;; Process each type to get its localized name
          (doseq [type-info (:results types-response)]
            (let [type-name (:name type-info)
                  type-url (:url type-info)]
              (try
                (let [type-data (fetch-json type-url)
                      localized-name (extract-type-name type-data lang)]
                  (swap! types-map assoc type-name localized-name))
                (catch Exception e
                  (println "Error fetching type data for" type-name ":" (.getMessage e))))))

          ;; Put the types map on the channel
          (>! types-map-chan @types-map)))

      ;; Get the types map
      (let [types-map (<!! types-map-chan)]
        ;; Now fetch the list of pokemons
        (go
          (let [pokemon-list-response (fetch-json pokemon-list-url)
                pokemons (:results pokemon-list-response)]
            ;; Create a channel for each pokemon and collect results
            (let [pokemon-chans (map 
                                (fn [pokemon]
                                  (let [c (chan)]
                                    (go
                                      (let [pokemon-name (:name pokemon)
                                            pokemon-url (or (:url pokemon) (str pokemons-url "/" pokemon-name))
                                            pokemon-details (try
                                                            (fetch-json pokemon-url)
                                                            (catch Exception e
                                                              (println "Error fetching pokemon details for" pokemon-name ":" (.getMessage e))
                                                              {:types []}))
                                            pokemon-types (map #(get-in % [:type :name]) (:types pokemon-details))
                                            localized-types (mapv #(get types-map %) pokemon-types)]
                                        (>! c [pokemon-name localized-types])))
                                    c))
                                pokemons)
                  result (loop [chans pokemon-chans
                                result {}]
                          (if (seq chans)
                            (let [[v c] (alts!! chans)]
                              (if v
                                (let [[name types] v]
                                  (recur (remove #(= % c) chans) (assoc result name types)))
                                (recur (remove #(= % c) chans) result)))
                            result))]
              (>! result-chan result)))
          (close! result-chan)))

      ;; Wait for the result
      (<!! result-chan)))

  (get-pokemon-types [this pokemon-name]
    (let [pokemon-url (str pokemons-url "/" pokemon-name)
          pokemon-details (try
                            (fetch-json pokemon-url)
                            (catch Exception e
                              (println "Error fetching pokemon details for" pokemon-name ":" (.getMessage e))
                              {:types []}))
          pokemon-types (map #(get-in % [:type :name]) (:types pokemon-details))]
      pokemon-types)))

;; Mock implementation for testing
(defrecord PokemonApiClientMock [mock-data]
  PokemonApiClient
  (get-pokemons [this limit lang]
    mock-data)

  (get-pokemon-types [this pokemon-name]
    (get mock-data pokemon-name [])))

;; Integrant initialization
(defmethod ig/init-key :otus-18.infrastructure.api/client [_ config]
  (->PokemonApiClientImpl (:base-url config) (:pokemons-url config) (:type-path config)))

(defmethod ig/init-key :otus-18.infrastructure.api/pokemon-api-mock [_ config]
  (->PokemonApiClientMock (:mock-data config)))
