(ns poker.server
  (:require [org.httpkit.server :as hk-server]))

(def active_clients (atom #{}))

(defn register_client [socket]
  (swap! active_clients conj socket)
  (println "Total clients:" (count @active_clients)))

(defn delete_client [socket]
  (swap! active_clients disj socket)
  (println "Total clients:" (count @active_clients)))


(defn ws_handler [req]
  (hk-server/with-channel req channel
    (register_client channel)
    (hk-server/on-close channel (fn [status]
                                  (delete_client channel)))
    (hk-server/on-receive channel (fn [msg]
                                    (println "Receieved:" msg)
                                    (doseq [client @active_clients]
                                      (hk-server/send! client msg))
                                  ))))

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