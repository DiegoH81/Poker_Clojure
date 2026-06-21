(ns poker.server
  (:require [poker.logicaApuestas :as apuestas]
            [poker.cards :as cartas]
            [poker.evaluator :as eval]
            [poker.pokerPrincipal :as motor]
            [org.httpkit.server :as hk-server]
            [clojure.data.json :as json]
            [poker.utils :as utils]))

(def active_clients (atom {}))

(def mesa-ag (agent {:players []
                     :pot 0
                     :community-cards []
                     :turn 0
                     :current-bet 0
                     :ronda 0
                     :armada? false
                     :ganador nil}))

(def rank-str {2 "2" 3 "3" 4 "4" 5 "5" 6 "6" 7 "7" 8 "8" 9 "9"
               10 "10" 11 "J" 12 "Q" 13 "K" 14 "A"})
(def tipo-str {:corazones "h" :diamantes "d" :treboles "c" :picas "s"})

(defn carta-str [c] (str (rank-str (:rank c)) (tipo-str (:tipo c))))

(defn jugador-json [j viewer-id]
  {:id (:id @j)
   :nombre (:name @j)
   :dinero (:money @j)
   :cartas (if (= (:id @j) viewer-id)
             (mapv carta-str (:hand @j))
             (vec (repeat (count (:hand @j)) "back")))})

(defn estado-json [viewer-id]
  (if (not (:armada? @mesa-ag))
    {:jugador_id_actual viewer-id
     :esperando true
     :mensaje (str "Esperando jugadores (" (count (:players @mesa-ag)) "/4)")}
    (let [players (:players @mesa-ag)
          jugador-actual (nth players (:turn @mesa-ag))]
      {:jugador_id_actual viewer-id
       :turno_id (:id @jugador-actual)
       :jugadores (mapv #(jugador-json % viewer-id) players)
       :mesa {:pot (:pot @mesa-ag)
              :cartas (mapv carta-str (:community-cards @mesa-ag))}
       :opciones (if (= viewer-id (:id @jugador-actual))
                   (motor/obtener-opciones jugador-actual mesa-ag)
                   [])
       :ganador (:ganador @mesa-ag)})))

(defn broadcast-estado []
  (doseq [[socket id] @active_clients]
    (hk-server/send! socket (json/write-str (estado-json id)))))

(defn agregar-jugador-a-mesa [estado nombre id]
  (update estado :players conj (apuestas/crear-jugador nombre id)))



(defn register_client [socket nombre]
  (let [new_id (count (:players @mesa-ag))]
    (send mesa-ag agregar-jugador-a-mesa nombre new_id)
    (await mesa-ag)
    (swap! active_clients assoc socket new_id)
    (println "Conectado:" nombre "(" (count (:players @mesa-ag)) "/4 )")
    (when (= 4 (count (:players @mesa-ag)))
      (send mesa-ag assoc :armada? true)
      (await mesa-ag)
      (println "¡Mesa armada!")
      (motor/barajar-cartas-iniciales mesa-ag))))

(defn delete_client [socket]
  (swap! active_clients dissoc socket)
  (println "Total clients:" (count @active_clients)))

(defn on-mensaje-recibido [player-id action value]
  (when (:armada? @mesa-ag)
    (let [cantidad (when value (Integer/parseInt value))]
      (motor/procesar-accion-jugador mesa-ag action cantidad))))

(defn ws_handler [req]
  (hk-server/with-channel req channel
    (let [params (utils/parse_query_string (:query-string req))
          nombre (:nombre params)]
      (register_client channel nombre))

    (broadcast-estado)

    (hk-server/on-close channel (fn [status]
                                  (delete_client channel)))
    (hk-server/on-receive channel (fn [msg]
                                    (let [parts (clojure.string/split msg #" ")
                                          action (first parts)
                                          value (second parts)
                                          player-id (get @active_clients channel)]

                                      (on-mensaje-recibido player-id action value)

                                      (broadcast-estado))))))

(defn app [req]
  (if (= (:uri req) "/ws")
    (ws_handler req)
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body "Active server"}))


(defn -main [& args]
  (let [server (hk-server/run-server app {:port 8080})]
    (println "Running on port 8080")
    (println "Enter to stop...")
    (read-line)
    (server)))