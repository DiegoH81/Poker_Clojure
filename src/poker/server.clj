(ns poker.server
  (:require [org.httpkit.server :as hk-server]
            [clojure.data.json :as json]))

(def active_clients (atom {}))

(def game-state (atom {:mesa {:cartas [] :pot 0}
                       :jugadores []
                       :turno_id 0
                       :opciones ["Retirar" "Pasar" "Apostar" "Igualar"]}))


(defn register_client [socket]
  (let [new_id (count @active_clients)]
    (swap! active_clients assoc socket new_id)
    (swap! game-state (fn [state]
                        (update state :jugadores conj {:id new_id
                                                       :nombre (str "Jugador-" new_id)
                                                       :dinero 1000
                                                       :cartas []})))))

(defn delete_client [socket]
  (swap! active_clients dissoc socket)
  (println "Total clients:" (count @active_clients)))

(defn get_new_turn [current_turn]
  (let [new_turn (inc current_turn)]
    (if (>= new_turn (count @active_clients))
      0
      new_turn)))

(defn process_action [current_state player-id action value]

  (let [new_turn (get_new_turn (:turno_id current_state))]

    (cond
      (= action "Retirarse") (println "Jugador" player-id "se retiró")
      (= action "Pasar") (println "Jugador" player-id "pasó")
      (= action "Apostar") (println "Jugador" player-id "apostó" value)

      :else

      (println "Acción desconocida"))

    (assoc current_state :turno_id new_turn)))

(defn ws_handler [req]
  (hk-server/with-channel req channel
    (register_client channel)

    (doseq [socket (keys @active_clients)]
      (let [id-del-socket (get @active_clients socket)
            json-especifico (json/write-str (merge @game-state {:jugador_id_actual id-del-socket}))]
        (hk-server/send! socket json-especifico)))

    (hk-server/on-close channel (fn [status]
                                  (delete_client channel)))
    (hk-server/on-receive channel (fn [msg]
                                    (let [parts (clojure.string/split msg #" ")
                                          action (first parts)
                                          value (second parts)
                                          player-id (get @active_clients channel)]



                                      (swap! game-state #(process_action % player-id action value))

                                      (let [json-broadcast (json/write-str @game-state)]

                                        (doseq [socket (keys @active_clients)]
                                          (let [id-del-socket (get @active_clients socket)
                                                json-especifico (json/write-str (merge @game-state {:jugador_id_actual id-del-socket}))]
                                            (hk-server/send! socket json-especifico)))))))))

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