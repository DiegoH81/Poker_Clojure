(ns pokerPrincipal
  (:require [cards :as cartas]
            [evaluator :as eval]
            [logicaApuestas :as apuestas]))



(defn avanzar-turno [m]
  (assoc m :turn (mod (inc (:turn m)) 4)))

(defn aplicar-big-blind-mesa [m]
  (assoc m :pot (+ (:pot m) 50) 
           :current-bet 50 
           :turn (mod (inc (:turn m)) 4)))

(defn sumar-al-pozo-y-avanzar [m diferencia]
  (assoc m :pot (+ (:pot m) diferencia) 
           :turn (mod (inc (:turn m)) 4)))

(defn aplicar-subir-mesa [m diferencia cantidad]
  (assoc m :pot (+ (:pot m) diferencia) 
           :current-bet cantidad 
           :turn (mod (inc (:turn m)) 4)))

(defn aplicar-big-blind-jugador [j]
  (assoc j :money (- (:money j) 50) 
           :bet 50 
           :action "BigBlind"))

(defn aplicar-igualar-jugador [j diferencia apuesta-actual]
  (assoc j :money (- (:money j) diferencia) 
           :bet apuesta-actual 
           :action "Igualar"))

(defn aplicar-subir-jugador [j diferencia cantidad]
  (assoc j :money (- (:money j) diferencia) 
           :bet cantidad 
           :action "Subir"))

(defn agregar-comunitarias-mesa [m cartas]
  (update m :community-cards into cartas))

(defn agregar-cartas-jugador [estado cartas]
  (update estado :hand into cartas))

(defn asignar-mano-inicial [estado cartas]
  (assoc estado :hand (vec cartas)))



(defn jugador-activo? [j]
  (not (:fold @j)))

(defn jugador-nivelado? [apuesta-maxima j]
  (or (:fold @j) (= (:bet @j) apuesta-maxima)))

(defn tiene-jugada-pendiente? [j]
  (= (:action @j) ""))



(defn analizar-combo [nombre-jugador combo]
  (let [frec (frequencies (map :rank combo))
        hist (sort > (vals frec))
        check-draws (map key (sort-by val > frec))]
        {:jugador nombre-jugador
        :rank-type (eval/histogram-cases hist check-draws combo)
        :draws (vec (sort > (map :rank combo)))}))

(defn criterio-poker [mano]
  [[(:rank-eval (:rank-type mano)) (:draws mano)]])

(defn evaluar-mejor-mano [j]
  (let [nombre (:name @j)
        mano-7 (:hand @j)
        todos-combos (clojure.math.combinatorics/combinations mano-7 5)
        manos-formateadas (for [c todos-combos] (analizar-combo nombre c))]
    (last (sort-by criterio-poker manos-formateadas))))



(defn ejecutar-big-blind [jugador mesa-ag]
  (send jugador aplicar-big-blind-jugador)
  (send mesa-ag aplicar-big-blind-mesa))

(defn ejecutar-igualar [jugador mesa-ag]
  (let [apuesta-actual (:current-bet @mesa-ag)
        p-bet (:bet @jugador)
        diferencia (- apuesta-actual p-bet)]
    (send jugador aplicar-igualar-jugador diferencia apuesta-actual)
    (send mesa-ag sumar-al-pozo-y-avanzar diferencia)))

(defn ejecutar-pasar [jugador mesa-ag]
  (send jugador assoc :action "Pasar")
  (send mesa-ag avanzar-turno))

(defn ejecutar-subir [jugador mesa-ag cantidad]
  (let [p-bet (:bet @jugador)
        diferencia (- cantidad p-bet)]
    (send jugador aplicar-subir-jugador diferencia cantidad)
    (send mesa-ag aplicar-subir-mesa diferencia cantidad)))

(defn ejecutar-retirarse [jugador mesa-ag]
  (send jugador assoc :action "Retirarse" :fold true)
  (send mesa-ag avanzar-turno))

(defn obtener-opciones [jugador mesa-ag]
  (if (:fold @jugador)
    []
    (if (< (:bet @jugador) (:current-bet @mesa-ag))
      ["Igualar" "Subir" "Retirarse"]
      ["Pasar" "Subir" "Retirarse"])))

(defn limpiar-apuestas-ronda [players mesa-ag]
  (loop [lista-jugadores players]
    (when (seq lista-jugadores)
      (let [j (first lista-jugadores)]
        (send j assoc :bet 0 :action (if (:fold @j) "Retirarse" ""))
        (recur (rest lista-jugadores)))))
  
  (send mesa-ag assoc :current-bet 0)
  (await-for 1000 mesa-ag))

(defn procesar-voto [accion jugador mesa-ag]
  (cond
    (= accion "Igualar")   (ejecutar-igualar jugador mesa-ag)
    (= accion "Pasar")     (ejecutar-pasar jugador mesa-ag)
    (= accion "Subir")     (do (println "Ingresa el monto total al que deseas subir la apuesta:")
                               (let [cant (Integer/parseInt (read-line))]
                                 (ejecutar-subir jugador mesa-ag cant)))
    (= accion "Retirarse") (ejecutar-retirarse jugador mesa-ag))
  (await jugador mesa-ag))



(defn gestionar-ronda-apuestas [mesa-ag nombre-ronda]
  (println "\n=======================================================")
  (println " INICIANDO RONDA DE APUESTAS:" nombre-ronda)
  (println "=======================================================")
  (let [players (:players @mesa-ag)]
    
    (when (= nombre-ronda "Pre-Flop")
      (let [primer-jugador (nth players (:turn @mesa-ag))]
        (ejecutar-big-blind primer-jugador mesa-ag)
        (await primer-jugador mesa-ag)
        (println "-> El jugador" (:name @primer-jugador) "ha pagado la Big Blind obligatoria (50).")))

    (loop []
      (let [activos (filter jugador-activo? players)
            apuesta-maxima (:current-bet @mesa-ag)
            todos-nivelados (every? (partial jugador-nivelado? apuesta-maxima) players)
            jugadas-pendientes (some tiene-jugada-pendiente? activos)]
        
        (cond
          (= (count activos) 1)
          (do (println "\n¡¡PARTIDA FINALIZADA POR ABANDONO!!")
              :ganador-por-abandono)

          (and todos-nivelados (not jugadas-pendientes))
          (do (println "\n--- Apuestas concluidas para" nombre-ronda "---")
              (limpiar-apuestas-ronda players mesa-ag)
              :continuar)

          :else
          (let [idx-turno (:turn @mesa-ag)
                jugador-actual (nth players idx-turno)]
            (if (:fold @jugador-actual)
              (do
                (send mesa-ag avanzar-turno)
                (await mesa-ag)
                (recur))
              (do
                (println "\n- TURNO DE:" (:name @jugador-actual) "-")
                (println "Mesa    Pozo:" (:pot @mesa-ag) " | Apuesta del nivel:" (:current-bet @mesa-ag))
                (println "Estado -> Dinero:" (:money @jugador-actual) " | Apostado en esta ronda:" (:bet @jugador-actual))
                
               
                (println "Tus Cartas:" (:hand @jugador-actual)) 
                
                (let [opciones (obtener-opciones jugador-actual mesa-ag)]
                  (println "Opciones válidas:" opciones)
                  (print "Pon tu acción: ")
                  (flush)
                  (let [entrada (read-line)]
                    (if (some (partial = entrada) opciones)
                      (procesar-voto entrada jugador-actual mesa-ag)
                      (println "Acción inválida. Vuelve a escribir."))))
                (recur)))))))))



(defn repartir-comunitarias-a-todos [players mesa-ag cartas]
  (send mesa-ag agregar-comunitarias-mesa cartas)
  
  (loop [lista-jugadores players]
    (when (seq lista-jugadores)          
      (let [j (first lista-jugadores)]     
        (send j agregar-cartas-jugador cartas)
        (await j)                                         
        (recur (rest lista-jugadores)))))  
  
  (await mesa-ag))



(defn iniciar-juego []
  (let [mesa-ag apuestas/mesa
        players (:players @mesa-ag)
        mazo-barajado (shuffle cartas/game-cards)
        
        cartas-p1 (take 2 mazo-barajado)
        temp-mazo (drop 2 mazo-barajado)
        cartas-p2 (take 2 temp-mazo)
        temp-mazo (drop 2 temp-mazo)
        cartas-p3 (take 2 temp-mazo)
        temp-mazo (drop 2 temp-mazo)
        cartas-p4 (take 2 temp-mazo)
        mazo-restante (drop 2 temp-mazo)] 

    (println "=== BIENVENIDO AL JUEGO DE POKER ===")

    (send (nth players 0) asignar-mano-inicial cartas-p1)
    (send (nth players 1) asignar-mano-inicial cartas-p2)
    (send (nth players 2) asignar-mano-inicial cartas-p3)
    (send (nth players 3) asignar-mano-inicial cartas-p4)
    
    (loop [lista-p players]
      (when (seq lista-p)
        (await (first lista-p))
        (recur (rest lista-p))))
        
    (println "-> 2 Cartas privadas repartidas a cada jugador.")

    ;; 1ra Ronda de apuestas: Pre-Flop
    (if (= (gestionar-ronda-apuestas mesa-ag "Pre-Flop") :ganador-por-abandono)
      (let [ganador (first (filter jugador-activo? players))]
        (println "¡El jugador" (:name @ganador) "gana el pozo de" (:pot @mesa-ag) "porque todos se retiraron!"))
      
      ;; 2da Ronda: Revelando el Flop (3 cartas comunitarias)
      (let [flop-cards (take 3 mazo-restante)
            mazo-2 (drop 3 mazo-restante)]
        (println "\n*** REVELANDO EL FLOP (3 Cartas Comunitarias): ***" flop-cards)
        (repartir-comunitarias-a-todos players mesa-ag flop-cards)
        
        (if (= (gestionar-ronda-apuestas mesa-ag "Flop") :ganador-por-abandono)
          (let [ganador (first (filter jugador-activo? players))]
            (println "¡El jugador" (:name @ganador) "gana el pozo de" (:pot @mesa-ag) "porque todos se retiraron!"))
          
          ;; 3ra Ronda: Revelando el Turn (1 carta comunitaria)
          (let [turn-card (take 1 mazo-2)
                mazo-3 (drop 1 mazo-2)]
            (println "\n*** REVELANDO EL TURN (4ta Carta Comunitaria): ***" turn-card)
            (repartir-comunitarias-a-todos players mesa-ag turn-card)
            
            (if (= (gestionar-ronda-apuestas mesa-ag "Turn") :ganador-por-abandono)
              (let [ganador (first (filter jugador-activo? players))]
                (println "¡El jugador" (:name @ganador) "gana el pozo de" (:pot @mesa-ag) "porque todos se retiraron!"))
              
              ;; 4ta Ronda: Revelando el River (Última carta comunitaria)
              (let [river-card (take 1 mazo-3)]
                (println "\n*** REVELANDO EL RIVER (Última Carta Comunitaria): ***" river-card)
                (repartir-comunitarias-a-todos players mesa-ag river-card)
                
                (if (= (gestionar-ronda-apuestas mesa-ag "River") :ganador-por-abandono)
                  (let [ganador (first (filter jugador-activo? players))]
                    (println "¡El jugador" (:name @ganador) "gana el pozo de" (:pot @mesa-ag) "porque todos se retiraron!"))
                  
                  ;; SHOWDOWN: Evaluación final si llegaron al final con el mazo
                  (let [finalistas (filter jugador-activo? players)]
                    (println "\n=============================================")
                    (println "         SHOWDOWN - EVALUACIÓN FINAL         ")
                    (println "=============================================")
                    (let [resultados (map evaluar-mejor-mano finalistas)
                          ganador (last (sort-by criterio-poker resultados))]
                      
                      (loop [lista-r resultados]
                        (when (seq lista-r)
                          (let [r (first lista-r)]
                            (println "Jugador:" (:jugador r) "tiene la jugada:" (:type (:rank-type r))))
                          (recur (rest lista-r))))
                      
                      (println "\n¡¡EL GANADOR ES:" (:jugador ganador) "con un" (:type (:rank-type ganador)) "!!")
                      (println "Se lleva la cantidad total acumulada de:" (:pot @mesa-ag) "fichas."))))))))))))

(defn -main [& args]
  (iniciar-juego)
  (shutdown-agents))