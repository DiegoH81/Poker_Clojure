(ns test.evaluator
    (:require [clojure.math.combinatorics :as combo]))

(defn histogram-cases [freq]
    (cond 
        (= freq '(4 1)) {:rank-eval 8 :type "Four of a kind"}
        (= freq '(3 2)) {:rank-eval 7 :type "Full house"}
        (= freq '(3 1 1)) {:rank-eval 4 :type "Three of a kind"}
        (= freq '(2 2 1)) {:rank-eval 3 :type "Two pair"}
        (= freq '(2 1 1 1)) {:rank-eval 2 :type "One pair"}
        ;;Evaluation for flushes()
        :else {:rank-eval 1 :type "High card"}))

(defn histogram-evaluation [combinations all-combinations]
    (let [format_hands (map #(let [histogram (sort > (vals %1))]
                            {:original-hand %2
                            :histogram histogram
                            :evaluation-type (histogram-cases histogram)
                            :check-draws (vec(sort >(map :rank %2)))})
                        combinations
                        all-combinations)
        greater-rank (apply max (map #(:rank-eval (:evaluation-type %1)) format_hands))
        best-hands (filter #(= greater-rank (:rank-eval (:evaluation-type %1))) format_hands)
        best-hand-player (last(sort-by :check-draws best-hands))]
        (println "MEJOR MANO A BUSCAR " greater-rank)
        (println "Las mejores manos" best-hands)
        (println "Mejor mano del jugador final" best-hand-player)
    ))


(defn combinatories [cards-7]
    (let [all_combinations (combo/combinations cards-7 5)
        filter_combinations (map #(frequencies (map :rank %1)) all_combinations)]
        (println "All combinations -> " all_combinations)
        (println "Only ranks -> " filter_combinations)
        (histogram-evaluation filter_combinations all_combinations)))