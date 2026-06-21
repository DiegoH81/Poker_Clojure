(ns poker.logicaApuestas)

(defn crear-jugador [nombre in_id] 
  (agent {:name nombre
          :id in_id
          :hand []
          :money 1000
          :bet 0
          :action ""
          :fold false}))