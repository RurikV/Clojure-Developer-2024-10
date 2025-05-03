(ns otus-18.homework.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [otus-18.homework.migrations :as migrations]
            [otus-18.homework.pokemons :as pokemons]
            [clj-http.client :as http]))

;; Function to fetch Pokemon details and extract types
(defn get-pokemon-types
  "Get the types of a Pokemon by name"
  [pokemon-name]
  (let [pokemon-url (str pokemons/pokemons-url "/" pokemon-name)
        pokemon-details (try
                          (-> (http/get pokemon-url {:as :json})
                              :body)
                          (catch Exception e
                            (println "Error fetching pokemon details for" pokemon-name ":" (.getMessage e))
                            {:types []}))
        pokemon-types (map #(get-in % [:type :name]) (:types pokemon-details))]
    pokemon-types))

;; Database connection configuration
(def db-spec
  {:dbtype "postgresql"
   :dbname "pokemon_db"
   :host "localhost"
   :port 5432
   :user "postgres"
   :password "postgres"})

;; Create a connection pool
(def datasource (jdbc/get-datasource db-spec))

;; Initialize the database (apply migrations)
(defn init-db!
  "Initialize the database by applying migrations"
  []
  (migrations/migrate db-spec))

;; Reset the database (rollback all migrations and apply them again)
(defn reset-db!
  "Reset the database by rolling back all migrations and applying them again"
  []
  (migrations/rollback-all db-spec)
  (migrations/migrate db-spec))

;; Save a type to the database
(defn save-type!
  "Save a type to the database"
  [type-name localized-name]
  (sql/insert! datasource :types
               {:name type-name
                :localized_name localized-name}
               {:return-keys true}))

;; Get a type by name
(defn get-type-by-name
  "Get a type by name"
  [type-name]
  (first (sql/find-by-keys datasource :types {:name type-name})))

;; Save a pokemon to the database
(defn save-pokemon!
  "Save a pokemon to the database"
  [pokemon-name pokemon-url]
  (sql/insert! datasource :pokemons
               {:name pokemon-name
                :url pokemon-url}
               {:return-keys true}))

;; Get a pokemon by name
(defn get-pokemon-by-name
  "Get a pokemon by name"
  [pokemon-name]
  (first (sql/find-by-keys datasource :pokemons {:name pokemon-name})))

;; Associate a pokemon with a type
(defn associate-pokemon-with-type!
  "Associate a pokemon with a type"
  [pokemon-id type-id]
  (sql/insert! datasource :pokemon_types
               {:pokemon_id pokemon-id
                :type_id type-id}))

;; Load pokemon data from API and save to database
(defn load-pokemons-to-db!
  "Load pokemon data from API and save to database"
  [& {:keys [limit lang] :or {limit 50 lang "ja"}}]
  (let [pokemons-data (pokemons/get-pokemons :limit limit :lang lang)]
    (doseq [[pokemon-name type-names] pokemons-data]
      (let [pokemon-url (str pokemons/pokemons-url "/" pokemon-name)
            pokemon (or (get-pokemon-by-name pokemon-name)
                        (save-pokemon! pokemon-name pokemon-url))
            pokemon-types (get-pokemon-types pokemon-name)]
        (doseq [[i type-name] (map-indexed vector pokemon-types)]
          (let [localized-name (nth type-names i nil)
                type-data (or (get-type-by-name type-name)
                              (save-type! type-name localized-name))]
            (associate-pokemon-with-type! (:id pokemon) (:id type-data))))))))
