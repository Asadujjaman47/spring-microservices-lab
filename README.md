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

### Infra UIs (local)

| Tool | URL | Default creds |
|---|---|---|
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

Passwords come from `.env` — see `.env.example` for the variable names.

## Project layout

```
.
├── docker-compose.yml         # local infra
├── docker/postgres/init/      # first-boot DB provisioning
├── pom.xml                    # parent: Boot + Cloud BOMs, Spotless
├── common-lib/                # shared envelope, errors, tracing, JWT, auditing
├── config-repo/               # git-backed Spring Cloud Config (Phase 2)
├── <service>/                 # per-service modules (added per phase)
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

Phase 1 (foundation) in progress — see the checklist in [`docs/PLAN.md`](docs/PLAN.md).

## License

MIT — see [`LICENSE`](LICENSE).