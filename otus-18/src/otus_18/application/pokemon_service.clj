(ns otus-18.application.pokemon-service
  (:require [integrant.core :as ig]
            [otus-18.domain.pokemon :as domain]))

;; Application service for Pokemon operations
(defprotocol PokemonService
  "Protocol for Pokemon service operations"
  (get-pokemon [this name] "Get a Pokemon by name")
  (get-all-pokemons [this] "Get all Pokemons")
  (get-pokemons-by-type [this type-name] "Get Pokemons by type")
  (get-pokemon-count-by-type [this] "Get the number of Pokemons of each type")
  (get-pokemons-with-multiple-types [this] "Get Pokemons with multiple types")
  (get-type-distribution [this] "Get the distribution of Pokemon types")
  (load-pokemons-to-db! [this limit lang] "Load Pokemon data from API and save to database"))

;; Implementation of the Pokemon service
(defrecord PokemonServiceImpl [repository api-adapter]
  PokemonService
  (get-pokemon [this name]
    (domain/get-pokemon-by-name repository name))

  (get-all-pokemons [this]
    (domain/get-all-pokemons repository))

  (get-pokemons-by-type [this type-name]
    (domain/get-pokemons-by-type repository type-name))

  (get-pokemon-count-by-type [this]
    (domain/get-pokemon-count-by-type repository))

  (get-pokemons-with-multiple-types [this]
    (domain/get-pokemons-with-multiple-types repository))

  (get-type-distribution [this]
    (domain/get-type-distribution repository))

  (load-pokemons-to-db! [this limit lang]
    (let [pokemons-data (require 'otus-18.boundary.api.pokemon-api-adapter)
          get-pokemons-fn (resolve 'otus-18.boundary.api.pokemon-api-adapter/get-pokemons)
          pokemons-data (get-pokemons-fn api-adapter limit lang)]
      (doseq [[pokemon-name type-names] pokemons-data]
        (let [pokemon-url (str "https://pokeapi.co/api/v2/pokemon/" pokemon-name)
              pokemon (or (domain/get-pokemon-by-name repository pokemon-name)
                          (domain/save-pokemon! repository (domain/map->Pokemon {:name pokemon-name :url pokemon-url})))
              pokemon-types (require 'otus-18.boundary.api.pokemon-api-adapter)
              get-pokemon-types-fn (resolve 'otus-18.boundary.api.pokemon-api-adapter/get-pokemon-types)
              pokemon-types (get-pokemon-types-fn api-adapter pokemon-name)
              pokemon-id (:id pokemon)]
          (doseq [[i type-name] (map-indexed vector pokemon-types)]
            (let [localized-name (nth type-names i nil)
                  type-data (or (domain/get-type-by-name repository type-name)
                                (domain/save-type! repository (domain/map->Type {:name type-name :localized-name localized-name})))
                  type-id (:id type-data)]
              (domain/associate-pokemon-with-type! repository pokemon-id type-id))))))
    {:status :success :message "Pokemon data loaded"}))

;; Integrant initialization
(defmethod ig/init-key :otus-18.application/service [_ {:keys [repository api-adapter]}]
  (->PokemonServiceImpl repository api-adapter))
