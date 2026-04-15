# Microservice Learning Project — Plan

## Goal
Build a small but realistic e-commerce order system to learn microservice patterns end-to-end: centralized config, service discovery, sync REST, async messaging, caching, containerization, and clean DTO/entity boundaries.

## Tech Stack
| Concern | Choice | Version | Why |
|---|---|---|---|
| Language | Java | **21 (LTS)** | Virtual threads, records, pattern matching |
| Framework | Spring Boot | **3.5.13** | Latest 3.5.x, Java 21 baseline |
| Cloud stack | Spring Cloud | **2025.0.0 (Northfields)** | Compatible with Boot 3.5.x |
| Build | Maven (multi-module) | 3.9+ | Monorepo of services |
| DB | PostgreSQL | **18.3** | DB-per-service (`userdb`, `productdb`, `orderdb`) |
| Cache | Redis | **8.2** (latest stable) | Read-through cache + distributed locks |
| Broker | RabbitMQ | **4.1** (latest stable) | Topic/fanout exchanges, event-driven flows |
| Config server | Spring Cloud Config | 2025.0.0 | Centralized, git-backed configuration |
| Discovery | Netflix Eureka | 2025.0.0 | Service registry + client-side LB |
| Gateway | Spring Cloud Gateway (MVC) | 2025.0.0 | Edge routing, filters, rate limiting |
| Mapping | MapStruct | 1.6.3 | Compile-time DTO ↔ entity mapping |
| Boilerplate | Lombok | 1.18.36 | Reduce getter/setter/builder noise |
| Env loading | `spring-dotenv` | 4.0.0 | `.env` support in `application.yml` |
| Containers | Docker + Docker Compose v2 | — | One-command local stack |
| Migrations | Flyway | 11.x | Versioned per-service migrations |
| API docs | springdoc-openapi | 2.6.x | Auto Swagger UI per service |
| Observability | Actuator + Micrometer + Zipkin | — | Tracing across services |

> **Compatibility note:** Spring Boot 3.5.x pairs with Spring Cloud **2025.0.x** — do not mix with 2024.0.x. Verify against the official compatibility matrix before bumping either.

## Architecture

```
                                 ┌────────────────────┐
                                 │  config-service    │  (Spring Cloud Config, git-backed)
                                 │  :8888             │
                                 └─────────┬──────────┘
                                           │ all services pull config at boot
                                           ▼
                                 ┌────────────────────┐
                                 │ discovery-service  │  (Eureka Server)
                                 │  :8761             │
                                 └─────────┬──────────┘
                                           │ register / lookup
   ┌───────────────────────────────────────┼───────────────────────────────────────┐
   │                                       │                                       │
   ▼                                       ▼                                       ▼
┌─────────────┐  REST   ┌──────────┐   ┌──────────────┐   ┌──────────┐   ┌────────────────┐
│   gateway   │────────▶│  user-   │   │  product-    │   │  order-  │   │ notification-  │
│  :8080      │         │  service │   │  service     │   │  service │   │ service        │
└─────────────┘         │  :8081   │   │  :8082       │   │  :8083   │   │  :8084         │
                        └────┬─────┘   └──────┬───────┘   └────┬─────┘   └────────┬───────┘
                             │                │                │                  │
                             ▼                ▼                ▼                  │
                          Postgres      Postgres+Redis      Postgres              │
                                                                │                 │
                                                                │ publishes       │ consumes
                                                                ▼                 │
                                                           ┌──────────┐           │
                                                           │ RabbitMQ │───────────┘
                                                           │  :5672   │
                                                           └──────────┘
```

### Services

| # | Service | Port | Responsibility |
|---|---|---|---|
| 1 | **config-service** | 8888 | Spring Cloud Config Server; serves `application.yml` per service from a local git repo (`config-repo/`). Starts first. |
| 2 | **discovery-service** | 8761 | Eureka Server. All business services register here; gateway resolves downstream via `lb://` URIs. |
| 3 | **gateway** | 8080 | Edge routing, JWT validation, rate limiting, request logging. |
| 4 | **user-service** | 8081 | Registration, JWT issuance, profile CRUD. Postgres. |
| 5 | **product-service** | 8082 | Catalog + stock. Postgres + Redis (read-through, TTL 5m). |
| 6 | **order-service** | 8083 | Creates orders, reserves stock (REST → product-service via Eureka), publishes `OrderCreated` to RabbitMQ. Postgres. |
| 7 | **notification-service** | 8084 | Consumes `OrderCreated`, `OrderCancelled`; logs / fakes email send. Stateless. |

### Boot order
`config-service` → `discovery-service` → business services → `gateway`.
In docker-compose this is enforced with `depends_on` + healthchecks.

### Shared module
`common-lib` — keeps cross-service concerns in one place:
- `ApiResponse<T>` record + `ErrorCode` enum + shared `@RestControllerAdvice`.
- Domain event records (JSON-serialized), versioned by class name (`OrderCreatedV1`).
- `BaseEntity` with UUIDv7 PK + JPA auditing columns.
- Correlation-ID filter (propagates `traceId` to MDC).
- JWT-validation filter + Spring Boot auto-configuration — services just add the dep.
- UTC `ObjectMapper` configuration.

Keep it thin — no business logic, no service-specific types.

### Config repo
A `config-repo/` subdirectory inside this monorepo, consumed by `config-service` via `file://` URL in the `git` profile (or as a plain directory in the `native` profile). Migrating it to a standalone repo later is a URL change — no code impact.
```
config-repo/
├── application.yml              # shared defaults (actuator, logging)
├── user-service.yml
├── product-service.yml
├── order-service.yml
├── notification-service.yml
└── gateway.yml
```
Secrets are **not** here — they come from `.env` → env vars → Spring placeholders.

## Module Layout (Maven multi-module)

```
Redis-RabbitMQ-Micro-service/
├── pom.xml                     # parent, dependencyManagement (Boot + Cloud BOMs)
├── docker-compose.yml
├── .env.example
├── common-lib/
├── config-service/
├── discovery-service/
├── gateway/
├── user-service/
├── product-service/
├── order-service/
├── notification-service/
├── config-repo/                # git-backed config (see Config repo section)
├── docker/                     # infra side-files (Postgres init SQL, etc.)
├── scripts/                    # smoke.sh, migration helpers, etc.
└── docs/
    ├── PLAN.md
    ├── adr/                    # architecture decision records
    └── http/                   # .http files for manual/scripted testing
```

Per-service internal layout:
```
src/main/java/.../<service>/
├── api/          # controllers, DTOs
├── domain/       # entities, value objects
├── repository/   # JPA repositories
├── service/      # business logic
├── mapper/       # MapStruct mappers
├── messaging/    # Rabbit publishers/listeners
├── client/       # Eureka-aware REST clients
├── config/       # beans, Redis, Rabbit, security config
└── Application.java
```

## Phased Roadmap

### Phase 0 — Project Bootstrap (one-time setup)
Runs **before any code**. Establishes the repo + branch structure so every later commit is tracked from the start.
- [x] `git init`.
- [x] Add `.gitignore` (Java, Maven `target/`, IntelliJ `.idea/`, `.env`, logs, build artifacts).
- [x] Add `LICENSE` (MIT) and `.editorconfig` (UTF-8, LF, 4-space Java / 2-space YAML).
- [x] Seed `docs/adr/0000-template.md` for future ADRs.
- [x] First commit on `main`: `docs(plan): initial project plan and workflow` (includes `docs/PLAN.md`, `.gitignore`, `LICENSE`, `.editorconfig`).
- [x] Create long-lived `develop` branch off `main`.
- [x] Tag `v0.0.0` on `main` (baseline marker, optional).
- [x] Push to GitHub; enable branch protection on `main` and `develop` (require PR, require checks, no direct commits).
- [x] Branch `feature/p1-foundation` off `develop` — Phase 1 work starts here.

**Acceptance:** `git log --oneline --all` shows the initial commit on `main` and `develop`; branch protection is active on GitHub.

### Phase 1 — Foundation (skeleton + infra)
- [x] Parent `pom.xml` with Spring Boot 3.5.13 BOM, Spring Cloud 2025.0.0 BOM, Java 21, Lombok, MapStruct, Spotless + Google Java Format, `spring-boot:build-image` goal.
- [x] `docker-compose.yml` with Postgres 18.3 (single container, init SQL creates `userdb`/`productdb`/`orderdb`), Redis 8.2, RabbitMQ 4.1 + management UI, **Zipkin**, **pgAdmin**. Internal-only network for business services.
- [x] `.env.example` documenting every required variable (DB creds, JWT secret, RabbitMQ creds).
- [x] `common-lib`: `ApiResponse<T>`, `ErrorCode` enum, `@RestControllerAdvice`, base event records, correlation-ID filter, JWT-validation filter + auto-config, `BaseEntity` (UUIDv7 + JPA auditing), UTC `ObjectMapper` config.
- [x] Top-level `README.md` with quickstart.

### Phase 2 — Platform services (config + discovery + bus)
- [x] `config-service` with git-backed `config-repo/` (`native` profile for local, `git` via `file://` for docker/prod).
- [x] `discovery-service` (Eureka Server, self-preservation off in dev).
- [x] **Spring Cloud Bus + RabbitMQ** wired so `/actuator/busrefresh` broadcasts config changes.
- [x] Minimal `user-service` stub that pulls config from config-service and registers with Eureka — proves the platform works.

### Phase 3 — Persistence + caching + testing
- [x] `user-service` end-to-end: `/api/v1/users` — entity → Flyway migration → repo → service → MapStruct mapper → controller → Swagger, BCrypt password hashing.
- [x] `product-service` with CRUD + Redis `@Cacheable` via Lettuce, cache eviction on update/delete, TTL 5m.
- [x] Verify cache via `redis-cli MONITOR`.
- [x] **Testcontainers** introduced: `AbstractIntegrationTest` base class; first `@SpringBootTest` with `@ServiceConnection` for Postgres + Redis.
- [x] Flyway `R__seed_*.sql` for demo data (active in `local`/`docker`).

### Phase 4 — Async messaging + resilience
- [ ] `order-service`: place-order flow at `/api/v1/orders`.
- [ ] Sync call to `product-service` for stock reservation via `RestClient` + Eureka load balancer.
- [ ] **Resilience4j** circuit breaker + fallback on the stock-reservation call.
- [ ] Publish `OrderCreatedV1` (JSON) to topic exchange `domain.events`, routing key `order.created`.
- [ ] `notification-service` consumes with **manual ack**, DLQ + exponential backoff, idempotent via event-id dedupe table.
- [ ] Integration test covers full cross-service flow with Awaitility.

### Phase 5 — Gateway + security
- [ ] Spring Cloud Gateway MVC: routes via `lb://user-service` etc.
- [ ] JWT auth filter at gateway (shared HMAC); downstream services validate via `common-lib` filter.
- [ ] **Redis-backed `RequestRateLimiter`** — 100/min per IP, 1000/min per authenticated user.
- [ ] **Aggregated Swagger UI** at `localhost:8080/swagger-ui` via springdoc.

### Phase 6 — Observability + polish
- [ ] Actuator exposure per profile (open in `local`/`docker`, restricted + basic-auth in `prod`).
- [ ] Micrometer Tracing → Zipkin; `traceId` propagated into MDC and `ApiResponse`.
- [ ] Structured JSON logs (`logstash-logback-encoder`) in `docker`/`prod`.
- [ ] Minimal GitHub Actions workflow: `mvn verify` on push/PR.
- [ ] Hit 70% line coverage across domain + service packages.

## Key Learning Targets
- Centralized config + dynamic refresh via `@RefreshScope` and **Spring Cloud Bus** over RabbitMQ (`/actuator/busrefresh`).
- Service discovery, client-side load balancing, handling instance failure.
- DB-per-service and why you can't join across services.
- Sync vs async: when REST is fine, when events win.
- Cache-aside pattern with Lettuce + invalidation pitfalls.
- At-least-once delivery, manual acks, idempotent consumers, DLQs.
- Circuit breakers + fallback (Resilience4j).
- Gateway concerns: routing via Eureka, JWT, Redis-backed rate limiting, aggregated Swagger.
- Distributed tracing: `traceId` from Micrometer → MDC → Zipkin → response envelope.
- Compile-time mapping with MapStruct (no reflection cost).
- Lombok boundaries: don't `@Data` on JPA entities.

## Conventions
- **DTOs** never leak JPA entities past the service layer — MapStruct maps at the boundary.
- **API routes** always prefixed `/api/v1/...`.
- **IDs** are UUIDv7, generated in-app (not by DB).
- **Timestamps** always UTC, ISO-8601 with millis.
- **HTTP responses** always wrapped in `ApiResponse<T>` envelope (defined in `common-lib`).
- **Migrations** live in `src/main/resources/db/migration`, one version per PR.
- **Events** are immutable records in `common-lib`, versioned by class name suffix (`OrderCreatedV1`).
- **Config**: non-secret config in `config-repo/`; secrets via `.env` → env vars → `${...}` placeholders. Never commit secrets.
- **Service-to-service calls** go through Eureka (`lb://service-name`), never hardcoded hosts.
- **Logs** are plain text in `local`, structured JSON in `docker`/`prod`.
- **Commits** follow Conventional Commits (`feat`, `fix`, `chore`, …); see Git Workflow section.
- **Ports** fixed per service (see Services table above).

## Out of Scope (for now)
- Kubernetes / Helm.
- Service mesh (Istio/Linkerd).
- Saga orchestration framework (stick to manual event choreography).
- Frontend.

## Decisions (all locked in)
| # | Topic | Decision |
|---|---|---|
| 1 | Config backend | `native` profile for local dev, `git` profile for prod-like. Same `config-repo/` directory, different Spring profile. |
| 2 | Service registry | **Netflix Eureka**. |
| 3 | Gateway flavor | **Spring Cloud Gateway MVC** (servlet). |
| 4 | Postgres topology | **Single container, multiple databases** (init SQL creates them). |
| 5 | JWT strategy | **Shared HMAC secret** initially; revisit JWKS later. |
| 6 | Config refresh | **Spring Cloud Bus + RabbitMQ** — `/actuator/busrefresh` broadcasts. |
| 7 | Distributed tracing | **Zipkin** (docker-compose) + Micrometer Tracing bridge. |
| 8 | Java base package / groupId | `com.learning.microservice`. |
| 9 | Config repo location | **Subdirectory** `config-repo/` in monorepo. |
| 10 | DB isolation | **DB-per-service** (`userdb`, `productdb`, `orderdb`) with own creds. |
| 11 | common-lib scope | Ships **shared JWT-validation filter + auto-config**. |
| 12 | Event serialization | **JSON (Jackson)**. |
| 13 | Dev ergonomics | **Spring Boot DevTools** + **pgAdmin**. |
| 14 | Testcontainers | Introduced in **Phase 3**. |
| 15 | RabbitMQ topology | **Single topic exchange** `domain.events` with domain routing keys. |
| 16 | API versioning | All routes prefixed **`/api/v1/...`**. |
| 17 | ID strategy | **UUIDv7**, generated in-app. |
| 18 | Auditing | `BaseEntity` with `createdAt`, `updatedAt`, `createdBy`, `updatedBy` via JPA auditing. |
| 19 | Delete strategy | **Hard delete**. |
| 20 | Logging | Plain text in `local`, **structured JSON** in `docker`/`prod`. |
| 21 | CI | Minimal **GitHub Actions** in Phase 6. |
| 22 | Testing | Unit + integration, integration-heavy via Testcontainers, **70% coverage target**. |
| 23 | Spring profiles | **`local`**, **`docker`**, **`prod`**. |
| 24 | Error code taxonomy | **Central `ErrorCode` enum** in `common-lib`, domain-namespaced. |
| 25 | Circuit breaker | **Resilience4j** on order → product stock call. |
| 26 | Swagger | **Aggregated at gateway**, single UI. |
| 27 | Rate limiting | **Redis-backed `RequestRateLimiter`** at gateway: 100/min IP, 1000/min user. |
| 28 | Seed data | Flyway **`R__seed_*.sql`** in `local`/`docker` only. |
| 29 | Docker image build | **Spring Boot Cloud Native Buildpacks**. |
| 30 | Port exposure | **Internal-only** business services; only platform/UI ports on host. |
| 31 | Redis client | **Lettuce**. |
| 32 | RabbitMQ ack mode | **Manual ack** with DLQ on `basicNack`. |
| 33 | Code style | **Spotless** + **Google Java Format**, enforced on `mvn verify`. |
| 34 | Actuator exposure | Open in `local`/`docker`; restricted + basic auth in `prod`. |
| 35 | Password hashing | **BCrypt** strength 12. |
| 36 | README | Top-level quickstart after Phase 1. |

## API Response Format

Every HTTP endpoint returns a consistent envelope. Defined once in `common-lib`, serialized via Jackson.

### Success
```json
{
  "success": true,
  "data": { ... },
  "message": "Order created",
  "timestamp": "2026-04-15T10:23:45.123Z",
  "path": "/api/v1/orders",
  "traceId": "a1b2c3d4e5f6"
}
```

### Error
```json
{
  "success": false,
  "data": null,
  "message": "Product not found",
  "errorCode": "PRODUCT_NOT_FOUND",
  "errors": [
    { "field": "productId", "message": "No product with id 42" }
  ],
  "timestamp": "2026-04-15T10:23:45.123Z",
  "path": "/api/v1/orders",
  "traceId": "a1b2c3d4e5f6"
}
```

### Rules
- **Envelope type:** `ApiResponse<T>` record in `common-lib`, with static factories `ApiResponse.ok(data)` / `ApiResponse.error(code, message, errors)`.
- **`traceId`** is pulled from the Micrometer tracing context (same id visible in Zipkin).
- **`errorCode`** is an enum (`ErrorCode`) — machine-readable, stable across versions; `message` is human-readable and may change.
- **`errors`** is populated for validation failures (`MethodArgumentNotValidException`) — one entry per invalid field.
- **HTTP status** still carries semantic meaning (200/201/400/404/409/500). The envelope is in addition to, not instead of.
- **Pagination** responses wrap a page object in `data`:
  ```json
  { "content": [...], "page": 0, "size": 20, "totalElements": 137, "totalPages": 7 }
  ```
- **Events** published to RabbitMQ do **not** use this envelope — they're pure domain event records (`OrderCreatedV1`), since there's no HTTP request/response context.
- **Timestamps** are always UTC, ISO-8601 with millis.
- All services use a single `@RestControllerAdvice` in `common-lib` that maps exceptions → `ApiResponse.error(...)` with the correct HTTP status.

## Testing Strategy

**Both unit and integration tests — but skewed toward integration, since the learning value here is in the wiring.**

| Layer | Type | What to cover | Tools |
|---|---|---|---|
| Domain / service logic | **Unit** | Business rules, validation, state transitions, edge cases. Fast, no Spring context. | JUnit 5, AssertJ, Mockito |
| Mappers | **Unit** | MapStruct round-trips (entity ↔ DTO), null handling, nested mappings. | JUnit 5, AssertJ |
| Repositories | **Slice (`@DataJpaTest`)** | Custom queries, named queries — only where non-trivial. | `@DataJpaTest` + Testcontainers Postgres |
| Controllers | **Slice (`@WebMvcTest`)** | Request validation, status codes, envelope shape — only where controller has real logic (filters, custom advice). | `@WebMvcTest`, MockMvc |
| Service end-to-end | **Integration** | Full `@SpringBootTest` with real Postgres, Redis, RabbitMQ via Testcontainers. Covers wiring, config, Flyway, cache behavior, event publish/consume. | `@SpringBootTest` + Testcontainers |
| Cross-service flows | **Integration** | Happy path: place order → stock reserved → event published → notification consumed. Run via Testcontainers compose module. | Testcontainers, Awaitility |

### Rules
- **Don't unit-test controllers in isolation if the service has no logic** — a `@SpringBootTest` integration test covers the same ground and catches config mistakes.
- **Don't mock the DB or broker** in integration tests — use Testcontainers. (Spring Boot 3.4+ has `@ServiceConnection` which makes this trivial.)
- **One `AbstractIntegrationTest` base class per service** starts the containers once per test class (or reuses a singleton pattern).
- **Coverage target:** 70% line coverage across service + domain packages. Don't chase 100% — diminishing returns.
- **Test data:** builders (`OrderTestData.aValidOrder().build()`) over fixtures.
- **Event assertions** use Awaitility for async wait (`await().atMost(5, SECONDS).untilAsserted(...)`).

## Phase Acceptance Criteria

Each phase ends with a runnable demo. Tag `v0.X.0` and merge `develop` → `main` **only after** the corresponding acceptance check passes.

| Phase | Acceptance checks |
|---|---|
| **0 — Bootstrap** | Repo initialized, `main` + `develop` pushed with protection rules, `v0.0.0` tagged, `feature/p1-foundation` branched off `develop`. |
| **1 — Foundation** | `docker compose up` brings all infra healthy. `mvn verify` green (Spotless, tests). pgAdmin (5050), RabbitMQ UI (15672), Zipkin (9411) reachable. |
| **2 — Platform services** | `GET http://localhost:8888/user-service/default` returns config. Eureka dashboard (8761) shows `user-service` registered. `POST /actuator/busrefresh` returns 200. |
| **3 — Persistence + caching** | `POST /api/v1/users` then `GET` returns it. `GET /api/v1/products/{id}` twice — second call hits Redis (verify via `redis-cli MONITOR`). Testcontainers integration tests green. |
| **4 — Async messaging** | `POST /api/v1/orders` → notification-service logs the event within seconds. Kill product-service → circuit breaker opens, fallback returned. Poison message lands in DLQ. |
| **5 — Gateway + security** | All calls go through gateway (8080) with valid JWT. Exceed rate limit → HTTP 429. Aggregated Swagger at `localhost:8080/swagger-ui` shows every service. |
| **6 — Observability + polish** | Open Zipkin, trace a full order flow across 3 services. JSON logs in `docker` profile. CI green on PR. Coverage ≥ 70%. |

## API Testing & Endpoint Documentation

Two layers, complementary:

### 1. Live reference — Swagger UI (auto-generated)
- **springdoc-openapi** generates OpenAPI 3.1 from controller annotations.
- **Aggregated at gateway**: `http://localhost:8080/swagger-ui` shows every service's endpoints in one UI (decision #26).
- Always up to date — if code changes, Swagger changes.
- Use for **exploration, schema inspection, one-off calls**.

### 2. Version-controlled test collection — `.http` files
- Plain-text HTTP request files, committed under `docs/http/`, runnable from IntelliJ's built-in HTTP Client (no plugin needed).
- One file per service + one end-to-end scenario file. Environment variables kept in `docs/http/http-client.env.json` (gitignored if they hold tokens).
- Use for **scripted manual testing, regression checks, onboarding**.

Layout:
```
docs/http/
├── http-client.env.json            # {host, jwt, userId, productId} per env
├── _auth.http                      # login → capture JWT into env
├── user-service.http
├── product-service.http
├── order-service.http
├── notification-service.http       # management/health endpoints
└── e2e-place-order.http            # full scenario: login → create product → place order → verify
```

Example `order-service.http`:
```http
### Place order (requires JWT from _auth.http)
POST {{host}}/api/v1/orders
Authorization: Bearer {{jwt}}
Content-Type: application/json

{
  "productId": "{{productId}}",
  "quantity": 2
}

> {% client.global.set("orderId", response.body.data.id); %}

### Get order by id
GET {{host}}/api/v1/orders/{{orderId}}
Authorization: Bearer {{jwt}}
```

### Rules
- **Every new endpoint** ships in the same PR as its `.http` request. No endpoint without a test request.
- **`http-client.env.json`** has three profiles: `local`, `docker`, `ci`. The `host` switches between direct service and gateway URLs.
- **Responses captured via `> {% ... %}` scripts** so later requests can chain (e.g., grab an id, reuse in next call).
- **Swagger stays the source of truth** for schema — `.http` files are the test harness.
- **`.http` files are also indirect documentation** — a new contributor can read them top-to-bottom to understand the API shape.

### Optional extras
- **Postman/Bruno export** later if you want GUI-based testing — springdoc can emit OpenAPI JSON that both tools import.
- **Smoke script** `scripts/smoke.sh` that curls `/actuator/health` for every service — called from the acceptance check of each phase.

## Git Workflow & Progress Tracking

**Model:** Git Flow (lite) — one long-lived `main`, one long-lived `develop`, short-lived `feature/*` branches.

### Branches
| Branch | Purpose | Receives merges from | Protected |
|---|---|---|---|
| `main` | Production-stable, always deployable | `develop` (at milestones) | Yes — no direct commits |
| `develop` | Integration branch, always green | `feature/*` (via PR) | Yes — PR-only |
| `feature/<phase>-<slug>` | One feature or task | — | No |
| `fix/<slug>` | Bug fix off develop | — | No |
| `hotfix/<slug>` | Urgent fix off main | merged into both `main` and `develop` | No |

### Flow

```
main     ●─────────────●───────────────●──────▶  (tagged v0.1.0, v0.2.0 …)
          \           ↑                ↑
           \         merge            merge
            \         │                │
develop      ●───●───●───●────●───●───●────▶
                  ↑   ↑       ↑   ↑
                 PR  PR      PR  PR
                  │   │       │   │
feature/*         ●   ●       ●   ●
```

### Rules
- **Start a feature:** `git switch develop && git pull && git switch -c feature/p3-user-crud`.
- **Merge into develop:** open a PR (even solo), self-review, then **squash-merge**. Delete the feature branch.
- **Promote to main:** when a phase is complete + tested, merge `develop` into `main` with a **merge commit** (preserves milestone), tag `v0.X.0`.
- **Hotfix:** branch `hotfix/*` from `main`, fix, merge into both `main` and `develop`, tag patch version.
- **Never** commit directly to `main` or `develop`.
- **Never** force-push shared branches.

### Commit messages — Conventional Commits
```
<type>(<scope>): <subject>

[optional body]
```
Types: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `perf`, `build`, `ci`.
Scope = module (`user-service`, `common-lib`, `infra`, `docs`).

Examples:
- `feat(user-service): add registration endpoint`
- `fix(order-service): retry on stock-reservation timeout`
- `chore(infra): bump postgres to 18.3`
- `docs(plan): lock in git workflow`

### Branch naming
`<type>/p<phase>-<slug>` — e.g. `feature/p1-foundation`, `feature/p4-order-flow`, `fix/p3-cache-eviction`.

### Tagging
- `v0.0.0` after Phase 0 (bootstrap baseline, optional)
- `v0.1.0` after Phase 1 (foundation)
- `v0.2.0` after Phase 2 (platform services)
- … one minor version per completed phase
- Patch bumps (`v0.1.1`) for hotfixes

### Progress tracking
- **Primary:** tick the `[ ]` checkboxes in the Phased Roadmap of this `PLAN.md` as each item lands on `develop`. Commit the doc update in the same PR.
- **Decisions made mid-flight:** drop an ADR in `docs/adr/NNNN-<slug>.md` (format: Context → Decision → Consequences). Don't retroactively edit the plan — ADRs are the audit trail.
- **Open work:** tracked via GitHub Issues once the repo is pushed (labels: `phase-0`…`phase-6`, `blocker`, `good-first`).

### Automation
- **Pre-commit hook:** `mvn spotless:apply` (added in Phase 1).
- **PR checks** (added in Phase 6 CI): `mvn verify` must pass before merge; coverage gate at 70%.
- **Branch protection** on `main` and `develop`: require PR, require checks to pass, require linear history on develop.

