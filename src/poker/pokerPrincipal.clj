(ns poker.pokerPrincipal
  (:require [poker.cards :as cartas]
            [poker.evaluator :as eval]
            [poker.logicaApuestas :as apuestas]))

(def mazo-restante (agent []))
(def fase-actual (agent :pre-flop))

(defn imprimir-jugador [j]
  (println (format "  %-8s | id: %-5d | dinero: %-5d | apuesta: %-4d | fold: %-5s | accion: %-10s | mano: %s"
                   (:name @j)
                   (:id @j)
                   (:money @j)
                   (:bet @j)
                   (:fold @j)
                   (:action @j)
                   (:hand @j))))

(defn imprimir-estado [mesa-ag]
  (println "\n========================= ESTADO DE LA MESA =========================")
  (println "Pozo:" (:pot @mesa-ag)
           "| Apuesta actual:" (:current-bet @mesa-ag)
           "| Turno (idx):" (:turn @mesa-ag)
           "| Ronda:" (:ronda @mesa-ag))
  (println "Cartas comunitarias:" (:community-cards @mesa-ag))
  (println "-----------------------------------------------------------------------")
  (doseq [j (:players @mesa-ag)]
    (imprimir-jugador j))
  (println "=========================================================================\n"))

(defn buscar-siguiente-turno [turno-actual players]
  (loop [i 1]
    (if (>= i (count players))
      turno-actual
      (let [idx (mod (+ turno-actual i) (count players))
            j (nth players idx)]
        (if (:fold @j)
          (recur (inc i)) ;; fold - check next
          idx)))))        ;; active

(defn avanzar-turno [m]
  (assoc m :turn (buscar-siguiente-turno (:turn m) (:players m))))

(defn aplicar-big-blind-mesa [m cantidad-real]
  (assoc m :pot (+ (:pot m) cantidad-real) 
           :current-bet cantidad-real 
           ;; go to next turn
           :turn (buscar-siguiente-turno (:turn m) (:players m))))

(defn sumar-al-pozo-y-avanzar [m diferencia]
  (assoc m :pot (+ (:pot m) diferencia) 
           :turn (buscar-siguiente-turno (:turn m) (:players m))))

(defn aplicar-subir-mesa [m diferencia cantidad]
  (assoc m :pot (+ (:pot m) diferencia) 
           :current-bet cantidad 
           :turn (buscar-siguiente-turno (:turn m) (:players m))))

(defn aplicar-big-blind-jugador [j cantidad-real]
  (assoc j :money (- (:money j) cantidad-real) 
           :bet cantidad-real 
           :action "Big Blind"))

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
  (or (:fold @j) 
      (<= (:money @j) 0) ;; if $ = 0 - is leveled
      (>= (:bet @j) apuesta-maxima)))

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
        diferencia (- apuesta-actual p-bet)
        ;; avoid put non existent money
        diferencia-real (min diferencia (:money @jugador))]
    
    (send jugador aplicar-igualar-jugador diferencia-real apuesta-actual)
    (send mesa-ag sumar-al-pozo-y-avanzar diferencia-real)))

(defn ejecutar-pasar [jugador mesa-ag]
  (send jugador assoc :action "Pasar")
  (send mesa-ag avanzar-turno))

(defn ejecutar-subir [jugador mesa-ag cantidad-extra]
  (let [apuesta-actual (:current-bet @mesa-ag)
        p-bet (:bet @jugador)
        ;; biggest bet
        nueva-apuesta (+ apuesta-actual cantidad-extra)
        ;; to pay is = new bet - old bet
        diferencia (- nueva-apuesta p-bet)
        ;; not exceds its money
        diferencia-real (min diferencia (:money @jugador))]
    
    (send jugador aplicar-subir-jugador diferencia-real nueva-apuesta)
    (send mesa-ag aplicar-subir-mesa diferencia-real nueva-apuesta)))

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

(defn procesar-voto [accion jugador mesa-ag in-cantidad]
  (cond
    (= accion "Igualar") (ejecutar-igualar jugador mesa-ag)
    (= accion "Pasar") (ejecutar-pasar jugador mesa-ag)
    (= accion "Subir") (ejecutar-subir jugador mesa-ag in-cantidad)
    (= accion "Retirarse") (ejecutar-retirarse jugador mesa-ag))
  (await jugador mesa-ag))

(defn repartir-comunitarias-a-todos [players mesa-ag cartas]
  (send mesa-ag agregar-comunitarias-mesa cartas)
  
  (loop [lista-jugadores players]
    (when (seq lista-jugadores)          
      (let [j (first lista-jugadores)]     
        (send j agregar-cartas-jugador cartas)
        (await j)                                         
        (recur (rest lista-jugadores)))))  
  
  (await mesa-ag))

(defn barajar-cartas-iniciales [mesa-ag]
  (let [players (:players @mesa-ag)
        mazo-barajado (shuffle cartas/game-cards)

        cartas-p1 (take 2 mazo-barajado)
        temp-mazo (drop 2 mazo-barajado)
        cartas-p2 (take 2 temp-mazo)
        temp-mazo (drop 2 temp-mazo)
        cartas-p3 (take 2 temp-mazo)
        temp-mazo (drop 2 temp-mazo)
        cartas-p4 (take 2 temp-mazo)]

    (println "=== BIENVENIDO AL JUEGO DE POKER ===")

    (send (nth players 0) asignar-mano-inicial cartas-p1)
    (send (nth players 1) asignar-mano-inicial cartas-p2)
    (send (nth players 2) asignar-mano-inicial cartas-p3)
    (send (nth players 3) asignar-mano-inicial cartas-p4)
    (loop [lista-p players]
      (when (seq lista-p)
        (await (first lista-p))
        (recur (rest lista-p))))
    
    (send mazo-restante (fn [_] (drop 8 mazo-barajado)))
    (send fase-actual (fn [_] :pre-flop))
    (await mazo-restante fase-actual)

    (send mesa-ag assoc :turn (buscar-siguiente-turno -1 (:players @mesa-ag)))
    (await mesa-ag)

    ;; get player turn
    (let [idx-turno (:turn @mesa-ag)
          jugador-bb (nth (:players @mesa-ag) idx-turno)
          dinero-disponible (:money @jugador-bb)
          
          ;; calculate how much to charge
          bb-real (min 50 dinero-disponible)]
      
      ;; charge player
      (send jugador-bb aplicar-big-blind-jugador bb-real)
      (send mesa-ag aplicar-big-blind-mesa bb-real)
      ;; wait agent update
      (await jugador-bb mesa-ag))))

(defn showdown [mesa-ag]
  (let [players (:players @mesa-ag)
        finalistas (filter jugador-activo? players)
        resultados (map evaluar-mejor-mano finalistas)
        ganador (last (sort-by criterio-poker resultados))
        
        ;; search the winner
        ganador-ag (first (filter #(= (:name @%) (:jugador ganador)) finalistas))
        
        pozo-final (:pot @mesa-ag)]
    (println "         SHOWDOWN - EVALUACIÓN FINAL         ")

    ;; send money to winner
    (send ganador-ag update :money + pozo-final)
    (await ganador-ag)

    (println "\n¡¡EL GANADOR ES:" (:jugador ganador) "con un" (:type (:rank-type ganador)) "!!")

    (println "Se lleva la cantidad total acumulada de:" (:pot @mesa-ag) "fichas.")
    (send mesa-ag assoc :ganador {:nombre (:jugador ganador)
                                  :jugada (:type (:rank-type ganador))
                                  :pozo pozo-final
                                  :tipo "showdown"})
    (await mesa-ag)))

(defn avanzar-fase [mesa-ag players]
  (case @fase-actual
    :pre-flop (let [flop (take 3 @mazo-restante)]
                (send mazo-restante (fn [m] (drop 3 m)))
                (await mazo-restante)
                (repartir-comunitarias-a-todos players mesa-ag flop)
                (send fase-actual (fn [_] :flop))
                (await fase-actual))
    :flop (let [c (take 1 @mazo-restante)]
            (send mazo-restante (fn [m] (drop 1 m)))
            (await mazo-restante)
            (repartir-comunitarias-a-todos players mesa-ag c)
            (send fase-actual (fn [_] :turn))
            (await fase-actual))
    :turn (let [c (take 1 @mazo-restante)]
            (send mazo-restante (fn [m] (drop 1 m)))
            (await mazo-restante)
            (repartir-comunitarias-a-todos players mesa-ag c)
            (send fase-actual (fn [_] :river))
            (await fase-actual))
    :river (do (showdown mesa-ag)
               (send fase-actual (fn [_] :pre-flop))
               (await fase-actual))))

(defn procesar-accion-jugador [mesa-ag in-tipo-accion in-cantidad]
  (let [players (:players @mesa-ag)
        idx-turno (:turn @mesa-ag)
        jugador-actual (nth players idx-turno)]

    (procesar-voto in-tipo-accion jugador-actual mesa-ag in-cantidad)

    (let [activos (filter jugador-activo? players)
          apuesta-maxima (:current-bet @mesa-ag)
          todos-nivelados (every? (partial jugador-nivelado? apuesta-maxima) players)
          jugadas-pendientes (some tiene-jugada-pendiente? activos)]

      (cond
        (= (count activos) 1)
        (let [ganador (first activos)]

          (println "\n¡¡PARTIDA FINALIZADA POR ABANDONO!!")
          (send ganador update :money + (:pot @mesa-ag))
          (await ganador) ;; wait for winner update
          (send mesa-ag assoc :ganador {:nombre (:name @ganador)
                                        :jugada nil
                                        :pozo (:pot @mesa-ag)
                                        :tipo "abandono"})
          (await mesa-ag)
          :ganador-por-abandono)
        
        (and todos-nivelados (not jugadas-pendientes))
        (do (limpiar-apuestas-ronda players mesa-ag)
            (avanzar-fase mesa-ag players))))

    (let [opciones (obtener-opciones jugador-actual mesa-ag)]
      (println "Opciones válidas:" opciones))))

(defn procesar-reinicio [mesa-ag player-id]
  ;; add id player to set - avoid duplicades
  (send mesa-ag update :votos-reinicio (fnil conj #{}) player-id)
  (await mesa-ag)
  ;; check 4 players vote
  (when (= 4 (count (:votos-reinicio @mesa-ag)))
    ;; clean player states
    (doseq [j (:players @mesa-ag)]
      (let [sin-dinero? (<= (:money @j) 0)]
        (send j assoc 
              :hand [] 
              :bet 0 
              :action (if sin-dinero? "ELIMINADO" "") 
              :fold sin-dinero?))) ;; if $ = 0 - fold true

    ;; clean all
    (send mesa-ag assoc 
          :pot 0
          :community-cards []
          :current-bet 0
          :ganador nil
          :ronda 0
          :votos-reinicio #{})
    (await mesa-ag)
    ;; sort again
    (barajar-cartas-iniciales mesa-ag)))

(defn procesar-reinicio-total [mesa-ag player-id]
  (send mesa-ag update :votos-reinicio (fnil conj #{}) player-id)
  (await mesa-ag)
  
  (when (= 4 (count (:votos-reinicio @mesa-ag)))
    ;; restart all states
    (doseq [j (:players @mesa-ag)]
      (send j assoc :hand [] :bet 0 :action "" :fold false :money 1000))
    
    ;; clean table
    (send mesa-ag assoc 
          :pot 0
          :community-cards []
          :current-bet 0
          :ganador nil
          :ronda 0
          :votos-reinicio #{})
    (await mesa-ag)
    (barajar-cartas-iniciales mesa-ag)))