(ns otus-18.homework.query
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [honey.sql :as hsql]
            [honey.sql.helpers :as h]
            [otus-18.homework.db :as db]))

;; Query constructor for Pokemon data
(defn build-query
  "Build a query for Pokemon data based on the given parameters"
  [& {:keys [type-name limit offset order-by]
      :or {limit 10 offset 0 order-by :name}}]
  (cond-> (h/select :p.name :p.url)
    true (h/from [:pokemons :p])
    true (h/join [:pokemon_types :pt] [:= :p.id :pt.pokemon_id]
                [:types :t] [:= :pt.type_id :t.id])
    type-name (h/where [:= :t.name type-name])
    true (h/order-by [order-by])
    true (h/limit limit)
    true (h/offset offset)))

;; Execute a query and return the results
(defn execute-query
  "Execute a query and return the results"
  [query]
  (let [sql-map (hsql/format query)
        results (jdbc/execute! db/datasource sql-map)]
    results))

;; Get all Pokemon types
(defn get-all-types
  "Get all Pokemon types"
  []
  (sql/query db/datasource
             ["SELECT * FROM types ORDER BY name"]))

;; Get Pokemon by type
(defn get-pokemons-by-type
  "Get Pokemon by type"
  [type-name & {:keys [limit offset]
                :or {limit 10 offset 0}}]
  (let [results (sql/query db/datasource
                          ["SELECT p.name, p.url
                            FROM pokemons p
                            JOIN pokemon_types pt ON p.id = pt.pokemon_id
                            JOIN types t ON pt.type_id = t.id
                            WHERE t.name = ?
                            ORDER BY p.name
                            LIMIT ? OFFSET ?"
                           type-name limit offset])]
    (map (fn [row]
           {:p/name (:pokemons/name row)
            :p/url (:pokemons/url row)})
         results)))

;; Get Pokemon count by type
(defn get-pokemon-count-by-type
  "Get the number of Pokemon of each type"
  []
  (let [results (sql/query db/datasource
                          ["SELECT t.name, COUNT(pt.pokemon_id) as count
                            FROM types t
                            JOIN pokemon_types pt ON t.id = pt.type_id
                            GROUP BY t.name
                            ORDER BY count DESC"])]
    (map (fn [row]
           {:t/name (:types/name row)
            :count (:count row)})
         results)))

;; Get Pokemon with multiple types
(defn get-pokemons-with-multiple-types
  "Get Pokemon with multiple types"
  [& {:keys [limit offset]
      :or {limit 10 offset 0}}]
  (let [results (sql/query db/datasource
                          ["SELECT p.name, COUNT(pt.type_id) as type_count
                            FROM pokemons p
                            JOIN pokemon_types pt ON p.id = pt.pokemon_id
                            GROUP BY p.name
                            HAVING COUNT(pt.type_id) > 1
                            ORDER BY type_count DESC
                            LIMIT ? OFFSET ?"
                           limit offset])]
    (map (fn [row]
           {:p/name (:pokemons/name row)
            :type_count (:type_count row)})
         results)))

;; Get Pokemon types distribution
(defn get-type-distribution
  "Get the distribution of Pokemon types"
  []
  (sql/query db/datasource
             ["SELECT t.name, t.localized_name, COUNT(pt.pokemon_id) as count,
                      ROUND(COUNT(pt.pokemon_id) * 100.0 / (SELECT COUNT(*) FROM pokemons), 2) as percentage
               FROM types t
               JOIN pokemon_types pt ON t.id = pt.type_id
               GROUP BY t.name, t.localized_name
               ORDER BY count DESC"]))

;; Custom query constructor
(defn custom-query
  "Build a custom query based on the given parameters"
  [& {:keys [select from join where group-by having order-by limit offset]
      :or {select [:p.name]
           from [[:pokemons :p]]
           limit 10
           offset 0}}]
  (let [query (apply h/select select)
        query (if from (apply h/from query from) query)
        query (if join
                (let [join-pairs (partition 2 join)]
                  (reduce (fn [q [table condition]]
                            (h/join q table condition))
                          query
                          join-pairs))
                query)
        query (if where (h/where query where) query)
        query (if group-by (apply h/group-by query group-by) query)
        query (if having (h/having query having) query)
        query (if order-by (h/order-by query order-by) query)
        query (if limit (h/limit query limit) query)
        query (if offset (h/offset query offset) query)]
    query))
