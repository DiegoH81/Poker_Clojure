(ns logicaApuestas)

(defn crear-jugador [nombre] 
  (agent {:name nombre
          :hand []
          :money 1000
          :bet 0
          :action ""
          :fold false}))


(def mesa (agent {:players [(crear-jugador "Anthony")
                            (crear-jugador "Alan")
                            (crear-jugador "Luigi")
                            (crear-jugador "Diego")]
                  :pot 0
                  :turn 0
                  :current-bet 0
                  :ronda 0}))