# Uma_Musume - Texas Hold'em Poker - Distributed Multiplayer Game

**Integrantes:**
- Alvarez Puma, Alan Patrizio
- Hidalgo Machaca, Diego Alejandro
- Huicho Perez, Anthony
- Valenzuela Calderón, Luigi Yamil

El programa es una implementación robusta y distribuida del clásico juego de cartas **Texas Hold'em** para 4 jugadores, desarrollada íntegramente en **Clojure** utilizando una arquitectura distribuida Cliente-Servidor y comunicación bidireccional en tiempo real.

## Características Técnicas

* **Arquitectura Desacoplada:** El sistema divide la lógica de negocio central en un **Servidor Central** único y **Clientes Locales** independientes para cada terminal de jugador.
* **Concurrencia Segura:** Control de estados reactivos y asíncronos mediante el uso nativo de **Agentes de Clojure** (`mesa-ag`, `ws-buffer`), garantizando mutaciones libres de colisiones de hilos (*thread-safe*).
* **Interoperabilidad Frontend/Backend:** Los clientes levantan un microservicio web local basado en la **especificación Ring** para inyectar la interfaz gráfica (HTML5/CSS3/JS) y exponen una API REST interna para comunicar el navegador con el socket de red de forma transparente.

## Tecnologías Utilizadas

* **Lenguaje:** Clojure 1.11+ (Programación Funcional)
* **Servidor HTTP & WebSockets:** Http-Kit
* **Serialización:** `clojure.data.json`
* **Interoperabilidad:** Java Native Net HTTP (para el WebSocket Listener asíncrono)
* **Frontend:** HTML5, CSS3 clásico y Vanilla JavaScript (peticiones asíncronas `fetch`)

---

## Instrucciones de Ejecución

Este proyecto está diseñado para ejecutarse tanto en una sola máquina (abriendo múltiples consolas) como en **computadoras distintas conectadas a la misma red local**.

### Prerrequisitos
Tener instalado el entorno de ejecución de Clojure ([Clojure CLI](https://clojure.org/guides/install_clojure)).

### Ejecución
Para correr el servidor se usa el siguiente comando:
- clj -M -m poker.server PUERTO

Para los clientes locales o remotos usar:
- clj -M -m poker.client PUERTO_WEB IP_SERVIDOR PUERTO_SERVIDOR

Ejemplos:
- clj -M -m poker.server 8080 (Lanza el nodo central en la PC principal)
- clj -M -m poker.client 3001 localhost 8080 (Jugador 1 en la misma máquina del servidor)
- clj -M -m poker.client 3002 10.0.0.1 8080 (Jugador 2 desde otra PC conectada a la red local IP 10.0.0.1)
