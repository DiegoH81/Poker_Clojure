(ns test.game_test
    (:require [clojure.string :as str][test.cards :as cartas][test.evaluator :as eval]))


(defn crear-jugador [name]
    (agent {:nombre name
            :hand []
            :tokens 5000
            :action ""
            :pot 0
            :fold false}))

(defn print-jugadores [jugador1 jugador2 jugador3 jugador4]
    (println)
    (println "Jugador 1 " @jugador1)
    (println)
    (println "Jugador 2 " @jugador2)
    (println)
    (println "Jugador 3 " @jugador3)
    (println)
    (println "Jugador 4 " @jugador4)
    (println))


(defn add-cards [state cards]
    (update state :hand into cards))

(defn convert-format [player]
    (let [card (nth (:hand player) 1)
        rank (case (:rank card)
            14 "A"
            11 "J"
            12 "Q"
            13 "K"
            (str (- (:rank card) 1)))
        family (case (:tipo card)
                :corazones "h"
                :diamantes "d"
                :treboles "c"
                :picas "s")]
    (str rank family)))



(defn delivered-cards [jugador1 jugador2 jugador3 jugador4 shuffle_cards]
    (let [cartas_p1 (take 2 shuffle_cards)
        shuff_restantes (drop 2 shuffle_cards)
        cartas_p2 (take 2 shuff_restantes)
        shuff_restantes (drop 2 shuff_restantes)
        cartas_p3 (take 2 shuff_restantes)
        shuff_restantes (drop 2 shuff_restantes)
        cartas_p4 (take 2 shuff_restantes)]
        (send jugador1 add-cards cartas_p1)
        (send jugador2 add-cards cartas_p2)
        (send jugador3 add-cards cartas_p3)
        (send jugador4 add-cards cartas_p4)
        (await jugador1 jugador2 jugador3 jugador4)))

(def jugador1 (crear-jugador "Alan"))
(def jugador2 (crear-jugador "Juan"))
(def jugador3 (crear-jugador "Paquito"))
(def jugador4 (crear-jugador "Joaquin"))

(defn add-gamecard [carta filter_cards]
    (send jugador1 update :hand into carta)
    (send jugador2 update :hand into carta)
    (send jugador3 update :hand into carta)
    (send jugador4 update :hand into carta)
    (await jugador1 jugador2 jugador3 jugador4)
    (drop 1 filter_cards))

(defn game-loop [jugador1 jugador2 jugador3 jugador4]
    (let [players [jugador1 jugador2 jugador3 jugador4]
        shuffle_cards (shuffle cartas/game-cards)]
        (delivered-cards jugador1 jugador2 jugador3 jugador4 shuffle_cards)
        (loop [deck (drop 8 shuffle_cards)
            rounds 0]
            (println "Ronda:" rounds)
            (print-jugadores jugador1 jugador2 jugador3 jugador4)

            (if (= rounds 3)
                (println "Acabo el juego")
                (let [n (if (= rounds 0) 3 1)
                    cartas (take n deck)
                    rest-deck (drop n deck)]
                    (add-gamecard cartas players)
                    (recur rest-deck (inc rounds)))))
        (println (eval/combinatories (@jugador1 :hand)))))
        


(defn -main "Hello" [& args]
    (game-loop jugador1 jugador2 jugador3 jugador4)
    (println "Despues de loop:")
    (print-jugadores jugador1 jugador2 jugador3 jugador4)
    ;;(println (convert-format @jugador1)) Para parsear las cartas de un agente
    (shutdown-agents))
    ;;(println(combo/combinations [1 2 3 4 5] 3)) para combinatoria

