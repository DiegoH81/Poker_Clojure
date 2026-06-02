(ns poker.server
  (:require [org.httpkit.server :as hk-server]))

(def active_clients (atom #{}))

(defn register_client [socket]
  (swap! active_clients conj socket)
  (println "Cliente conectado. Total de clientes:" (count @active_clients)))

(defn delete_client [socket]
  (swap! active_clients disj socket)
  (println "Cliente desconectado. Total de clientes:" (count @active_clients)))


(defn ws_handler [req]
  (hk-server/with-channel req channel
    (register_client channel)
    (hk-server/on-close channel (fn [status]
                                  (delete_client channel)))
    (hk-server/on-receive channel (fn [msg]
                                    (println "Servidor recibió:" msg)
                                    (doseq [client @active_clients]
                                      (hk-server/send! client msg))
                                  ))))

(defn app [req]
  (if (= (:uri req) "/ws")
    (ws_handler req)
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body "Servidor activo"}))


(defn -main [& args]
  (let [server (hk-server/run-server app {:port 8080})]
    (println "Servidor corriendo en puerto 8080")
    (println "Presiona Enter para detener...")
    (read-line)
    (server)))