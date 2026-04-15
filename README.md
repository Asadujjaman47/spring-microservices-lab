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

### Per-service OpenAPI / Swagger

| Service | Swagger UI |
|---|---|
| user-service | <http://localhost:8081/swagger-ui.html> |
| product-service | <http://localhost:8082/swagger-ui.html> |
| order-service | <http://localhost:8083/swagger-ui.html> |

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

The Postgres container auto-creates three databases on first boot, each with its own owner:

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

Phase 4 (async messaging + resilience) complete — order-service places orders over a Eureka-load-balanced `RestClient` to product-service, wraps the stock-reservation call in a Resilience4j circuit breaker with a degraded-fallback, and publishes `OrderCreatedV1` to the `domain.events` topic exchange after commit. notification-service consumes with idempotent dedupe (PK on `event_id`), Spring AMQP retry (exponential backoff, 3 attempts), and a dead-letter queue on final failure. Testcontainers integration tests cover the happy path (Awaitility-verified event publish/consume), CB fallback, duplicate dedupe, and DLQ routing. See [`docs/PLAN.md`](docs/PLAN.md) for the full roadmap.

## License

MIT — see [`LICENSE`](LICENSE).