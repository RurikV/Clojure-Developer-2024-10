(ns otus-18.infrastructure.db.pokemon-repository
  (:require [integrant.core :as ig]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [otus-18.domain.pokemon :as domain]
            [ragtime.jdbc :as ragtime-jdbc]
            [ragtime.repl :as ragtime-repl]))

;; Helper function to get the types of a Pokemon by ID
(defn get-pokemon-types-by-id [repository pokemon-id]
  (let [results (sql/query (:db repository)
                          ["SELECT t.id, t.name, t.localized_name
                            FROM types t
                            JOIN pokemon_types pt ON t.id = pt.type_id
                            WHERE pt.pokemon_id = ?"
                           pokemon-id])]
    (map (fn [result]
           (domain/map->Type
            {:id (:types/id result)
             :name (:types/name result)
             :localized-name (:types/localized_name result)}))
         results)))

;; Implementation of the Pokemon repository
(defrecord PokemonRepositoryImpl [db migrations-config]
  domain/PokemonRepository

  (get-pokemon-by-name [this name]
    (let [result (first (sql/find-by-keys db :pokemons {:name name}))]
      (when result
        (domain/map->Pokemon
         {:id (:pokemons/id result)
          :name (:pokemons/name result)
          :url (:pokemons/url result)
          :types (get-pokemon-types-by-id this (:pokemons/id result))}))))

  (get-all-pokemons [this]
    (let [results (sql/query db ["SELECT * FROM pokemons ORDER BY name"])]
      (map (fn [result]
             (domain/map->Pokemon
              {:id (:pokemons/id result)
               :name (:pokemons/name result)
               :url (:pokemons/url result)
               :types (get-pokemon-types-by-id this (:pokemons/id result))}))
           results)))

  (save-pokemon! [this pokemon]
    (let [result (sql/insert! db :pokemons
                             {:name (:name pokemon)
                              :url (:url pokemon)}
                             {:return-keys true})]
      (domain/map->Pokemon
       {:id (:pokemons/id result)
        :name (:pokemons/name result)
        :url (:pokemons/url result)
        :types (:types pokemon)})))

  (get-type-by-name [this name]
    (let [result (first (sql/find-by-keys db :types {:name name}))]
      (when result
        (domain/map->Type
         {:id (:types/id result)
          :name (:types/name result)
          :localized-name (:types/localized_name result)}))))

  (get-all-types [this]
    (let [results (sql/query db ["SELECT * FROM types ORDER BY name"])]
      (map (fn [result]
             (domain/map->Type
              {:id (:types/id result)
               :name (:types/name result)
               :localized-name (:types/localized_name result)}))
           results)))

  (save-type! [this type]
    (let [result (sql/insert! db :types
                             {:name (:name type)
                              :localized_name (:localized-name type)}
                             {:return-keys true})]
      (domain/map->Type
       {:id (:types/id result)
        :name (:types/name result)
        :localized-name (:types/localized_name result)})))

  (associate-pokemon-with-type! [this pokemon-id type-id]
    (sql/insert! db :pokemon_types
                {:pokemon_id pokemon-id
                 :type_id type-id}))

  (get-pokemons-by-type [this type-name]
    (let [results (sql/query db
                            ["SELECT p.id, p.name, p.url
                              FROM pokemons p
                              JOIN pokemon_types pt ON p.id = pt.pokemon_id
                              JOIN types t ON pt.type_id = t.id
                              WHERE t.name = ?
                              ORDER BY p.name"
                             type-name])]
      (map (fn [result]
             (domain/map->Pokemon
              {:id (:pokemons/id result)
               :name (:pokemons/name result)
               :url (:pokemons/url result)
               :types (get-pokemon-types-by-id this (:pokemons/id result))}))
           results)))

  (get-pokemon-count-by-type [this]
    (let [results (sql/query db
                            ["SELECT t.name, COUNT(pt.pokemon_id) as count
                              FROM types t
                              JOIN pokemon_types pt ON t.id = pt.type_id
                              GROUP BY t.name
                              ORDER BY count DESC"])]
      (map (fn [result]
             {:name (:types/name result)
              :count (:count result)})
           results)))

  (get-pokemons-with-multiple-types [this]
    (let [results (sql/query db
                            ["SELECT p.id, p.name, p.url, COUNT(pt.type_id) as type_count
                              FROM pokemons p
                              JOIN pokemon_types pt ON p.id = pt.pokemon_id
                              GROUP BY p.id, p.name, p.url
                              HAVING COUNT(pt.type_id) > 1
                              ORDER BY type_count DESC"])]
      (map (fn [result]
             (domain/map->Pokemon
              {:id (:pokemons/id result)
               :name (:pokemons/name result)
               :url (:pokemons/url result)
               :types (get-pokemon-types-by-id this (:pokemons/id result))
               :type-count (:type_count result)}))
           results)))

  (get-type-distribution [this]
    (let [results (sql/query db
                            ["SELECT t.name, t.localized_name, COUNT(pt.pokemon_id) as count,
                                    ROUND(COUNT(pt.pokemon_id) * 100.0 / (SELECT COUNT(*) FROM pokemons), 2) as percentage
                             FROM types t
                             JOIN pokemon_types pt ON t.id = pt.type_id
                             GROUP BY t.name, t.localized_name
                             ORDER BY count DESC"])]
      (map (fn [result]
             {:name (:types/name result)
              :localized-name (:types/localized_name result)
              :count (:count result)
              :percentage (:percentage result)})
           results))))

;; Database migration functions
(defn load-config [db-spec]
  {:datastore  (ragtime-jdbc/sql-database db-spec)
   :migrations (ragtime-jdbc/load-resources "migrations")})

(defn migrate [db-spec]
  (ragtime-repl/migrate (load-config db-spec)))

(defn rollback [db-spec]
  (ragtime-repl/rollback (load-config db-spec)))

(defn rollback-all [db-spec]
  (let [config (load-config db-spec)]
    (dotimes [_ 100]
      (try
        (ragtime-repl/rollback config)
        (catch Exception _
          (println "All migrations have been rolled back")
          (reduced nil))))))

;; Mock implementation for testing
(defrecord PokemonRepositoryMock [mock-data]
  domain/PokemonRepository

  (get-pokemon-by-name [this name]
    (when-let [pokemon (get-in mock-data [:pokemons name])]
      (domain/map->Pokemon
       {:id 1
        :name (:name pokemon)
        :url (:url pokemon)
        :types (map (fn [type-name]
                      (domain/map->Type
                       {:id 1
                        :name type-name
                        :localized-name (get-in mock-data [:types type-name :localized_name])}))
                    (get-in mock-data [:pokemon-types name]))})))

  (get-all-pokemons [this]
    (map (fn [[name pokemon]]
           (domain/map->Pokemon
            {:id 1
             :name (:name pokemon)
             :url (:url pokemon)
             :types (map (fn [type-name]
                           (domain/map->Type
                            {:id 1
                             :name type-name
                             :localized-name (get-in mock-data [:types type-name :localized_name])}))
                         (get-in mock-data [:pokemon-types name]))}))
         (:pokemons mock-data)))

  (save-pokemon! [this pokemon]
    (domain/map->Pokemon
     {:id 1
      :name (:name pokemon)
      :url (:url pokemon)
      :types (:types pokemon)}))

  (get-type-by-name [this name]
    (when-let [type (get-in mock-data [:types name])]
      (domain/map->Type
       {:id 1
        :name (:name type)
        :localized-name (:localized_name type)})))

  (get-all-types [this]
    (map (fn [[name type]]
           (domain/map->Type
            {:id 1
             :name (:name type)
             :localized-name (:localized_name type)}))
         (:types mock-data)))

  (save-type! [this type]
    (domain/map->Type
     {:id 1
      :name (:name type)
      :localized-name (:localized-name type)}))

  (associate-pokemon-with-type! [this pokemon-id type-id]
    nil)

  (get-pokemons-by-type [this type-name]
    (let [pokemon-names (filter (fn [[_ types]]
                                 (some #(= type-name %) types))
                               (:pokemon-types mock-data))]
      (map (fn [[name _]]
             (domain/map->Pokemon
              {:id 1
               :name name
               :url (get-in mock-data [:pokemons name :url])
               :types (map (fn [type-name]
                             (domain/map->Type
                              {:id 1
                               :name type-name
                               :localized-name (get-in mock-data [:types type-name :localized_name])}))
                           (get-in mock-data [:pokemon-types name]))}))
           pokemon-names)))

  (get-pokemon-count-by-type [this]
    (let [type-counts (reduce (fn [counts [_ types]]
                               (reduce (fn [counts type]
                                         (update counts type (fnil inc 0)))
                                       counts
                                       types))
                             {}
                             (:pokemon-types mock-data))]
      (map (fn [[name count]]
             {:name name
              :count count})
           type-counts)))

  (get-pokemons-with-multiple-types [this]
    (let [pokemon-names (filter (fn [[_ types]]
                                 (> (count types) 1))
                               (:pokemon-types mock-data))]
      (map (fn [[name _]]
             (domain/map->Pokemon
              {:id 1
               :name name
               :url (get-in mock-data [:pokemons name :url])
               :types (map (fn [type-name]
                             (domain/map->Type
                              {:id 1
                               :name type-name
                               :localized-name (get-in mock-data [:types type-name :localized_name])}))
                           (get-in mock-data [:pokemon-types name]))
               :type-count (count (get-in mock-data [:pokemon-types name]))}))
           pokemon-names)))

  (get-type-distribution [this]
    (let [type-counts (reduce (fn [counts [_ types]]
                               (reduce (fn [counts type]
                                         (update counts type (fnil inc 0)))
                                       counts
                                       types))
                             {}
                             (:pokemon-types mock-data))
          total-pokemons (count (:pokemons mock-data))]
      (map (fn [[name count]]
             {:name name
              :localized-name (get-in mock-data [:types name :localized_name])
              :count count
              :percentage (float (* 100 (/ count total-pokemons)))})
           type-counts))))

;; Integrant initialization
(defmethod ig/init-key :otus-18.domain/repository [_ config]
  (if (contains? config :mock-data)
    (->PokemonRepositoryMock (:mock-data config))
    (->PokemonRepositoryImpl (:db config) (:migrations-config config))))

(defmethod ig/init-key :otus-18.infrastructure.db/pokemon-repository-mock [_ config]
  (->PokemonRepositoryMock (:mock-data config)))
