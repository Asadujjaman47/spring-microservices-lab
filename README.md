# spring-microservices-lab

Learning project: a small e-commerce order system built with Spring Boot + Spring Cloud to practice microservice patterns end-to-end — centralized config, service discovery, sync REST, async messaging, caching, containerization, clean DTO/entity boundaries.

See [`docs/PLAN.md`](docs/PLAN.md) for the full plan, architecture diagram, phased roadmap, and locked-in decisions.

## Tech stack

| Concern | Choice |
|---|---|
| Language | Java 21 (Temurin 21.0.4 via SDKMAN) |
| Framework | Spring Boot 3.5.13 |
| Cloud stack | Spring Cloud 2025.0.0 (Northfields) |
| Build | Maven 3.9+ (wrapper committed) |
| Database | PostgreSQL 18.3 (DB-per-service) |
| Cache | Redis 8.2 (Lettuce) |
| Broker | RabbitMQ 4.1 |
| Tracing | Zipkin + Micrometer Tracing |
| Mapping | MapStruct 1.6.3 |
| Style | Spotless + Google Java Format |

## Prerequisites

- Docker + Docker Compose v2
- JDK 21 (Temurin recommended — `sdk env` picks it up from `.sdkmanrc`)
- No Maven needed: use the committed `./mvnw` wrapper

## Quickstart

```bash
git clone https://github.com/Asadujjaman47/spring-microservices-lab.git
cd spring-microservices-lab
cp .env.example .env                 # edit for anything sensitive
docker compose up -d                 # brings up Postgres/Redis/RabbitMQ/Zipkin/pgAdmin
./mvnw verify                        # builds every module, runs tests + Spotless
```

### Run the platform (local JVM)

Services must start in order — `config-service` first (others pull their config from it), then `discovery-service`, then business services.

```bash
java -jar config-service/target/config-service-0.1.0-SNAPSHOT.jar                      # :8888
java -jar discovery-service/target/discovery-service-0.1.0-SNAPSHOT.jar                # :8761
SPRING_PROFILES_ACTIVE=local java -jar user-service/target/user-service-0.1.0-SNAPSHOT.jar        # :8081
SPRING_PROFILES_ACTIVE=local java -jar product-service/target/product-service-0.1.0-SNAPSHOT.jar  # :8082
SPRING_PROFILES_ACTIVE=local java -jar order-service/target/order-service-0.1.0-SNAPSHOT.jar      # :8083
SPRING_PROFILES_ACTIVE=local java -jar notification-service/target/notification-service-0.1.0-SNAPSHOT.jar  # :8084
java -jar gateway/target/gateway-0.1.0-SNAPSHOT.jar                                    # :8080
```

`SPRING_PROFILES_ACTIVE=local` on the business services activates `db/seed/` repeatable migrations that ship demo users + products.

Smoke test:
```bash
curl http://localhost:8888/user-service/default         # config served
curl http://localhost:8081/api/v1/users                 # seeded users
curl http://localhost:8082/api/v1/products              # seeded products
curl -X POST http://localhost:8081/actuator/busrefresh  # broadcast refresh over RabbitMQ

# Place an order → watch notification-service logs for the consumed event:
PRODUCT_ID=$(curl -s http://localhost:8082/api/v1/products | jq -r '.data[0].id')
USER_ID=$(curl -s http://localhost:8081/api/v1/users | jq -r '.data[0].id')
curl -X POST http://localhost:8083/api/v1/orders \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"$USER_ID\",\"productId\":\"$PRODUCT_ID\",\"quantity\":1}"
```

### OpenAPI / Swagger

| Scope | Swagger UI |
|---|---|
| **Aggregated (via gateway)** | <http://localhost:8080/swagger-ui.html> |
| user-service (direct) | <http://localhost:8081/swagger-ui.html> |
| product-service (direct) | <http://localhost:8082/swagger-ui.html> |
| order-service (direct) | <http://localhost:8083/swagger-ui.html> |

The aggregated UI is the intended entry point — click **Authorize**, paste a JWT from `POST /api/v1/auth/login`, and every service's spec becomes callable through the gateway.

### UIs (local)

| Tool | URL | Default creds |
|---|---|---|
| Eureka dashboard | <http://localhost:8761> | — |
| Config Server | <http://localhost:8888/actuator> | — |
| pgAdmin | <http://localhost:5050> | `admin@local.dev` / `admin` |
| RabbitMQ management | <http://localhost:15672> | `guest` / `guest` |
| Zipkin | <http://localhost:9411> | — |

Postgres (5432), Redis (6379), RabbitMQ AMQP (5672) are also exposed on the host for local development against the DB/broker.

### Per-service databases

The Postgres container auto-creates four databases on first boot, each with its own owner:

| Database | Owner | Purpose |
|---|---|---|
| `userdb` | `userdb_user` | user-service |
| `productdb` | `productdb_user` | product-service |
| `orderdb` | `orderdb_user` | order-service |
| `notificationdb` | `notificationdb_user` | notification-service (dedupe table) |

Passwords come from `.env` — see `.env.example` for the variable names.

## Project layout

```
.
├── docker-compose.yml         # local infra
├── docker/postgres/init/      # first-boot DB provisioning
├── pom.xml                    # parent: Boot + Cloud BOMs, Spotless
├── common-lib/                # shared envelope, errors, tracing, JWT, auditing
├── config-repo/               # Spring Cloud Config source (native/git)
├── config-service/            # Spring Cloud Config Server (:8888)
├── discovery-service/         # Netflix Eureka Server (:8761)
├── gateway/                   # Spring Cloud Gateway MVC, JWT enforcement (:8080)
├── user-service/              # users CRUD, BCrypt (:8081)
├── product-service/           # products CRUD, Redis @Cacheable (:8082)
├── order-service/             # order placement, Resilience4j CB, event publisher (:8083)
├── notification-service/      # event consumer, DLQ + idempotency (:8084)
├── scripts/                   # smoke scripts
└── docs/
    ├── PLAN.md                # authoritative plan
    ├── adr/                   # architecture decision records
    └── http/                  # .http request files for manual testing
```

## Development workflow

- **Branching:** Git Flow (lite) — `main` (protected, release-tagged) ← `develop` ← `feature/p<phase>-<slug>`. See `docs/PLAN.md` § Git Workflow.
- **Commits:** Conventional Commits (`feat`, `fix`, `chore`, `docs`, `test`, …).
- **Formatting:** `./mvnw spotless:apply` before committing; `./mvnw verify` enforces it.

## Status

Phase 6 (observability + polish) complete — GitHub Actions runs `mvn verify` on every push/PR; JaCoCo enforces 70% line coverage at the BUNDLE level on `service` + `domain` packages (infra modules skipped via per-module `jacoco.check.skip`). Micrometer Tracing (Brave bridge) ships with `common-lib` and reports to Zipkin; Brave's MDC scope decorator seeds `traceId` / `spanId`, which the `ApiResponse` envelope and the log pattern read — no custom correlation filter to maintain. Actuator surface is open in `local`/`docker` (`health,info,refresh,busrefresh,env,metrics`) and narrowed to `health,info,metrics` with hidden health details + 10% trace sampling in `prod`. Shared `logback-spring.xml` emits plain text locally and `logstash-logback-encoder` JSON on the `docker`/`prod` profiles, tagged with the service name. Earlier phases remain live: Spring Cloud Gateway MVC fronts every service at `:8080` with HMAC-signed JWT auth + aggregated Swagger (`:8080/swagger-ui.html`), order-service publishes `OrderCreatedV1` via a Resilience4j-wrapped `RestClient` and the `domain.events` topic exchange, and notification-service consumes with idempotent dedupe, Spring AMQP retry, and a DLQ. See [`docs/PLAN.md`](docs/PLAN.md) for the full roadmap.

## License

MIT — see [`LICENSE`](LICENSE).