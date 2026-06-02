(ns poker.client
  (:import [java.net.http HttpClient WebSocket WebSocket$Listener]
           [java.net URI]
           [java.util.concurrent CompletableFuture]))

(def buffer (StringBuilder.))

(def listener
   (reify WebSocket$Listener
     (onText [this ws data last]
       (.append buffer data)
       (when last
         (println "Received:" (str buffer))
         (.setLength buffer 0))
       (.request ws 1)
       (CompletableFuture/completedFuture nil))))

(defn -main [& args]
  (let [client (HttpClient/newHttpClient)
        ws (-> client
               (.newWebSocketBuilder)
               (.buildAsync (URI/create "ws://localhost:8080/ws") listener)
               (.join))]
    
    (loop []
      (println "Enter msg, exit to close:")
      (let [msg (read-line)]
        (when (not= msg "exit")
          (.sendText ws msg true)
          (recur)
          )))
    
    (.sendClose ws WebSocket/NORMAL_CLOSURE "bye")))