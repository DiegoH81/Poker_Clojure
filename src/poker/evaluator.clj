(ns poker.evaluator
    (:require [clojure.math.combinatorics :as combo]))

(defn is-straight [hand]
    (let [ranks (sort (distinct (map :rank hand)))]
        (or
        (= ranks [2 3 4 5 14])
        (and (= 5 (count ranks)) (= 4 (- (last ranks) (first ranks)))))))
    
(defn royal-flush [hand]
    (= (sort (map :rank hand))
        [10 11 12 13 14]))

(defn straight-high-card [hand]
    (let [ranks (sort (map :rank hand))]
        (if (= ranks [2 3 4 5 14]) 5 (last ranks))))

(defn is-flush [hand]
    (= 1 (count (group-by :tipo hand))))

(defn histogram-cases [freq check-draws hand]
    (cond 
        (= freq '(4 1)) {:rank-eval 8 :type "Four of a kind" :check-draw (vec check-draws)}
        (= freq '(3 2)) {:rank-eval 7 :type "Full house" :check-draw (vec check-draws)}
        (= freq '(3 1 1)) {:rank-eval 4 :type "Three of a kind" :check-draw (vec check-draws)}
        (= freq '(2 2 1)) {:rank-eval 3 :type "Two pair" :check-draw (vec check-draws)}
        (= freq '(2 1 1 1)) {:rank-eval 2 :type "One pair" :check-draw (vec check-draws)}
        (royal-flush hand) {:rank-eval 10 :type "Royal Flush" :check-draw [14]}
        (and (is-flush hand) (is-straight hand)) {:rank-eval 9 :type "Straight Flush" :check-draw [(straight-high-card hand)]}
        (is-flush hand) {:rank-eval 6 :type "Flush" :check-draw (vec (sort > (map :rank hand)))}
        (is-straight hand) {:rank-eval 5 :type "Straight" :check-draw [(straight-high-card hand)]}
        :else {:rank-eval 1 :type "High card" :check-draw (vec check-draws)}))

(defn tiebreak [hand]
    (:check-draw (:evaluation-type hand)))

(defn histogram-evaluation [combinations all-combinations]
    (let [format_hands (map #(let [histogram (sort > (vals %1))]
                            {:original-hand %2
                            :histogram histogram
                            :evaluation-type (histogram-cases histogram (map key (sort-by val > %1)) %2) })
                        combinations
                        all-combinations)
        greater-rank (apply max (map #(:rank-eval (:evaluation-type %1)) format_hands))
        best-hands (filter #(= greater-rank (:rank-eval (:evaluation-type %1))) format_hands)
        best-hand-player (last (sort-by tiebreak best-hands))]
        (println "Mejor mano del jugador final" best-hand-player)
        best-hand-player
    ))
