(ns poker.utils
  (:require [org.httpkit.server :as hk-server]
            [clojure.data.json :as json]))

(defn parse_query_string [qs]
  (if (nil? qs) {}
      (->> (clojure.string/split qs #"&")
           (map #(clojure.string/split % #"="))
           (map (fn [[k v]] [(keyword k) v]))
           (into {}))))