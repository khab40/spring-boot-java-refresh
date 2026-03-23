# Market Data Lake

Market Data Lake is a full-stack market data platform for catalog-driven data discovery, commerce, entitlements, API-key-based access, and operational monitoring. The application combines a Java 21 Spring Boot backend, a rich React and Next.js web UI, Stripe-based checkout flows, JWT and OAuth2 authentication, admin and audit capabilities, Prometheus and Grafana observability, and a lightweight Apache Airflow component reserved for future ingestion and lake orchestration workflows. Delta Lake is currently isolated from active runtime, development, and deployment while market-data delivery endpoints are served from a preview stub and transactional application state stays in H2.

## Features

- REST API compatible with OpenAPI specification
- Market Data management (CRUD operations)
- Legacy subscription management for Market Data
- User management with profile fields such as email, first name, last name, company, country, and phone number
- Full authentication system with password hashing, email verification, Google OAuth2 sign-in, JWT bearer tokens, and stateless security filters
- Data Catalog service that separates lake-facing catalog items from sellable offers
- Shopping cart and multi-offer Stripe checkout flow
- User entitlement tracking for subscription and one-time offer access
- API key issuance for user registration and login flows
- API key usage tracking with per-product quota enforcement for batch downloads and realtime subscriptions
- Usage history tables that can later support billing, forecasting, and audit reporting
- OTD SQL delivery flow for one-time products with Parquet export, signed download links, and delivery history
- Asynchronous Stripe checkout session creation and webhook-based payment completion
- Preview market-data stubs for runtime and development
- H2 database for transactional local development and containerized runtime
- MinIO-based S3-compatible object storage for generated download files
- Separate Next.js web UI running in its own Docker container
- Separate Apache Airflow standalone container for future workflow orchestration
- Spring Boot Actuator health groups and a custom monitoring summary endpoint
- Prometheus metrics scraping for JVM, HTTP, billing, catalog, usage, and integration health
- Grafana dashboards for local monitoring and operational BI
- Docker containerization
- Comprehensive unit tests

## Documentation

- [Architecture Overview](./ARCHITECTURE.md)
- [Architecture Decision Records](./docs/adr)
- [Architecture Diagrams](./docs/diagrams/architecture-overview.md)

## Core Flows

- Catalog administrators create `DataCatalogItem` records that describe what exists in the lake, how it is queried, and what market-data type it represents.
- Catalog administrators create linked `DataProduct` offers that define price, currency, purchase mode, billing interval, and quota limits for a catalog item.
- Clients create `User` records and query `/api/catalog/items` to discover available lake datasets and their linked offers.
- Credential-based registration sends an email verification link first, and verified users can then log in to receive a JWT for management APIs and an API key for downstream data access.
- Google sign-in uses Spring Security OAuth2 login, links or creates a local user, and then issues the same application JWT and API key pair used by password-authenticated users.
- Checkout requests create `PaymentTransaction` records with `PaymentTransactionItem` cart lines and then start Stripe session creation asynchronously.
- Stripe webhooks finalize payments and grant `UserEntitlement` access for subscriptions and one-time purchases.
- API access registration and login flows mint API keys for users, and each usage event is written into dedicated usage tables while entitlement limits are enforced.
- One-time-delivery users can submit a restricted SQL query, receive Parquet files stored in MinIO, and later access the same signed links from delivery history in the UI.
- The Next.js web UI consumes the backend REST API for signup, signin, catalog browsing, checkout polling, entitlement inspection, and admin audit workflows.
- Airflow is reserved for future ingestion adapters and data-lake-facing orchestration rather than the current request-response path.

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- Docker and Docker Compose

## Getting Started

### Local Development

1. Clone the repository
2. Run `./scripts/local-build.sh` to build the backend jar and frontend bundle with host tooling
3. Run `./scripts/local-run.sh` to start the Spring Boot API and Next.js UI without containerizing the app itself
4. Run `./scripts/local-test.sh` to execute backend and frontend tests with host tooling
5. Run `./scripts/local-shutdown.sh` to stop the local backend and frontend processes

Local backend execution uses:
- H2 for transactional application state
- an in-memory preview stub for market-data responses

The native local scripts are the fastest backend and UI feedback loop. By default they can still start lightweight `fss` and `mailpit` support containers so OTD delivery and email flows remain testable.

For full-stack local runtime with the UI, Mailpit, Airflow, Prometheus, and Grafana included, prefer the Docker workflow below.

### Containerized Runtime

1. Run `docker-compose up -d` or `./scripts/run.sh`
2. Transactional application state is stored in an H2 file mounted at `/data/h2/market-data-lake`
3. Market-data endpoints are served from the in-memory preview stub
4. The separate web UI is available at `http://localhost:3000`
5. Local email capture is available at `http://localhost:8025` through Mailpit
6. MinIO object storage API is available at `http://localhost:9000`
7. MinIO console is available at `http://localhost:9001`
8. Airflow is available at `http://localhost:8081` for future orchestration work
9. Prometheus is available at `http://localhost:9090`
10. Grafana is available at `http://localhost:3001`

### Airflow Configuration

The Docker stack now includes a lightweight Airflow standalone container using the official slim image.

Local defaults:

- Airflow UI: `http://localhost:8081`
- Username: `airflow`
- Password: `airflow`

Override credentials in `.env` if needed:

- `AIRFLOW_USERNAME`
- `AIRFLOW_PASSWORD`

Reserved workflow folders:

- `airflow/dags`
- `airflow/plugins`
- `airflow/logs`

This Airflow setup is intentionally minimal:

- single-container standalone mode
- examples disabled
- no separate Postgres, Redis, scheduler, worker, or triggerer containers
- intended for future ingestion adapters and data lake orchestration

### Monitoring and Health

The stack now uses Spring Boot Actuator and Micrometer with Prometheus for local-friendly observability, plus Grafana for dashboards and operational BI.

Public monitoring endpoints:

- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
- `GET /actuator/health/integrations`
- `GET /actuator/health/business`
- `GET /actuator/prometheus`
- `GET /actuator/mdl`
- `GET /api/health` on the UI container

Coverage includes:

- REST API liveness and readiness
- JVM heap, CPU, process, and HTTP metrics
- API container disk metrics
- Frontend UI reachability
- Airflow reachability
- Stripe configuration status and transaction state counts
- Data catalog inventory counts
- Usage records, download volume, realtime usage, and request totals
- Active entitlements and subscription totals
- Market-data runtime status and preview-row counts

The custom `GET /actuator/mdl` endpoint is used for richer operational summaries such as top usage users and subscription rollups. That keeps user-level detail out of Prometheus label cardinality while still making it available to Grafana and future audit tooling.

### Stripe Configuration

The application now reads Stripe settings from environment variables, and the local helper scripts automatically load them from `.env`.

Set these sandbox values in `.env` before using payment endpoints:

- `STRIPE_API_KEY` for Stripe test-mode checkout session creation
- `STRIPE_WEBHOOK_SECRET` for verified webhook processing
- `APP_BASE_URL` and `SERVER_PORT` for local callback routing

Local sandbox webhook flow:

1. Start the application locally on `http://localhost:8080`.
2. Run `./scripts/run.sh`. It now attempts to start `./scripts/stripe-listen.sh` automatically in the background when Stripe CLI is installed.
3. Stripe CLI will expose a reachable forwarding tunnel to `POST /api/payments/webhook`.
4. Copy the signing secret printed by Stripe CLI into `STRIPE_WEBHOOK_SECRET` in `.env` if needed.

Automatic listener behavior:

- `./scripts/run.sh` starts the Docker stack and then starts Stripe webhook forwarding in the background
- `./scripts/shutdown.sh` stops both the Docker stack and the background Stripe listener
- listener logs are written to `${TMPDIR:-/tmp}/market-data-lake/stripe-listen.log`
- set `MDL_AUTO_STRIPE_LISTEN=false` if you want to skip automatic listener startup

The webhook endpoint used by local forwarding is:

- `POST /api/payments/webhook`

If `STRIPE_WEBHOOK_SECRET` is left blank, webhook payloads can still be parsed for local testing, but signature verification is skipped.

### Authentication Configuration

JWT signing is configured with:

- `security.jwt.secret`
- `security.jwt.expiration-hours`

Email verification and SMTP are configured with:

- `app.auth.verification-base-url`
- `app.auth.verification-expiration-hours`
- `app.mail.from`
- `spring.mail.host`
- `spring.mail.port`
- `spring.mail.username`
- `spring.mail.password`

Google OAuth2 login is configured with:

- `app.auth.oauth2.success-url`
- `app.auth.oauth2.failure-url`
- `app.auth.google.client-id`
- `app.auth.google.client-secret`
- `app.auth.google.scopes`
- `app.auth.google.redirect-uri`

For local Google sandbox-style testing:

1. Create a Google OAuth client in Google Cloud Console.
2. Add `http://localhost:8080/login/oauth2/code/google` as an authorized redirect URI.
3. Set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` in `.env`.
4. Keep `APP_AUTH_OAUTH2_SUCCESS_URL=http://localhost:3000/oauth/callback`.
5. Keep `APP_AUTH_GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google` unless you intentionally changed the backend public URL.
6. Start the stack and use `Continue with Google` in the UI.

In Docker runtime, Mailpit is started automatically for local verification testing:

- SMTP: `localhost:1025`
- Mail UI: `http://localhost:8025`

All management endpoints are protected by bearer authentication unless explicitly documented as public.
Public endpoints include `/api/auth/**`, `/oauth2/**`, `/login/oauth2/**`, `/api/access/register`, `/api/access/login`, `/api/access/usage`, `/api/access/usage/summary`, and `/api/payments/webhook`.

### Web UI

The web UI is implemented in `frontend/` with React, Next.js, and TypeScript. It provides:

- signup, signin, and signout flows
- Google sign-in with OAuth2 callback handling in the frontend
- catalog browsing, item drill-down, and shopping cart management
- Stripe checkout initiation from the cart and transaction polling
- entitlement and API key visibility for signed-in users
- administration views for dashboard, product creation, market-data insertion, and audit activity

## API Endpoints

### Market Data

- `GET /api/market-data` - Get all market data
- `GET /api/market-data/runtime` - Get the current market-data runtime mode and stub status
- `GET /api/market-data/{id}` - Get market data by ID
- `GET /api/market-data/symbol/{symbol}` - Get market data by symbol
- `POST /api/market-data` - Create a new preview market-data row in the stub runtime
- `DELETE /api/market-data/{id}` - Delete market data by ID

### Subscriptions

- `GET /api/subscriptions` - Get all subscriptions
- `GET /api/subscriptions/{id}` - Get subscription by ID
- `GET /api/subscriptions/user/{userId}` - Get subscriptions by user ID
- `POST /api/subscriptions` - Create new subscription
- `DELETE /api/subscriptions/{id}` - Delete subscription by ID

### Users

- `GET /api/users` - Get all users, requires bearer token
- `GET /api/users/{id}` - Get user by ID, requires bearer token
- `POST /api/users` - Create a new user through the management API, requires bearer token
- `GET /api/users/{id}/entitlements` - Get user entitlements, requires bearer token

### Authentication

- `POST /api/auth/register` - Register with password credentials and send an email verification link
- `GET /api/auth/verify-email?token=...` - Verify the email address for a registered user
- `POST /api/auth/resend-verification` - Reissue a verification email for an unverified password-based account
- `POST /api/auth/login` - Authenticate a verified user and receive a JWT plus API key
- `GET /api/auth/me` - Get the currently authenticated user profile
- `GET /api/auth/me/entitlements` - Get the currently authenticated user's entitlements
- `POST /api/auth/logout` - Client-side signout endpoint
- `GET /oauth2/authorization/google` - Start Google OAuth2 login and redirect to Google consent

### Data Catalog

- `GET /api/catalog/items` - Get all catalog items with linked offers
- `GET /api/catalog/items?activeOnly=true` - Get active catalog items only
- `GET /api/catalog/items/{id}` - Get catalog item by ID
- `GET /api/catalog/items/code/{code}` - Get catalog item by code
- `POST /api/catalog/items` - Create a catalog item
- `GET /api/catalog/products` - Get all products
- `GET /api/catalog/products?activeOnly=true` - Get active products only
- `GET /api/catalog/products/{id}` - Get product by ID
- `GET /api/catalog/products/code/{code}` - Get sellable offer by code
- `POST /api/catalog/products` - Create a sellable offer for a catalog item

### Payments

- `POST /api/payments/checkout` - Start an asynchronous Stripe checkout flow
- `GET /api/payments/{id}` - Get payment transaction status
- `POST /api/payments/webhook` - Receive Stripe webhook events

### API Access

- `POST /api/access/register` - Register a user with password credentials and return a new API key
- `POST /api/access/login` - Issue a fresh API key for an existing user email/password login
- `POST /api/access/usage` - Record API usage and enforce purchased limits
- `GET /api/access/usage/summary?apiKey=...&productId=...` - Query remaining quota for a key and product

### Administration

- `GET /api/admin/dashboard` - Get dashboard counts and recent audit activity, requires admin role
- `GET /api/admin/payments` - Get recent payments, requires admin role
- `GET /api/admin/usage` - Get recent usage events, requires admin role

### Example Commerce Flow

1. Register with `POST /api/auth/register`.
2. Open the verification email from Mailpit or your configured SMTP inbox and call `GET /api/auth/verify-email?token=...`.
3. Sign in with `POST /api/auth/login` to get a bearer token and API key.
4. Query `GET /api/catalog/items` to browse catalog metadata and linked offers.
5. Add one or more compatible offers to the shopping cart in the UI or submit a cart payload to `POST /api/payments/checkout`.
6. Poll `GET /api/payments/{id}` until Stripe session creation completes.
7. Let Stripe call `POST /api/payments/webhook` to mark the transaction successful and grant entitlements.
8. Submit usage events through `POST /api/access/usage` and inspect remaining quota with `GET /api/access/usage/summary`.

### Example Google Sign-In Flow

1. Click `Continue with Google` in the web UI or open `GET /oauth2/authorization/google`.
2. Complete Google consent and return to `/login/oauth2/code/google`.
3. The backend links or creates the local `User`, marks the account as verified, and issues a JWT plus API key.
4. The backend redirects to the frontend OAuth callback page.
5. The frontend stores the returned session and resumes the signed-in UI state.

## API Documentation

The API is documented using OpenAPI 3.0. Access the Swagger UI at `http://localhost:8080/swagger-ui.html` when the service is running.

## Database Configuration

### Transactional State
- URL: `jdbc:h2:mem:marketdata` for local execution
- URL: `jdbc:h2:file:/data/h2/market-data-lake` in Docker Compose
- Console: `http://localhost:8080/h2-console`

### Market Data Preview Runtime
- Toggle: `${MARKETDATA_STUB_ENABLED}`
- Default: `true`
- Backing store: in-memory stub data managed by the application runtime

## Domain Model

- `User` stores the customer identity and contact fields used by the commerce flow.
- `User` also stores password hashes, email verification state, roles, and identity-provider fields for local and Google-based authentication.
- `EmailVerificationToken` stores hashed verification tokens, expiration times, and usage timestamps for one-time email confirmation.
- `DataCatalogItem` represents lake-facing metadata such as dataset code, market-data type, storage system, query reference, delivery path, and coverage window.
- `DataProduct` represents a sellable offer linked to a catalog item, including code, price, currency, access type, billing interval, and API usage quotas.
- `PaymentTransaction` tracks asynchronous cart checkout creation and final payment status.
- `PaymentTransactionItem` records each offer and quantity included in a checkout transaction.
- `UserEntitlement` records which offers a user can access, how many units were purchased, and how much quota has already been consumed.
- `ApiKey` stores issued credentials per user without persisting the raw token, only a hash and prefix.
- `ApiKeyUsageRecord` stores auditable usage events for later billing, prediction, and audit reporting.
- `MarketData` is currently served through a preview stub so UI, checkout, and entitlement flows can evolve independently from future lake-storage work.

## Architecture

```mermaid
graph TB
    Client[Browser Clients] --> Frontend[Next.js Web UI]
    Frontend --> Api[Spring Boot REST API]
    Api --> Market[Market Data Controller]
    Api --> Subs[Subscription Controller]
    Api --> Users[User Controller]
    Api --> Auth[Auth Controller]
    Api --> Catalog[Data Catalog Controller]
    Api --> Payments[Payment Controller]
    Api --> Access[API Access Controller]
    Api --> Docs[OpenAPI / Swagger UI]

    Users --> UserSvc[User Service]
    Auth --> AuthSvc[Auth Service]
    AuthSvc --> Jwt[JWT Service]
    Auth --> OAuth2[Google OAuth2 Login]
    OAuth2 --> AuthSvc
    Jwt --> Filter[JWT Authentication Filter]
    Users --> EntSvc[User Entitlement Service]
    Catalog --> CatalogSvc[Data Catalog Service]
    Payments --> PaymentSvc[Payment Service]
    PaymentSvc --> Async[Payment Async Processor]
    Async --> StripeGateway[Stripe Gateway]
    StripeGateway --> Stripe[Stripe Checkout API]
    Stripe --> Webhook[Stripe Webhook]
    Webhook --> Payments
    Payments --> WebhookSvc[Payment Webhook Service]
    WebhookSvc --> EntSvc
    Access --> ApiKeySvc[API Keys Service]
    ApiKeySvc --> KeyRepo[API Key Repositories]
    ApiKeySvc --> EntSvc
    AuthSvc --> ApiKeySvc
    AuthSvc --> UserSvc

    Market --> MarketSvc[Market Data Service]
    MarketSvc --> StubStore[Stub Market Data Store]
    StubStore --> Preview[(Preview Market Data)]
    Subs --> LegacySvc
    LegacySvc --> Repos[Repositories]
    UserSvc --> Repos
    EntSvc --> Repos
    CatalogSvc --> Repos
    PaymentSvc --> Repos
    WebhookSvc --> Repos
    KeyRepo --> Repos
    Repos --> Db[(Relational Transaction Store)]

    subgraph Local
        Db --> H2[H2 In-Memory]
    end

    subgraph Containerized
        Db --> H2File[H2 File Store]
        Api --> Docker[Docker Compose Stack]
        Frontend --> Docker
    end
```

More architecture documentation:
- [Architecture Overview](./ARCHITECTURE.md)
- [Architecture Decision Records](./docs/adr)
- [Architecture Diagrams](./docs/diagrams/architecture-overview.md)

## Project Structure

```
.
├── src/
│   ├── main/
│   │   ├── java/com/example/springbootjavarefresh/
│   │   │   ├── config/          # Mail and application configuration
│   │   │   ├── controller/      # REST API controllers
│   │   │   ├── dto/             # Request and response DTOs
│   │   │   ├── entity/          # Domain entities and JPA models
│   │   │   ├── observability/   # Actuator endpoint, health indicators, custom metrics
│   │   │   ├── repository/      # Relational repositories
│   │   │   ├── security/        # JWT, OAuth2, filters, security config
│   │   │   ├── service/         # Catalog, auth, payments, API keys, admin, market data logic
│   │   │   └── SpringBootJavaRefreshApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/com/example/springbootjavarefresh/
│       │   ├── controller/      # Controller tests
│       │   ├── observability/   # Monitoring endpoint tests
│       │   ├── security/        # JWT and auth tests
│       │   └── service/         # Service-layer tests
│       └── resources/
│           └── application.properties
├── frontend/
│   ├── app/                     # Next.js app router pages, API routes, OAuth callback
│   ├── components/              # React UI components
│   ├── lib/                     # Frontend API client and shared UI types
│   ├── public/                  # Static frontend assets
│   ├── package.json             # Frontend dependencies and scripts
│   └── Dockerfile               # Separate UI container image
├── airflow/
│   ├── dags/                    # Future orchestration DAGs
│   ├── logs/                    # Local Airflow logs
│   └── plugins/                 # Future Airflow plugins
├── monitoring/
│   ├── prometheus/              # Prometheus scrape configuration
│   └── grafana/                 # Provisioned datasources and dashboards
├── docs/
│   ├── adr/                     # Architecture decision records
│   └── diagrams/                # Architecture and ADR diagrams
├── scripts/                     # Build, run, test, logs, Stripe, Airflow helpers
├── docker-compose.yml           # Full local stack definition
├── ARCHITECTURE.md              # Architecture overview
└── README.md
```

## Testing

Run unit tests with:
```bash
mvn test
```

The Dockerized Java 21 test workflow is also verified:
```bash
./scripts/test.sh
```

Current automated coverage includes:
- Controller tests for market data, subscriptions, users, authentication, catalog, payments, and API access
- Service tests for market data, subscriptions, user creation, authentication, catalog creation, API key issuance, entitlement grants, payment initiation, asynchronous checkout, and webhook completion paths
- Full Spring Boot startup test against the H2 configuration

## Building for Production

```bash
mvn clean package
java -jar target/market-data-lake-0.0.1-SNAPSHOT.jar
```

## Docker

Build and run with Docker:
```bash
docker build -t market-data-lake .
docker run -p 8080:8080 market-data-lake
```

Or use the helper scripts in `scripts/`:
```bash
./scripts/build.sh
./scripts/run.sh
./scripts/test.sh
./scripts/logs.sh app
./scripts/airflow-cli.sh dags list
./scripts/shutdown.sh
./scripts/local-build.sh
./scripts/local-run.sh
./scripts/local-test.sh
./scripts/local-shutdown.sh
```

Verified workflow:
- `./scripts/build.sh` builds the backend and frontend Docker images
- `./scripts/run.sh` starts the backend API, frontend UI, Mailpit, Airflow, Prometheus, and Grafana with Docker Compose
- `./scripts/test.sh` runs the Maven backend tests and the Next.js frontend tests
- `./scripts/logs.sh app` tails container logs
- `./scripts/airflow-cli.sh <args>` runs Airflow CLI commands inside the Airflow container
- `./scripts/shutdown.sh` stops and removes the stack
- `./scripts/local-build.sh` builds the backend jar and frontend bundle natively on the host
- `./scripts/local-run.sh` runs the backend on `http://localhost:8080` and the UI on `http://localhost:3000`
- `./scripts/local-test.sh` runs backend and frontend tests natively on the host
- `./scripts/local-shutdown.sh` stops the host-run backend and frontend processes

Native local workflow notes:
- `./scripts/local-run.sh` keeps browser requests same-origin through `/backend`
- the local Next.js server proxies `/backend/*` to `http://localhost:8080`
- Google OAuth login starts directly from `http://localhost:8080` so the backend keeps the OAuth session cookie on the callback origin
- `MDL_LOCAL_SUPPORT=true` starts only `fss` and `mailpit` in Docker for support services
- `MDL_LOCAL_SUPPORT=false` skips even those optional support containers

The Docker image builds with Maven in a build stage and runs on Java 21 JRE in the final stage.

## Render Preview Deployment

The repository now includes [render.yaml](./render.yaml) for a lightweight two-service preview deployment:
- `market-data-lake-api` for the Spring Boot backend
- `market-data-lake-ui` for the Next.js frontend

Recommended preview-mode settings:
- keep `MARKETDATA_STUB_ENABLED=true`
- set `APP_FRONTEND_ORIGIN` to the public UI origin
- set `APP_AUTH_OAUTH2_SUCCESS_URL` to `https://<ui-host>/oauth/callback`
- set `APP_AUTH_OAUTH2_FAILURE_URL` to `https://<ui-host>/?authError=google-signin-failed`
- set `APP_AUTH_GOOGLE_REDIRECT_URI` to `https://<api-host>/login/oauth2/code/google`
- set `NEXT_PUBLIC_AUTH_BASE_URL` on the UI service to the public backend URL
- set `INTERNAL_API_BASE_URL` on the UI service to the public backend URL

Preview deployment limitations on free tiers:
- H2 remains ephemeral unless the platform provides persistent disk
- OTD Parquet delivery still requires a reachable S3-compatible endpoint and bucket
- Mailpit and Airflow stay local-development services rather than part of the preview blueprint

## Contributing

Please read CONTRIBUTING.md for details on our code of conduct and the process for submitting pull requests.

## License

Market Data Lake is licensed under the MIT License. See the LICENSE file for details.
