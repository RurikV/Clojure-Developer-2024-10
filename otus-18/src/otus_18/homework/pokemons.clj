(ns otus-18.homework.pokemons
  (:require [clojure.core.async :as a :refer [<! <!! >! >!! go go-loop chan close! alts! alts!! timeout thread pipeline pipeline-async]]
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
  "Fetches JSON data from the given URL and parses it asynchronously.
   Returns a channel that will receive the parsed JSON data."
  [url]
  (let [result-chan (chan 1)]
    (thread
      (try
        (let [response (http/get url {:as :json})]
          (>!! result-chan (:body response)))
        (catch Exception e
          (println "Error fetching data from" url ":" (.getMessage e))
          (>!! result-chan nil))
        (finally
          (close! result-chan))))
    result-chan))

(defn fetch-all-types
  "Fetches all Pokemon types and creates a map of type name to localized type name.
   Returns a channel that will receive the map of type names to localized names."
  [lang]
  (let [result-chan (chan 1)]
    (go
      (let [types-response-chan (fetch-json type-path)
            types-response (<! types-response-chan)]
        (if types-response
          (let [types-urls (map :url (:results types-response))
                type-map (atom {})
                remaining (atom (count types-urls))
                process-complete (chan)]
            
            ;; Process each type URL
            (doseq [url types-urls]
              (go
                (let [type-data-chan (fetch-json url)
                      type-data (<! type-data-chan)]
                  (when type-data
                    ;; Extract type name from URL
                    (let [type-name (last (clojure.string/split url #"/"))
                          localized-name (extract-type-name type-data lang)]
                      (swap! type-map assoc type-name localized-name)))
                  
                  ;; Check if all types have been processed
                  (when (zero? (swap! remaining dec))
                    (>! process-complete @type-map)))))
            
            ;; Wait for all types to be processed
            (let [result (<! process-complete)]
              (>! result-chan result)
              (close! result-chan)))
          
          ;; Handle error case when types-response is nil
          (do
            (>! result-chan {})
            (close! result-chan)))))
    
    result-chan))

(defn fetch-pokemon-details
  "Fetches details for a specific Pokemon.
   Returns a channel that will receive the Pokemon details."
  [pokemon-url]
  (fetch-json pokemon-url))

(defn get-pokemons-async
  "Асинхронно запрашивает список покемонов и название типов в заданном языке.
   Возвращает канал, в который будут поступать пары [имя-покемона типы-покемона] по мере их обработки.
   Имена покемонов на английском, а типы - на указанном языке."
  [& {:keys [limit lang] :or {limit 50 lang "ja"}}]
  (let [pokemon-list-url (str pokemons-url "?limit=" limit)
        result-chan (chan)
        output-chan (chan)]
    
    ;; Start a pipeline to process results
    (go
      (loop []
        (when-let [pokemon-data (<! result-chan)]
          (>! output-chan pokemon-data)
          (recur)))
      (close! output-chan))
    
    ;; Main processing pipeline
    (go
      ;; First, fetch all types to create a reference map
      (let [types-map-chan (fetch-all-types lang)
            types-map (<! types-map-chan)]
        
        ;; Now fetch the list of pokemons
        (let [pokemon-list-chan (fetch-json pokemon-list-url)
              pokemon-list (<! pokemon-list-chan)]
          
          (if pokemon-list
            ;; Process each pokemon
            (let [pokemons (:results pokemon-list)]
              
              ;; Create a processing pipeline for each pokemon
              (doseq [pokemon pokemons]
                (let [pokemon-name (:name pokemon)
                      pokemon-url (or (:url pokemon) (str pokemons-url "/" pokemon-name))]
                  
                  ;; Process this pokemon asynchronously
                  (go
                    (let [details-chan (fetch-json pokemon-url)
                          pokemon-details (<! details-chan)]
                      
                      (if pokemon-details
                        (let [pokemon-types (map #(get-in % [:type :name]) (:types pokemon-details))
                              localized-types (mapv #(get types-map %) pokemon-types)]
                          (>! result-chan [pokemon-name localized-types]))
                        
                        ;; Handle error case
                        (>! result-chan [pokemon-name []])))))))
            
            ;; Handle error case when pokemon-list is nil
            (close! result-chan)))))
    
    ;; Return the output channel
    output-chan))

(defn get-pokemons
  "Асинхронно запрашивает список покемонов и название типов в заданном языке.
   Для обратной совместимости с тестами, блокирует выполнение и возвращает map,
   где ключами являются имена покемонов, а значения - коллекция названий типов на заданном языке.
   
   Для неблокирующего использования рекомендуется использовать get-pokemons-async."
  [& {:keys [limit lang timeout-ms] :or {limit 50 lang "ja" timeout-ms 10000}}]
  (let [result-chan (get-pokemons-async :limit limit :lang lang)
        timeout-chan (timeout timeout-ms)
        result (atom {})]
    
    ;; Collect all results from the channel
    (loop []
      (let [[val port] (alts!! [result-chan timeout-chan])]
        (cond
          ;; Timeout occurred
          (= port timeout-chan)
          @result
          
          ;; Got a value from the result channel
          val
          (let [[name types] val]
            (swap! result assoc name types)
            (recur))
          
          ;; Channel closed or nil value
          :else
          @result)))
    
    ;; Return the collected results
    @result))