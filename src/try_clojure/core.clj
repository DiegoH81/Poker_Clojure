(ns try-clojure.core)




(+ 2 40)
(+ 2 24)

(println "Suma de:" (+ 1234 7654))

;;  7 + 3 * 4 + 5 ) / 10


(* 3 4)
(+ 7 5)
(+ *1 *2)

(/ *1 10)


(defn saludar
  ([] (saludar "TEMPORAAL"))
  ([name] (println "Holaa" name)))


(saludar)
(saludar "Amigo")


(def greet_nw (fn [name] (println "hola" name)))
(def greet #(str "Holaa " %))
(greet "weis")
(greet_nw "weis")



(defn greeting 
  ([] "Hello, World!")
  ([palabra] (str "Hello, " palabra "!"))
  ([palabra_1 palabra_2] (str palabra_1 ", "palabra_2 "!" )))



;; For testing
(assert (= "Hello, World!" (greeting)))
(assert (= "Hello, Clojure!" (greeting "Clojure")))
(assert (= "Good morning, Clojure!" (greeting "Good morning" "Clojure")))


(defn do-nothing [x] x)
(println (do-nothing 12))

(defn always-thing [& _] 100)
(println "HOLAAA " (do-nothing 122))

(get ["abc" true 99] 1)


(loop [i 1]
  (if (<= i 3)
    (do
      (println "Iteración:" i)
      (recur (inc i)))
    (println "¡Bucle terminado!")))


