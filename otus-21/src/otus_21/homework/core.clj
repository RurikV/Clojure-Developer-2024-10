(ns otus-21.homework.core
  (:require [clojure.zip :as z]
            [clojure.string :as str]))

;; Define a structure for our file system
;; A directory is a map with :type :dir, :name, and :children
;; A file is a map with :type :file, :name, and :size

(defn parse-terminal-log
  "Parse the terminal log into a sequence of commands and outputs"
  [input]
  (let [lines (remove empty? (str/split-lines input))]
    (reduce (fn [result line]
              (if (str/starts-with? line "$")
                (conj result {:command line :output []})
                (let [last-idx (dec (count result))
                      last-cmd (get result last-idx)]
                  (assoc result last-idx 
                         (update last-cmd :output conj line)))))
            []
            lines)))

(defn build-fs-tree
  "Build a file system tree from the parsed terminal log"
  [parsed-log]
  (loop [log parsed-log
         zipper (z/zipper
                 (fn [node] (= (:type node) :dir))
                 :children
                 (fn [node children] (assoc node :children children))
                 {:type :dir, :name "/", :children []})]
    (if (empty? log)
      (z/root zipper)
      (let [{:keys [command output]} (first log)
            cmd-parts (str/split command #"\s+")]
        (case (nth cmd-parts 1)
          "cd" (let [dir (nth cmd-parts 2)]
                 (cond
                   (= dir "/") (recur (rest log) 
                                      (loop [loc zipper]
                                        (if-let [up-loc (z/up loc)]
                                          (recur up-loc)
                                          loc)))
                   (= dir "..") (recur (rest log) (if-let [up-loc (z/up zipper)] up-loc zipper))
                   :else (recur (rest log) 
                                (if-let [loc (z/down zipper)]
                                  (loop [current loc]
                                    (cond
                                      (= dir (:name (z/node current))) current
                                      (z/right current) (recur (z/right current))
                                      :else zipper))
                                  zipper))))
          "ls" (let [children (map (fn [line]
                                     (let [[info name] (str/split line #"\s+")]
                                       (if (= info "dir")
                                         {:type :dir, :name name, :children []}
                                         {:type :file, :name name, :size (Long/parseLong info)})))
                                   output)]
                 (recur (rest log) 
                        (if zipper
                          (z/edit zipper assoc :children children)
                          zipper))))))))

(defn calculate-dir-sizes
  "Calculate the size of each directory in the file system tree"
  [fs-tree]
  (letfn [(calc-size [node]
            (if (= (:type node) :file)
              node
              (let [children (map calc-size (:children node))
                    total-size (reduce + (map (fn [child]
                                               (if (= (:type child) :dir)
                                                 (:total-size child)
                                                 (:size child)))
                                             children))]
                (assoc node 
                       :children children
                       :total-size total-size))))]
    (calc-size fs-tree)))

(defn find-dirs-with-size-limit
  "Find all directories with size <= limit"
  [fs-tree limit]
  (letfn [(find-dirs [node acc]
            (if (= (:type node) :file)
              acc
              (let [new-acc (if (<= (:total-size node) limit)
                              (conj acc (:total-size node))
                              acc)]
                (reduce (fn [a child]
                          (find-dirs child a))
                        new-acc
                        (:children node)))))]
    (find-dirs fs-tree [])))

(defn sum-of-sizes [input]
  "По журналу сеанса работы в терминале воссоздаёт файловую систему
и подсчитывает сумму размеров директорий, занимающих на диске до
100000 байт (сумма размеров не учитывает случай, когда найденные
директории вложены друг в друга: размеры директорий всё так же
суммируются)."
  (let [parsed-log (parse-terminal-log input)
        fs-tree (build-fs-tree parsed-log)
        sized-tree (calculate-dir-sizes fs-tree)
        small-dirs (find-dirs-with-size-limit sized-tree 100000)]
    (reduce + small-dirs)))
