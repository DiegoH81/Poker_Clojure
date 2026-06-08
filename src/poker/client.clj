(ns poker.client
  (:require [org.httpkit.server :as hk-server]
            [clojure.data.json :as json])
  
  (:import [java.net.http HttpClient WebSocket WebSocket$Listener]
           [java.net URI]
           [java.util.concurrent CompletableFuture]))

(def estado-juego (atom {}))
(def ws_atom (atom nil))

(defn parse_query_string [qs]
  (if (nil? qs) {}
      (->> (clojure.string/split qs #"&")
           (map #(clojure.string/split % #"="))
           (map (fn [[k v]] [(keyword k) v]))
           (into {}))))

(def listener
   (reify WebSocket$Listener
     (onText [this ws data last]
       (try
         (let [json-str (str data)
               json-data (json/read-str json-str :key-fn keyword)]
           (reset! estado-juego json-data)
           (println "Estado actualizado correctamente"))
         (catch Exception e
           (println "ERROR AL PARSEAR JSON: " e)))
       (.request ws 1)
       (CompletableFuture/completedFuture nil))))


(defn serve-static [path]
  (let [resource (clojure.java.io/resource (subs path 1))]
    (if resource
      {:status 200
       :headers {"Content-Type" (cond
                                  (clojure.string/ends-with? path ".css")  "text/css"
                                  (clojure.string/ends-with? path ".js")   "application/javascript"
                                  (clojure.string/ends-with? path ".png")  "image/png"
                                  (clojure.string/ends-with? path ".otf")  "font/otf"
                                  (clojure.string/ends-with? path ".ttf")  "font/ttf"
                                  :else "application/octet-stream")}
       :body (clojure.java.io/input-stream resource)}
      {:status 404 :body "Not found"})))


(defn web_handler [req]
  (let [uri (:uri req)]
    (case uri
      "/" {:status 200
           :headers {"Content-Type" "text/html"}
           :body (slurp (clojure.java.io/resource "index.html"))}
      "/api/messages" {:status 200
                       :headers {"Content-Type" "application/json"}
                       :body (clojure.data.json/write-str @estado-juego)}
      "/api/decision" (let [params (parse_query_string (:query-string req))
                            accion (str (params :accion) " " (params :valor))]
                        (.sendText @ws_atom accion true)
                        {:status 200 :body "Accion enviada"})
      (serve-static uri))))

(defn -main [& args]

  (let [port (if (seq args)
               (Integer/parseInt (first args))
               3000)]


    (hk-server/run-server web_handler {:port port})
    (println "View server running on port: " port)


    (let [client (HttpClient/newHttpClient)
          ws (-> client
                 (.newWebSocketBuilder)
                 (.buildAsync (URI/create "ws://localhost:8080/ws") listener)
                 (.join))]
         (reset! ws_atom ws)
      

      (loop []
        (println "Enter msg, exit to close:")
        (let [msg (read-line)]
          (when (not= msg "exit")
            (.sendText ws msg true)
            (recur))))

      (.sendClose ws WebSocket/NORMAL_CLOSURE "bye"))))