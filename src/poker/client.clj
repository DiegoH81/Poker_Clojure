(ns poker.client
  (:import [java.net.http HttpClient WebSocket WebSocket$Listener]
           [java.net URI]
           [java.util.concurrent CompletableFuture]))

(def listener
  (reify WebSocket$Listener
    (onText [this ws data last]
      (println "Recibido:" (str data))
      (CompletableFuture/completedFuture nil))))

(defn -main [& args]
  (let [client (HttpClient/newHttpClient)
        ws (-> client
               (.newWebSocketBuilder)
               (.buildAsync (URI/create "ws://localhost:8080/ws") listener)
               (.join))]
    
    (loop []
      (println "Introduzca un mensaje a enviar, o salir para terminar")
      (let [msg (read-line)]
        (when (not= msg "salir")
          (.sendText ws msg true)
          (Thread/sleep 5000)
          (recur)
          )))
    
    (.sendClose ws WebSocket/NORMAL_CLOSURE "bye")))