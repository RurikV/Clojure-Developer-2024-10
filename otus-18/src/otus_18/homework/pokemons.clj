(ns otus-18.homework.pokemons
  (:require [clojure.core.async :as a :refer [<! <!! >! >!! go go-loop chan close! alts! alts!! timeout]]
            [clj-http.client :as http]
            [cheshire.core :as json]))

(def base-url     "https://pokeapi.co/api/v2")
(def pokemons-url (str base-url "/pokemon"))
(def type-path    (str base-url "/type"))

(defn extract-pokemon-name [pokemon]
  (:name pokemon))

(defn extract-type-name [pokemon-type lang]
  (->> (:names pokemon-type)
       (filter (fn [type-name] (= lang (-> type-name :language :name))))
       (first)
       :name))

(defn fetch-json
  "Fetches JSON data from the given URL and parses it"
  [url]
  (-> (http/get url {:as :json})
      :body))

(defn fetch-all-types
  "Fetches all Pokemon types and creates a map of type name to localized type name"
  [lang]
  (let [types-chan (chan)
        types-result (chan)]
    (go
      (let [types-response (fetch-json type-path)
            types-urls (map :url (:results types-response))]
        (doseq [url types-urls]
          (>! types-chan url))
        (close! types-chan)))

    (go-loop [type-map {}]
      (if-let [url (<! types-chan)]
        (let [type-data (fetch-json url)
              type-name (get-in type-data [:name])
              localized-name (extract-type-name type-data lang)]
          (recur (assoc type-map type-name localized-name)))
        (do
          (>! types-result type-map)
          (close! types-result))))

    (<!! types-result)))

(defn fetch-pokemon-details
  "Fetches details for a specific Pokemon"
  [pokemon-url]
  (fetch-json pokemon-url))

(defn get-pokemons
  "Асинхронно запрашивает список покемонов и название типов в заданном языке. Возвращает map, где ключами являются
  имена покемонов (на английском английский), а значения - коллекция названий типов на заданном языке."
  [& {:keys [limit lang] :or {limit 50 lang "ja"}}]
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
