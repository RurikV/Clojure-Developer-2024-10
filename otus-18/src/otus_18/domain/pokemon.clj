(ns otus-18.domain.pokemon)

;; Domain entities

(defrecord Pokemon [id name url types])

(defrecord Type [id name localized-name])

;; Domain protocols

(defprotocol PokemonRepository
  "Protocol for Pokemon repository operations"
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