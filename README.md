# Distributed Tic Tac Toe — Microservices

A Tic Tac Toe game played **automatically by microservices**. The Game Session Service
generates moves for both players and forwards them to the Game Engine Service, which owns
the rules and the board. A browser UI watches the battle live over Server-Sent Events.

```
┌──────────────┐   SSE / REST   ┌──────────────────┐     REST      ┌──────────────────────┐
│    web-ui    │ ─────────────► │   game-session   │ ────────────► │     game-engine      │
│    :8080     │                │      :8082       │               │        :8081         │
│  static SPA  │                │  sessions, move  │               │  board, validation,  │
└──────────────┘                │  automation,     │               │  win/draw detection  │
                                │  move history    │               └──────────────────────┘
                                └──────────────────┘
```

| Module              | Purpose                                                                                  |
|---------------------|------------------------------------------------------------------------------------------|
| `common`            | Shared API contract (`GameState`, `MoveRequest`, ...), game rules and id validation.     |
| `game-engine`       | Core game logic: board state, move validation, win/draw detection. In-memory store.     |
| `game-session`      | Session management, automated move generation (rule-based strategy), move history, SSE. |
| `web-ui`            | Browser UI: renders the board live as the services play (SSE with polling fallback).    |
| `api-gateway`       | Spring Cloud Gateway (`:8090`): single entry point routing to all services.             |
| `discovery-server`  | Eureka registry (`:8761`): services register and discover each other dynamically.       |
| `integration-tests` | Boots **the real services** and verifies the full automated flow over HTTP.             |

## Requirements

- Java 25+ — the Gradle wrapper is included, no Gradle installation needed
- Stack: Spring Boot 4.1, Spring Cloud 2025.1 (Oakwood)

## Build & test everything

```bash
./gradlew build
```

This compiles all modules and runs unit tests, per-service integration tests, and the
cross-service end-to-end tests.

## Run

Start each service in its own terminal — the discovery server first, since everything
registers with it and resolves addresses through it:

```bash
# Terminal 1 — Discovery Server / Eureka (port 8761)
./gradlew :discovery-server:bootRun

# Terminal 2 — Game Engine (port 8081)
./gradlew :game-engine:bootRun

# Terminal 3 — Game Session (port 8082)
./gradlew :game-session:bootRun

# Terminal 4 — Web UI (port 8080)
./gradlew :web-ui:bootRun

# Terminal 5 — API Gateway (port 8090)
./gradlew :api-gateway:bootRun
```

Then open **http://localhost:8090** (the gateway — single entry point) and click
**Start Simulation**. The board updates in real time as the services play; the move
history and final result appear alongside. The Eureka dashboard at
**http://localhost:8761** shows every registered instance.

Alternatively, after `./gradlew bootJar`, run the same five with
`java -jar <module>/build/libs/<module>-1.0.0.jar`.

## Run with Docker

With Docker installed (no local JDK needed — the images build the services themselves):

```bash
docker compose up --build
```

Then open **http://localhost:8090** — the API gateway, the single entry point: it serves
the UI and routes `/games/**` to the engine and `/sessions/**` to the session service
(browser, API and SSE all through one origin). The compose file:

- builds each service from the shared multi-stage [Dockerfile](Dockerfile) (`MODULE` build arg),
- starts them in dependency order using **healthchecks** (Spring Boot Actuator's
  `/actuator/health`): everything waits for a healthy discovery server, the session
  service waits for a healthy engine, the UI for the session service, the gateway for
  the UI,
- lets services find each other through **Eureka** (`EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`
  points at the discovery-server container); only the UI's `SESSION_BASE_URL` is a fixed
  host address (the gateway's published port), because it is the *browser* on the host
  that makes those calls.

Stop everything with `docker compose down`.

## API

### Game Engine Service (`:8081`)

| Method | Path                   | Description                                       | Errors                                                                           |
|--------|------------------------|---------------------------------------------------|----------------------------------------------------------------------------------|
| POST   | `/games`               | Create a game. Body `{"gameId": "..."}` optional. | `400` invalid gameId, `409` duplicate id                                         |
| GET    | `/games/{gameId}`      | Current board and status.                         | `400` invalid gameId, `404` unknown game                                         |
| POST   | `/games/{gameId}/move` | Body `{"player": "X", "position": 0-8}`.          | `400` invalid input, `404` unknown, `409` occupied cell / wrong turn / game over |

Ids are validated everywhere they appear (body and path): 1–64 characters of letters,
digits, `-` and `_` — anything else is rejected with `400` before it reaches the domain.

Game state response:

```json
{
  "gameId": "…",
  "board": [
    "X",
    null,
    "O",
    null,
    "X",
    null,
    null,
    null,
    null
  ],
  "status": "IN_PROGRESS | WON | DRAW",
  "winner": "X | O | null",
  "nextPlayer": "X | O | null"
}
```

Board positions are indexed 0–8, left-to-right, top-to-bottom. Errors follow RFC 7807
(`application/problem+json`).

### Game Session Service (`:8082`)

| Method | Path                             | Description                                                                           |
|--------|----------------------------------|---------------------------------------------------------------------------------------|
| POST   | `/sessions`                      | Create a session; initializes the game in the engine (sessionId = gameId).            |
| POST   | `/sessions/{sessionId}/simulate` | Start automated simulation (async, returns `202`). `409` if already running/finished. |
| GET    | `/sessions/{sessionId}`          | Session details: status, board, winner, full move history.                            |
| GET    | `/sessions/{sessionId}/events`   | **SSE stream** of live session updates while the game is played.                      |

If the engine is unreachable, session endpoints answer `502` and a running simulation is
marked `FAILED` with the error preserved on the session. Engine calls are bounded by
configurable timeouts (`engine.connect-timeout`, default `2s`; `engine.read-timeout`,
default `5s`), so a hanging engine cannot stall sessions indefinitely.

### Web UI (`:8080`)

Serves the single-page app and `GET /config`, which tells the browser where the session
service lives (`session.base-url`, default `http://localhost:8082`). If `/config` is
unavailable (e.g. the page is opened straight from disk), the UI falls back to that same
default. The session service allows cross-origin calls via `cors.allowed-origins`
(default `*` to keep the demo easy to run; restrict it in a real deployment).

## How the simulation works

1. `POST /sessions` generates a UUID and creates the matching game in the engine.
2. `POST /sessions/{id}/simulate` flips the session to `RUNNING` (guarded against double
   starts) and launches the game loop on a virtual thread.
3. Each turn, the session service asks its `MoveStrategy` for a move and forwards it to
   the engine. The strategy is rule-based: win if possible → block the opponent → take
   the center → take a random corner → random cell. Random tie-breaking keeps games varied
   (wins **and** draws occur).
4. The engine validates each move, updates the board and reports the status. The engine is
   the single source of truth — the session service never decides outcomes itself, it only
   mirrors the engine's responses into the session's move history.
5. After every move the session snapshot is pushed to subscribed UI clients over SSE
   (the UI falls back to polling if the stream drops). A configurable delay
   (`simulation.move-delay-ms`, default 400) makes the game watchable.

## Logging

All services log through **Logback** (Spring Boot's default), configured in each service's
`logback-spring.xml`:

- **Console** — Spring Boot's standard colored pattern.
- **Rolling file** — `logs/<service-name>.log` relative to the working directory
  (override the directory with the `LOG_PATH` environment variable). Rotation uses
  Spring Boot's defaults (10 MB per file, 7 days of history).

`./gradlew clean` also deletes the `logs/` directories, so it resets the checkout
completely — build outputs and runtime logs alike.

### Correlation

Every log line uses a compact pattern —
`time level thread [cid,gameId] logger:line - message` — with the correlation values
taken from the MDC:

- **cid** — a correlation id per HTTP request, taken from the `X-Correlation-Id` header
  when the caller provides a well-formed one, generated otherwise, and echoed in the
  response. The simulation loop is not a request, so each run gets its own cid, and the
  engine client propagates it on every outgoing call — so one game's entire move sequence
  can be joined **across both services** by a single cid.
- **gameId** — the game/session id, set wherever it is known, joining all activity for
  one game regardless of which request produced it.

`grep <cid> logs/*.log` reconstructs a full distributed flow. In a larger system this
would be Micrometer Tracing/OpenTelemetry with trace/span ids; the mechanism (MDC +
header propagation) is the same.

Application packages log at `DEBUG` (set in `logback-spring.xml`) so every automated move
is traceable end-to-end: the session service logs each move it sends, the engine logs each
move it applies, and rejected requests are logged at `WARN` with the reason. Levels can
still be overridden at runtime the Spring Boot way, e.g. `--logging.level.com.tictactoe=INFO`.

## Testing

| Layer               | Where                                          | What it covers                                                                                                                           |
|---------------------|------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| Unit                | `GameServiceTest`, `RuleBasedMoveStrategyTest` | Rules: wins in every direction, draws, illegal moves, turn order; strategy heuristics.                                                   |
| Service integration | `GameApiIntegrationTest`, `SessionApiTest`     | Full REST behavior of each service, error contracts (400/404/409), failure handling when the engine is down.                             |
| End-to-end          | `integration-tests` → `FullGameFlowTest`       | Both real services over HTTP: create → simulate → finished; engine and session views agree; board equals the replay of the move history. |

Run just the end-to-end suite with:

```bash
./gradlew :integration-tests:test
```

## Design notes & possible improvements

- **Concurrency** — the engine serializes moves per game (`synchronized` on the game
  aggregate, so independent games still run in parallel); concurrent or duplicate move
  requests can never corrupt a board and losers of the race get a `409`. Session state
  transitions are guarded the same way, so `simulate` cannot be started twice.
  `GameConcurrencyTest` proves it by racing 16 threads on the same cell (exactly one
  wins) and storming a game from 8 threads (turn alternation and the win line stay
  consistent).
- **Persistence** — in-memory by design, but isolated behind repository interfaces.
  Finished games/sessions are evicted after a retention period (`cleanup.retention`,
  default 1h; checked every `cleanup.interval`, default 5m), so the stores stay bounded.
- **Resilience** — engine errors are translated into typed exceptions and surfaced as
  RFC 7807 responses (`502` at the session boundary). Retries with backoff (Spring Retry
  or Resilience4j circuit breaker) would be the next step for transient network failures;
  moves are naturally idempotent-checkable because an occupied cell returns `409`.
- **Shared contract** — the `common` module holds the wire contract between the services
  (DTOs, enums, id validation) plus the game rules, so the engine and the session service
  can't drift apart silently. The trade-off is coupling: in a setup where services are
  deployed and versioned independently, this would become a versioned artifact (or an
  OpenAPI spec with generated clients) rather than a source-level module.
- **API gateway** — `api-gateway` (Spring Cloud Gateway, port `8090`) is the single entry
  point: `/games/**` → engine, `/sessions/**` → session service, everything else → web-ui.
  Headers — including `X-Correlation-Id` — pass through untouched, so correlation spans
  the gateway too. Note that the gateway handles **edge (north–south) traffic only**:
  internal east–west traffic (session → engine) does not hairpin through it — that would
  add a hop and a single point of failure to the hottest path for no benefit. The gateway
  changes the system's shape: one origin for the browser (no CORS needed on that path),
  one place for cross-cutting concerns (auth, rate limiting, request logging) later.
- **Service discovery (Eureka)** — all services register with the `discovery-server`
  (dashboard at `http://localhost:8761`) and addresses are resolved dynamically: the
  gateway's routes are `lb://game-engine`-style service ids, and the session service
  discovers the engine the same way (`engine.base-url: http://game-engine` resolved by a
  `@LoadBalanced` client). Instances can move, restart at new addresses, or scale out
  without any config change. Lease/refresh intervals are tuned down to ~5s for the demo;
  discovery is **eventually consistent** — a just-(re)started instance takes a few
  seconds to become routable again. Tests bypass discovery (`eureka.client.enabled=false`,
  `engine.load-balanced=false`) to stay hermetic; the same switches are the documented
  operational fallback to plain static addressing.
- **Smarter players** — the `MoveStrategy` interface allows a minimax (unbeatable)
  implementation or per-player strategies without touching the simulation loop.
- **Real-time** — SSE was chosen over WebSockets because updates flow strictly
  server→client; the UI degrades gracefully to polling.
