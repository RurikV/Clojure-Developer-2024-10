(ns otus-18.boundary.api.pokemon-api-adapter
  (:require [integrant.core :as ig]
            [otus-18.domain.pokemon :as domain]
            [otus-18.infrastructure.api.pokemon-api :as api]))

;; Adapter for the Pokemon API client
(defprotocol PokemonApiAdapter
  "Protocol for Pokemon API adapter operations"
  (get-pokemons [this limit lang] "Get Pokemon data with their types")
  (get-pokemon-types [this pokemon-name] "Get the types of a Pokemon by name"))

;; Implementation of the Pokemon API adapter
(defrecord PokemonApiAdapterImpl [api-client]
  PokemonApiAdapter
  
  (get-pokemons [this limit lang]
    (api/get-pokemons api-client limit lang))
  
  (get-pokemon-types [this pokemon-name]
    (api/get-pokemon-types api-client pokemon-name)))

;; Integrant initialization
(defmethod ig/init-key :otus-18.boundary.api/adapter [_ {:keys [api-client]}]
  (->PokemonApiAdapterImpl api-client))