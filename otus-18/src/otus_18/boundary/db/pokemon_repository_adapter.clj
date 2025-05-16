(ns otus-18.boundary.db.pokemon-repository-adapter
  (:require [integrant.core :as ig]
            [otus-18.domain.pokemon :as domain]))

;; Adapter for the Pokemon repository
(defprotocol PokemonRepositoryAdapter
  "Protocol for Pokemon repository adapter operations"
  (get-pokemon-by-name [this name] "Get a Pokemon by name")
  (get-all-pokemons [this] "Get all Pokemons")
  (save-pokemon! [this pokemon] "Save a Pokemon")
  (get-type-by-name [this name] "Get a type by name")
  (get-all-types [this] "Get all types")
  (save-type! [this type] "Save a type")
  (associate-pokemon-with-type! [this pokemon-id type-id] "Associate a Pokemon with a type")
  (get-pokemons-by-type [this type-name] "Get Pokemons by type")
  (get-pokemon-count-by-type [this] "Get the number of Pokemons of each type")
  (get-pokemons-with-multiple-types [this] "Get Pokemons with multiple types")
  (get-type-distribution [this] "Get the distribution of Pokemon types"))

;; Implementation of the Pokemon repository adapter
(defrecord PokemonRepositoryAdapterImpl [repository]
  PokemonRepositoryAdapter
  
  (get-pokemon-by-name [this name]
    (domain/get-pokemon-by-name repository name))
  
  (get-all-pokemons [this]
    (domain/get-all-pokemons repository))
  
  (save-pokemon! [this pokemon]
    (domain/save-pokemon! repository pokemon))
  
  (get-type-by-name [this name]
    (domain/get-type-by-name repository name))
  
  (get-all-types [this]
    (domain/get-all-types repository))
  
  (save-type! [this type]
    (domain/save-type! repository type))
  
  (associate-pokemon-with-type! [this pokemon-id type-id]
    (domain/associate-pokemon-with-type! repository pokemon-id type-id))
  
  (get-pokemons-by-type [this type-name]
    (domain/get-pokemons-by-type repository type-name))
  
  (get-pokemon-count-by-type [this]
    (domain/get-pokemon-count-by-type repository))
  
  (get-pokemons-with-multiple-types [this]
    (domain/get-pokemons-with-multiple-types repository))
  
  (get-type-distribution [this]
    (domain/get-type-distribution repository)))

;; Integrant initialization
(defmethod ig/init-key :otus-18.boundary.db/adapter [_ {:keys [repository]}]
  (->PokemonRepositoryAdapterImpl repository))