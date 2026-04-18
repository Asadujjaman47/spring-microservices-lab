# Run & Test Guide

End-to-end instructions for starting every service locally and exercising every feature shipped through Phase 5. Written for a fresh clone on Linux/macOS; Windows users should run the same commands in WSL2.

---

## 1. Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | **21 (Temurin 21.0.4)** | Project fails to build on JDK 22+ because Spotless/Google Java Format pin to 21. Install via SDKMAN: `sdk install java 21.0.4-tem`. |
| Docker | 24+ with Compose v2 | Required for Postgres/Redis/RabbitMQ/Zipkin/pgAdmin. |
| `curl` + `jq` | latest | Used in smoke tests below. |
| (optional) IntelliJ HTTP Client or VS Code REST Client | — | `docs/http/gateway.http` is pre-wired. |

Verify:

```bash
java -version   # expect 21.0.4
docker --version
docker compose version
```

If your shell default isn't 21, export it before every `./mvnw` call:

```bash
export JAVA_HOME=~/.sdkman/candidates/java/21.0.4-tem
export PATH=$JAVA_HOME/bin:$PATH
```

---

## 2. Clone, configure, build

```bash
git clone https://github.com/Asadujjaman47/spring-microservices-lab.git
cd spring-microservices-lab
cp .env.example .env
```

`.env` is **optional** for local dev — every variable has a default in `docker-compose.yml` and `config-repo/application.yml`, and the stack runs fine without it. Create it if you want to override the committed fallback `JWT_SECRET` with a real one:

```bash
# Generate one:
openssl rand -base64 48
# Paste the output into JWT_SECRET in .env
```

> **Important:** do **not** put `SPRING_PROFILES_ACTIVE` in `.env`. `spring-dotenv` loads the file *after* Spring Boot has already resolved active profiles, so profile activation from `.env` is silently ignored. Set the profile in the IntelliJ Run Configuration (§4.5) or as a shell env var when launching the JAR (§4.3).

Build everything (runs tests + Spotless):

```bash
./mvnw clean verify
```

First build downloads ~400MB of Maven dependencies; subsequent builds are fast. Every module must turn green before you proceed.

---

## 3. Start infrastructure

```bash
docker compose up -d
docker compose ps   # every container should be "healthy"
```

What comes up:

| Container | Port(s) | Purpose |
|---|---|---|
| `ms-postgres` | 5432 | Four per-service DBs auto-created on first boot (userdb, productdb, orderdb, notificationdb). |
| `ms-redis` | 6379 | Cache for product-service. |
| `ms-rabbitmq` | 5672 (AMQP), 15672 (UI) | Cloud Bus + domain events. |
| `ms-zipkin` | 9411 | Distributed tracing UI. |
| `ms-pgadmin` | 5050 | Web DB client. |

> Need the Spring services in containers too? Skip §4 entirely and jump to [§4.6 — full stack in containers](#46-phase-7--alternative-run-the-full-stack-in-containers), which adds the `apps` Compose profile on top of this.

Smoke-check the infra:

```bash
curl -s http://localhost:15672/api/overview -u guest:guest | jq '.product_name'   # "RabbitMQ"
docker exec ms-redis redis-cli ping                                                # PONG
docker exec ms-postgres psql -U postgres -c '\l' | grep -E 'userdb|productdb|orderdb|notificationdb'
```

---

## 4. Start the Java services

Services must start **in this order** — each pulls config from `:8888` and registers with Eureka on `:8761`. Open one terminal per service (or use `tmux`).

> **Every terminal** needs JDK 21 exported first:
> `export JAVA_HOME=~/.sdkman/candidates/java/21.0.4-tem PATH=$JAVA_HOME/bin:$PATH`

### 4.1 config-service (port 8888)

```bash
java -jar config-service/target/config-service-0.1.0-SNAPSHOT.jar
```

Sanity check:
```bash
curl -s http://localhost:8888/user-service/default | jq '.propertySources[0].name'
```

### 4.2 discovery-service (port 8761)

```bash
java -jar discovery-service/target/discovery-service-0.1.0-SNAPSHOT.jar
```

Dashboard: http://localhost:8761

### 4.3 Business services (any order after config + discovery are up)

Each business service uses `SPRING_PROFILES_ACTIVE=local` to apply seed data.

```bash
# Terminal A
SPRING_PROFILES_ACTIVE=local java -jar user-service/target/user-service-0.1.0-SNAPSHOT.jar         # :8081

# Terminal B
SPRING_PROFILES_ACTIVE=local java -jar product-service/target/product-service-0.1.0-SNAPSHOT.jar   # :8082

# Terminal C
SPRING_PROFILES_ACTIVE=local java -jar order-service/target/order-service-0.1.0-SNAPSHOT.jar       # :8083

# Terminal D
SPRING_PROFILES_ACTIVE=local java -jar notification-service/target/notification-service-0.1.0-SNAPSHOT.jar  # :8084
```

### 4.4 gateway (port 8080)

```bash
java -jar gateway/target/gateway-0.1.0-SNAPSHOT.jar
```

Wait ~10s, then confirm every service appears in Eureka:

```bash
curl -s http://localhost:8761/eureka/apps -H 'Accept: application/json' | jq '.applications.application[].name'
# Expect: USER-SERVICE, PRODUCT-SERVICE, ORDER-SERVICE, NOTIFICATION-SERVICE, GATEWAY
```

### 4.5 Running from IntelliJ (instead of JARs)

If you prefer clicking Run buttons over managing terminals, two things need to be set on **every** service's Run Configuration — otherwise `.env` is ignored and seed data never loads.

**Run → Edit Configurations… → (each service)**

1. **Working directory** → change from the default (`$MODULE_WORKING_DIR$`, which is the module folder) to **`$PROJECT_DIR$`** (the repo root).
   - *Why:* `spring-dotenv` reads `.env` from the JVM's working directory. With the default, it looks inside `user-service/` and silently finds nothing.

2. **Active profiles** → set to `local` for the four business services (user, product, order, notification). Leave empty for config-service, discovery-service, and gateway.
   - *Why:* The seed-data Flyway path (`classpath:db/seed`) is gated on `on-profile: local | docker`. Without the profile, Flyway applies migrations but skips seeds — you'll see empty `users` / `products` tables.
   - *Note:* this field **cannot** be replaced by `SPRING_PROFILES_ACTIVE` in `.env` (see §2).

If the "Active profiles" field isn't visible, click **Modify options** and enable it.

**Start order:** same as §4 — config-service → discovery-service → user / product / order / notification → gateway.

**Verify the profile took effect.** Each business service's startup log should show:

```
The following 1 profile is active: "local"
```

If it says `default`, the Run Configuration setting didn't apply — restart after re-saving the config.

### 4.6 Phase 7 — Alternative: run the full stack in containers

Instead of launching JARs or IntelliJ configs, build one OCI image per service (Spring Boot Cloud Native Buildpacks — no Dockerfiles) and bring the whole stack up with a single command. The containerized services sit behind the same docker-compose network as Postgres / Redis / RabbitMQ / Zipkin, so you get a fully self-contained demo environment.

**Build the images** (first run pulls ~500 MB of buildpack layers, ~15 min; repeat runs are minutes):

```bash
export JAVA_HOME=~/.sdkman/candidates/java/21.0.4-tem
./mvnw -pl common-lib -DskipTests install
./mvnw -pl config-service,discovery-service,gateway,user-service,product-service,order-service,notification-service \
       -DskipTests spring-boot:build-image
docker images | grep spring-microservices-lab   # 7 images tagged 0.1.0-SNAPSHOT
```

Image names follow `spring-microservices-lab/<service>:0.1.0-SNAPSHOT` (configured once at the parent POM's `spring-boot-maven-plugin`; decision #29).

**Bring the stack up.** The Spring services live in a Compose `apps` profile so `docker compose up -d` still means "infra only" for the local-JVM workflow above. Opt in explicitly:

```bash
docker compose --profile apps up -d --build
docker compose ps          # 12 containers: 5 infra + 7 Spring
```

First boot waits on Postgres/Rabbit/Redis healthchecks before starting the Spring services; registration with Eureka takes another 30–60 s.

**Smoke test the containerized stack** (gateway is the only Spring port mapped to the host — business services stay internal, decision #30):

```bash
TOKEN=$(curl -sf -X POST http://localhost:8080/api/v1/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"email":"ada@example.com","password":"Demo@1234"}' | jq -r '.data.token')

curl -sf http://localhost:8761/eureka/apps -H 'Accept: application/json' \
  | jq -r '.applications.application[].name' | sort
# → GATEWAY, NOTIFICATION-SERVICE, ORDER-SERVICE, PRODUCT-SERVICE, USER-SERVICE

PRODUCT_ID=$(curl -sf -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products | jq -r '.data[0].id')
USER_ID=$(curl -sf -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/users | jq -r '.data[0].id')

curl -sf -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"userId\":\"$USER_ID\",\"productId\":\"$PRODUCT_ID\",\"quantity\":1}" | jq

docker compose logs --tail=20 notification-service | tail -5
# → JSON log line with "service":"notification-service" confirming the event was consumed
```

**Edit config without rebuilding the image.** The repo's `config-repo/` directory is bind-mounted read-only into config-service at `/config-repo`, overridden via `CONFIG_REPO_PATHS=file:/config-repo`. Edit a YAML and broadcast a refresh:

```bash
curl -X POST http://localhost:8080/actuator/busrefresh   # hits user-service; any bus-enabled service works
```

**Teardown:**

```bash
docker compose --profile apps down        # stop all 12, keep volumes
docker compose --profile apps down -v     # + wipe DBs for a clean next boot
```

---

## 5. Dashboards & UIs

| UI | URL | Default creds |
|---|---|---|
| Eureka | http://localhost:8761 | — |
| RabbitMQ | http://localhost:15672 | `guest` / `guest` |
| pgAdmin | http://localhost:5050 | `admin@local.dev` / `admin` |
| Zipkin | http://localhost:9411 | — |
| **Aggregated Swagger (via gateway)** | http://localhost:8080/swagger-ui.html | — |
| user-service Swagger (direct) | http://localhost:8081/swagger-ui.html | — |
| product-service Swagger (direct) | http://localhost:8082/swagger-ui.html | — |
| order-service Swagger (direct) | http://localhost:8083/swagger-ui.html | — |

When you add pgAdmin's first server, use host `ms-postgres` (container DNS, not `localhost`) and any of the per-service usernames (`userdb_user`, `productdb_user`, `orderdb_user`, `notificationdb_user`) with the password from `.env`.

---

## 6. Feature walkthrough — test every Phase

Everything below hits the **gateway at :8080**. That's the point of the gateway: one public surface, JWT enforced at the edge, routing to the right downstream.

### 6.1 Seeded demo users & products

| Email | Password | UUID |
|---|---|---|
| `ada@example.com` | `Demo@1234` | `00000000-0000-7000-8000-000000000001` |
| `alan@example.com` | `Demo@1234` | `00000000-0000-7000-8000-000000000002` |

| Product | UUID | Stock |
|---|---|---|
| Notebook | `00000000-0000-7000-8000-00000000aa01` | 50 |
| Mechanical keyboard | `00000000-0000-7000-8000-00000000aa02` | 15 |
| Coffee beans | `00000000-0000-7000-8000-00000000aa03` | 100 |

### 6.2 Phase 5 — JWT login & gateway enforcement

**Login (public path, no token required):**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ada@example.com","password":"Demo@1234"}' | jq -r '.data.token')
echo "$TOKEN"
```

**Hitting a protected path without a token → 401:**

```bash
curl -i http://localhost:8080/api/v1/orders
# HTTP/1.1 401
```

**Same path with the token → 200:**

```bash
curl -s http://localhost:8080/api/v1/orders -H "Authorization: Bearer $TOKEN" | jq .
```

### 6.3 Phase 3 — User CRUD (user-service)

```bash
# List seeded users
curl -s http://localhost:8080/api/v1/users -H "Authorization: Bearer $TOKEN" | jq .

# Create a new user
curl -s -X POST http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"email":"grace@example.com","password":"Demo@1234","firstName":"Grace","lastName":"Hopper"}' | jq .

# Fetch by id
curl -s http://localhost:8080/api/v1/users/00000000-0000-7000-8000-000000000001 \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Password hashes are BCrypt — inspect with pgAdmin:
```sql
SELECT email, password_hash FROM users;
```

### 6.4 Phase 3 — Products & Redis cache

```bash
# First call hits Postgres, populates Redis
time curl -s http://localhost:8080/api/v1/products -H "Authorization: Bearer $TOKEN" > /dev/null

# Second call is served from Redis — typically < 10ms
time curl -s http://localhost:8080/api/v1/products -H "Authorization: Bearer $TOKEN" > /dev/null
```

Confirm the cache entry:

```bash
docker exec ms-redis redis-cli KEYS 'products::*'
```

Cache invalidation on write — two flavours:

```bash
# PUT = full replace. All four fields are required.
curl -s -X PUT http://localhost:8080/api/v1/products/00000000-0000-7000-8000-00000000aa01 \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Notebook","description":"A5 hardcover","priceCents":1499,"stock":50}' | jq .

# PATCH = partial update. Send only the fields you want to change; nulls/omissions
# are ignored (MapStruct nullValuePropertyMappingStrategy = IGNORE).
curl -s -X PATCH http://localhost:8080/api/v1/products/00000000-0000-7000-8000-00000000aa01 \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"priceCents":1499}' | jq .

docker exec ms-redis redis-cli KEYS 'products::*'   # now empty — both verbs @CacheEvict
```

### 6.5 Phase 4 — Order placement + async event + notification consume

```bash
# Place an order
curl -s -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{
        "userId":"00000000-0000-7000-8000-000000000001",
        "productId":"00000000-0000-7000-8000-00000000aa01",
        "quantity":1
      }' | jq .
```

What just happened:

1. Gateway validated the JWT.
2. order-service called product-service (via Eureka-load-balanced `RestClient`) to reserve stock.
3. After the DB commit, order-service published `OrderCreatedV1` to the `domain.events` topic exchange on RabbitMQ.
4. notification-service consumed the event and logged it.

**Watch the consumer:** look at the notification-service terminal — you should see a line like:
```
Received OrderCreatedV1 ... orderId=... userId=...
```

**Inspect RabbitMQ:** http://localhost:15672 → Exchanges → `domain.events` → message rate spike; Queues → `notification.order-created` → consumer = 1.

**Stock decrement sanity:**

```bash
curl -s http://localhost:8080/api/v1/products/00000000-0000-7000-8000-00000000aa01 \
  -H "Authorization: Bearer $TOKEN" | jq '.data.stock'
# Went from 50 → 49
```

### 6.6 Phase 4 — Resilience4j circuit breaker

Stop product-service (Ctrl+C in its terminal), then place another order:

```bash
curl -i -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{
        "userId":"00000000-0000-7000-8000-000000000001",
        "productId":"00000000-0000-7000-8000-00000000aa01",
        "quantity":1
      }'
```

**How the breaker is tuned** (`config-repo/order-service.yml`): sliding window of 10 calls, `minimum-number-of-calls: 5`, `failure-rate-threshold: 50%`. That means you need to fire **at least 5 orders** while product-service is down before the breaker evaluates; once ≥50% of those calls fail, it opens and the fallback kicks in (orders return a degraded error instead of hanging).

Check the breaker state via its **dedicated endpoint** (not `/actuator/health` — `show-details: when_authorized` in `config-repo/application.yml` hides the components object for unauthenticated calls):

```bash
curl -s http://localhost:8083/actuator/circuitbreakers | jq .
# Look for .circuitBreakers.productService.state: CLOSED | OPEN | HALF_OPEN
```

Restart product-service — the breaker transitions `OPEN → HALF_OPEN → CLOSED` within ~10s (per `wait-duration-in-open-state`).

### 6.7 Phase 4 — Dead-letter queue & idempotency

notification-service dedupes by `event_id` (PK). Re-publish the same event from the RabbitMQ UI's "Publish message" panel with the same headers — notification-service will log `duplicate, skipping` and not double-process.

For DLQ: the integration tests (see §7) force consumer failure to verify routing to `notification.order-created.dlq`. You can inspect the queue in the RabbitMQ UI.

### 6.8 Phase 2 — Config refresh via Cloud Bus

Edit a property in `config-repo/product-service.yml` (e.g., a log level), commit, then hit `busrefresh` on **any service that's wired to Cloud Bus** (the gateway isn't — it lacks `spring-cloud-starter-bus-amqp`, so hitting `:8080/actuator/busrefresh` returns 500). Any business service works:

```bash
curl -X POST http://localhost:8081/actuator/busrefresh   # user-service
# …or :8082, :8083, :8084 — any of them broadcasts to the bus
```

Response is `204 No Content`. RabbitMQ then broadcasts to every `@RefreshScope` bean across all services. Only changed props are reloaded — no restart needed.

### 6.9 Phase 5 — Aggregated Swagger UI

Open http://localhost:8080/swagger-ui.html. Top-right dropdown lists `user-service`, `product-service`, `order-service`. Each spec is fetched via the gateway at `/v3/api-docs/{service}`.

**Authorize before "Try it out".** Every protected endpoint needs a JWT or the gateway returns 401. Workflow:

1. Fetch a token (§6.2 `/api/v1/auth/login`).
2. Click the **Authorize** button (top-right of the UI). Paste the raw token value (no `Bearer ` prefix). Close the dialog.
3. Every subsequent "Try it out" → "Execute" sends `Authorization: Bearer <token>`.

**Server URL.** Each spec's `servers[0].url` is forced to `http://localhost:8080` (the gateway) by `OpenApiAutoConfiguration` in `common-lib`. Without that override, springdoc would advertise the backend's LAN IP/port (e.g., `http://192.168.0.149:8083`), which the browser can't reach and which would also bypass the gateway's JWT enforcement. To customize (e.g., a remote deployment), set `common.openapi.gateway-url` in `config-repo/application.yml`.

Raw spec fetch (public — no JWT needed):

```bash
curl -s http://localhost:8080/v3/api-docs/user-service | jq '.info.title'
curl -s http://localhost:8080/v3/api-docs/user-service | jq '.servers'
# Expect: [{"url":"http://localhost:8080","description":"Gateway"}]
```

### 6.10 Phase 6 — Tracing (Micrometer → Zipkin)

Distributed tracing is wired through `common-lib` via Micrometer Tracing (Brave bridge) + `zipkin-reporter-brave`. Brave's MDC scope decorator seeds `traceId` / `spanId` on every request — no custom correlation filter — and those slots are read by both the `ApiResponse` envelope and the log pattern.

**Verify all three propagation points with one request:**

```bash
# 1. Envelope carries traceId
curl -s http://localhost:8080/api/v1/products -H "Authorization: Bearer $TOKEN" | jq '.traceId'
# → non-null 32-char hex
```

```bash
# 2. Same traceId in the service log line
#    Tail the product-service terminal — Boot's default pattern prints
#    [traceId,spanId] next to the thread name when Micrometer Tracing is on the classpath.
```

```bash
# 3. Zipkin UI shows the full span graph
open http://localhost:9411
#    Run Query → pick the trace → see gateway → product-service spans linked
```

**Best multi-service demo:** `POST /api/v1/orders` fans out gateway → order-service → product-service (stock reserve) → RabbitMQ publish, so the Zipkin trace has 3+ hops. Sampling is 100% in dev (`config-repo/application.yml`) and 10% in `prod`.

### 6.11 Phase 6 — Actuator per-profile exposure

Actuator surface is defined centrally in `config-repo/application.yml`.

| Profile | Exposed endpoints | Health details |
|---|---|---|
| default (`local`, `docker`) | `health,info,refresh,busrefresh,env,metrics` | `when_authorized` — components are hidden for unauthenticated callers (see §6.6) |
| `prod` | `health,info,metrics` | `never` — components are never shown, even to authenticated callers |

**Local (default profile):**

```bash
curl -s http://localhost:8081/actuator | jq '._links | keys'
# → ["busrefresh", "env", "env-toMatch", "health", "health-path", "info", "metrics", "metrics-requiredMetricName", "refresh"]

curl -s http://localhost:8081/actuator/health | jq .
# status UP (components hidden for unauthenticated callers — same behavior as §6.6)
```

**Prod profile** — flip one service over to verify the narrowed surface:

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw -pl user-service spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Dspring.flyway.enabled=false"
```

*Why the `flyway.enabled=false` override:* if the DB already has migration history from a prior `local`/`docker` run, Flyway will refuse to start under `prod` because the seed migrations (`db/seed/*`) are in history but not in `prod`'s classpath scan. That's Flyway protecting you from a dev-seeded prod DB. Disabling Flyway for this one smoke test is fine — the schema is already present; JPA `ddl-auto: validate` passes. See §9 Troubleshooting for the production-clean alternatives.

```bash
curl -s http://localhost:8081/actuator | jq '._links | keys'
# → ["health", "health-path", "info", "metrics", "metrics-requiredMetricName"]

curl -s http://localhost:8081/actuator/health | jq .
# status UP, no components object — `never` is stricter than `when_authorized`:
# even an authenticated caller with actuator role wouldn't see the details.

curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8081/actuator/env
# → 404
```

Stop it (`Ctrl+C`) and restart the normal `local`-profile JAR when done.

### 6.12 Phase 6 — Structured JSON logs (docker / prod)

Shared `common-lib/src/main/resources/logback-spring.xml` emits:

- **default** (local dev) → Boot's plain console pattern with `[traceId,spanId]` inline.
- **`docker` or `prod`** → `logstash-logback-encoder` JSON, one record per line, with MDC slots (`traceId`, `spanId`), a `service` field pinned to `spring.application.name`, and ISO-8601 `@timestamp`.

**Quick local flip:**

```bash
SPRING_PROFILES_ACTIVE=docker ./mvnw -pl user-service spring-boot:run
```

Each console line is now a single JSON object:

```json
{"@timestamp":"2026-04-17T…","level":"INFO","logger_name":"…","message":"Started UserServiceApplication…","service":"user-service","traceId":"…","spanId":"…"}
```

Eyeball: `service` field present, `traceId`/`spanId` populated on request-scoped lines (make a request and grep), `@timestamp` is UTC ISO-8601.

**Via full docker compose:** every service already runs with `SPRING_PROFILES_ACTIVE=docker`, so `docker compose logs user-service | jq .` parses cleanly — that's the shape an ELK/Loki stack would ingest.

---

## 7. Run the automated test suites

### All modules

```bash
./mvnw clean verify
```

This runs:

- Unit tests in every module.
- **Testcontainers integration tests** (they boot real Postgres/Redis/RabbitMQ images, so Docker must be running):
  - `user-service`: DB + JWT issuance tests
  - `product-service`: repository + Redis cache tests + `ProductPatchTests` (partial-update + eviction)
  - `order-service`: order placement, CB fallback, event publish (Awaitility)
  - `notification-service`: event consume, duplicate dedupe, DLQ routing
- `gateway`: `GatewayAuthTests` — public path, protected path, valid token, OpenAPI doc path.
- Spotless format check (`./mvnw spotless:apply` fixes formatting if this fails).
- **JaCoCo 70% line-coverage gate** (Phase 6) on `service` + `domain` packages of the four business modules. Infra modules (`common-lib`, `config-service`, `discovery-service`, `gateway`) skip the check via `jacoco.check.skip=true`. HTML report per module: `<module>/target/site/jacoco/index.html`.

GitHub Actions (`.github/workflows/ci.yml`) runs the same `./mvnw verify` on every push and PR against `develop` / `main`, caches `~/.m2`, and uploads the JaCoCo reports as build artifacts.

### Single module

```bash
./mvnw -pl gateway verify
./mvnw -pl order-service verify
```

### Skip Spotless during fast iteration

```bash
./mvnw -Dspotless.check.skip verify
```

### Manual HTTP flows (IntelliJ / VS Code REST Client)

Open `docs/http/gateway.http` — it's pre-wired with variables for the seeded user/product UUIDs and captures the JWT on login. Run the requests top-to-bottom for a full gateway walkthrough.

---

## 8. Shutdown

```bash
# Ctrl+C every Java service terminal, then:
docker compose down           # stop infra, keep volumes
docker compose down -v        # stop + wipe volumes (fresh DBs on next up)
```

---

## 9. Troubleshooting

| Symptom | Fix |
|---|---|
| `./mvnw verify` fails in `spotless:check` | Run `./mvnw spotless:apply` then retry. If it still fails, make sure you're on JDK 21, not 22+. |
| Service can't reach config-service | Start config-service **first**, then wait until `curl localhost:8888/actuator/health` returns `UP`. |
| Service not in Eureka | Wait 30s after start (Eureka's default registration interval). Check the service logs for `Registering application ... with eureka`. |
| Postgres init didn't create per-service DBs (e.g., `notificationdb` missing) | The init script (`docker/postgres/init/01-create-databases.sh`) only runs on **first boot** with an empty volume. If a newer DB was added later, the old volume won't replay the script. Fix: `docker compose down -v && docker compose up -d`. |
| Seed data missing — no Ada/Alan rows, `products` table empty | Active profile isn't `local`. Log line should read `The following 1 profile is active: "local"`. In IntelliJ: set "Active profiles" in the Run Configuration (§4.5) — **not** via `.env` (spring-dotenv loads too late). From JAR: prepend `SPRING_PROFILES_ACTIVE=local` to the command. |
| IntelliJ launches service but `.env` values are ignored | Working directory is the module folder, not the project root. `spring-dotenv` reads `.env` relative to the JVM cwd. Fix: set Run Configuration **Working directory** to `$PROJECT_DIR$` (§4.5). |
| Tempted to run `./mvnw flyway:repair` | `repair` only fixes the `flyway_schema_history` table (stale checksums, half-applied rows). It does **not** create databases or recover from connection errors. For a broken local env, `docker compose down -v && docker compose up -d` resets every DB cleanly. Only use `repair` when Spring Boot logs a `Validate failed: Migration checksum mismatch` after you've edited an already-applied migration. |
| `SPRING_PROFILES_ACTIVE=prod` boot fails with `Validate failed: Detected applied migration not resolved locally` (usually on a `V9xx__seed_*` file) | The DB already has Flyway history from a `local`/`docker` run where `db/seed/*` was on the classpath. The `prod` profile drops that seed location, so Flyway sees applied migrations with no local match and refuses to start — that's Flyway protecting you from a dev-seeded prod DB. Options: (a) for a quick smoke test of the prod profile (e.g. verifying actuator exposure), append `-Dspring-boot.run.jvmArguments="-Dspring.flyway.enabled=false"` — schema is already there, JPA `validate` passes. (b) to simulate a real prod boot, `docker compose down -v && docker compose up -d` first so the DB starts empty. (c) tolerant middle ground: `-Dspring-boot.run.jvmArguments="-Dspring.flyway.ignore-migration-patterns=*:missing"`. |
| 401 on every call to :8080 | You're missing `Authorization: Bearer <token>`. Log in via `/api/v1/auth/login` first, or hit a public path (`/api/v1/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/actuator/**`). |
| Order succeeds but notification-service logs nothing | Confirm RabbitMQ is healthy; check the `notification.order-created` queue has a consumer; check notification-service logs for connection errors. |
| Circuit breaker never opens | Requires `minimum-number-of-calls: 5` with ≥50% failure rate (see `config-repo/order-service.yml`). Fire 5–10 orders back-to-back while product-service is down. |
| `curl /actuator/health \| jq '.components.circuitBreakers'` returns null | `show-details: when_authorized` hides the components object for unauthenticated callers. Use the dedicated `/actuator/circuitbreakers` endpoint instead. |
| `POST /actuator/busrefresh` on gateway returns 500 | Gateway doesn't include `spring-cloud-starter-bus-amqp`, so the endpoint isn't wired. Hit it on any business service directly (e.g., `:8081`). |
| `/swagger-ui.html` via gateway returns 500 | Happens if `springdoc.api-docs.enabled: false` is set on the gateway — the UI bootstraps by fetching `/v3/api-docs/swagger-config`, and disabling `api-docs` also takes that endpoint offline. Keep `api-docs` enabled; it's harmless for the gateway (no controllers = empty spec). |
| Swagger "Try it out" returns `NetworkError` or hits the backend's LAN IP | `servers[0].url` in each service's spec is advertising the direct backend URL (e.g., `http://192.168.0.149:8083`). Ensure `common-lib`'s `OpenApiAutoConfiguration` is active (springdoc on classpath) or set `common.openapi.gateway-url` explicitly in `config-repo/application.yml`. |
| Swagger "Try it out" returns 401 | You forgot to click **Authorize** and paste a JWT. All protected paths enforce auth at the gateway. |
| `POST /api/v1/auth/login` with `Demo@1234` returns `USER_INVALID_CREDENTIALS` | Seed ran with a bad hash. Fix: `UPDATE users SET password_hash='$2a$10$TnR24j/8I7/0k645N9cp9u3aAolTDWF/ZzgP0bY7YsaDRpez4XVwy' WHERE email IN ('ada@example.com','alan@example.com');` — or `docker compose down -v` + re-up if you have a recent pull with the corrected `R__seed_users.sql`. |
| Redis cache doesn't fill | Check `ms-redis` is healthy and product-service's logs show `Cache 'products' configured`. |

---

## 10. Where to look next

- `docs/PLAN.md` — full roadmap and decision log.
- `docs/adr/` — architecture decision records (per-service DB, shared HMAC JWT, etc.).
- `docs/http/gateway.http` — ready-to-run request collection.
- `config-repo/` — externalized config (edit + `busrefresh` to reload).
