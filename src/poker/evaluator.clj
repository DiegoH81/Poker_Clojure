(ns test.evaluator
    (:require [clojure.math.combinatorics :as combo]))


(defn histogram-evaluation [combinations]
    (let [filter_pattern (map #(sort > (vals %)) combinations)
        group_pattern (keys(group-by identity filter_pattern))]
        (println "Despues de filtrar -> " filter_pattern)
        (println "Despues de Agrupar -> " group_pattern)
    ))


(defn combinatories [cards-7]
    (let [all_combinations (combo/combinations cards-7 5)
        filter_combinations (map #(frequencies (map :rank %)) all_combinations)]
        (println "All combinations -> " all_combinations)
        (println "Only ranks -> " filter_combinations)
        (histogram-evaluation filter_combinations)))