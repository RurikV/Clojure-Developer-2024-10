(ns otus-18.homework.query
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [com.github.seancorfield.honeysql :as hsql]
            [com.github.seancorfield.honeysql.helpers :as h]
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
  (execute-query (build-query :type-name type-name
                              :limit limit
                              :offset offset)))

;; Get Pokemon count by type
(defn get-pokemon-count-by-type
  "Get the number of Pokemon of each type"
  []
  (sql/query db/datasource
             ["SELECT t.name, COUNT(pt.pokemon_id) as count
               FROM types t
               JOIN pokemon_types pt ON t.id = pt.type_id
               GROUP BY t.name
               ORDER BY count DESC"]))

;; Get Pokemon with multiple types
(defn get-pokemons-with-multiple-types
  "Get Pokemon with multiple types"
  [& {:keys [limit offset]
      :or {limit 10 offset 0}}]
  (sql/query db/datasource
             ["SELECT p.name, COUNT(pt.type_id) as type_count
               FROM pokemons p
               JOIN pokemon_types pt ON p.id = pt.pokemon_id
               GROUP BY p.name
               HAVING COUNT(pt.type_id) > 1
               ORDER BY type_count DESC
               LIMIT ? OFFSET ?"
              limit offset]))

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
  (cond-> (apply h/select select)
    from (apply h/from from)
    join (apply h/join join)
    where (h/where where)
    group-by (apply h/group-by group-by)
    having (h/having having)
    order-by (h/order-by order-by)
    limit (h/limit limit)
    offset (h/offset offset)))
