# Market Data Lake

Market Data Lake is a REST API backend service built with Spring Boot for market data delivery, user management, JWT-based authentication, API key access control, data product cataloging, and asynchronous Stripe-backed payment flows. Market data is stored in Delta Lake, while transactional application state stays in a relational store backed by H2 for local and containerized runtime flows.

## Features

- REST API compatible with OpenAPI specification
- Market Data management (CRUD operations)
- Legacy subscription management for Market Data
- User management with profile fields such as email, first name, last name, company, country, and phone number
- Full authentication system with password hashing, JWT bearer tokens, and stateless security filters
- Data Catalog service for listing and managing purchasable data products
- User entitlement tracking for subscription and one-time product access
- API key issuance for user registration and login flows
- API key usage tracking with per-product quota enforcement for batch downloads and realtime subscriptions
- Usage history tables that can later support billing, forecasting, and audit reporting
- Asynchronous Stripe checkout session creation and webhook-based payment completion
- Delta Lake as the main market data storage system
- Market data partitioning by `marketDate` and `dataType`
- H2 database for transactional local development and containerized runtime
- Docker containerization
- Comprehensive unit tests

## Documentation

- [Architecture Overview](./ARCHITECTURE.md)
- [Architecture Decision Records](./docs/adr)
- [Architecture Diagrams](./docs/diagrams/architecture-overview.md)

## Core Flows

- Catalog administrators create `DataProduct` records that define price, currency, purchase mode, and billing interval.
- Catalog administrators can also define quota limits such as batch download megabytes, realtime subscription counts, and payload caps.
- Clients create `User` records and query `/api/catalog/products` to discover available offerings.
- Credential-based registration and login return a JWT for management APIs and an API key for downstream data access.
- Checkout requests create `PaymentTransaction` records immediately and then start Stripe session creation asynchronously.
- Stripe webhooks finalize payments and grant `UserEntitlement` access for subscriptions and one-time purchases.
- API access registration and login flows mint API keys for users, and each usage event is written into dedicated usage tables while entitlement limits are enforced.

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- Docker and Docker Compose

## Getting Started

### Local Development

1. Clone the repository
2. Run `mvn clean install` to build the project
3. Run `mvn spring-boot:run` to start the service
4. Run `mvn test` to execute unit tests

The service will be available at `http://localhost:8080`

Local execution uses:
- H2 for transactional application state
- a filesystem Delta table for market data at `MARKETDATA_DELTA_PATH`

### Containerized Runtime

1. Run `docker-compose up -d` or `./scripts/run.sh`
2. The application container mounts a shared Delta volume at `/data/delta`
3. The application writes market data to `/data/delta/market_data`
4. Transactional application state is stored in an H2 file mounted at `/data/h2/market-data-lake`

The Docker Compose stack includes the official Delta Lake image pinned to the stable `deltaio/delta-docker:4.0.0` tag.

### Stripe Configuration

The application now reads Stripe settings from environment variables, and the local helper scripts automatically load them from `.env`.

Set these sandbox values in `.env` before using payment endpoints:

- `STRIPE_API_KEY` for Stripe test-mode checkout session creation
- `STRIPE_WEBHOOK_SECRET` for verified webhook processing
- `APP_BASE_URL` and `SERVER_PORT` for local callback routing

Local sandbox webhook flow:

1. Start the application locally on `http://localhost:8080`.
2. Run `./scripts/stripe-listen.sh`.
3. Stripe CLI will expose a reachable forwarding tunnel to `POST /api/payments/webhook`.
4. Copy the signing secret printed by Stripe CLI into `STRIPE_WEBHOOK_SECRET` in `.env` if needed.

The webhook endpoint used by local forwarding is:

- `POST /api/payments/webhook`

If `STRIPE_WEBHOOK_SECRET` is left blank, webhook payloads can still be parsed for local testing, but signature verification is skipped.

### Authentication Configuration

JWT signing is configured with:

- `security.jwt.secret`
- `security.jwt.expiration-hours`

All management endpoints are protected by bearer authentication unless explicitly documented as public.
Public endpoints include `/api/auth/**`, `/api/access/register`, `/api/access/login`, `/api/access/usage`, `/api/access/usage/summary`, and `/api/payments/webhook`.

## API Endpoints

### Market Data

- `GET /api/market-data` - Get all market data
- `GET /api/market-data/{id}` - Get market data by ID
- `GET /api/market-data/symbol/{symbol}` - Get market data by symbol
- `POST /api/market-data` - Create new market data stored in Delta Lake
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

- `POST /api/auth/register` - Register with password credentials and receive a JWT plus API key
- `POST /api/auth/login` - Authenticate with email/password and receive a JWT plus API key

### Data Catalog

- `GET /api/catalog/products` - Get all products
- `GET /api/catalog/products?activeOnly=true` - Get active products only
- `GET /api/catalog/products/{id}` - Get product by ID
- `GET /api/catalog/products/code/{code}` - Get product by code
- `POST /api/catalog/products` - Create a catalog product

### Payments

- `POST /api/payments/checkout` - Start an asynchronous Stripe checkout flow
- `GET /api/payments/{id}` - Get payment transaction status
- `POST /api/payments/webhook` - Receive Stripe webhook events

### API Access

- `POST /api/access/register` - Register a user with password credentials and return a new API key
- `POST /api/access/login` - Issue a fresh API key for an existing user email/password login
- `POST /api/access/usage` - Record API usage and enforce purchased limits
- `GET /api/access/usage/summary?apiKey=...&productId=...` - Query remaining quota for a key and product

### Example Commerce Flow

1. Register or sign in with `POST /api/auth/register` or `POST /api/auth/login` to get a bearer token and API key.
2. Use the bearer token to create or query catalog products with `POST /api/catalog/products` or `GET /api/catalog/products`.
3. Start checkout with `POST /api/payments/checkout`.
4. Poll `GET /api/payments/{id}` until Stripe session creation completes.
5. Let Stripe call `POST /api/payments/webhook` to mark the transaction successful and grant entitlements.
6. Submit usage events through `POST /api/access/usage` and inspect remaining quota with `GET /api/access/usage/summary`.

## API Documentation

The API is documented using OpenAPI 3.0. Access the Swagger UI at `http://localhost:8080/swagger-ui.html` when the service is running.

## Database Configuration

### Transactional State
- URL: `jdbc:h2:mem:marketdata` for local execution
- URL: `jdbc:h2:file:/data/h2/market-data-lake` in Docker Compose
- Console: `http://localhost:8080/h2-console`

### Market Data Lake Storage
- Path: `${MARKETDATA_DELTA_PATH}`
- Local default: `${java.io.tmpdir}/mdl-delta/market_data`
- Docker Compose path: `/data/delta/market_data`
- Partition columns: `marketDate`, `dataType`

## Domain Model

- `User` stores the customer identity and contact fields used by the commerce flow.
- `User` also stores password hashes and roles for credential-based authentication.
- `DataProduct` represents a sellable data offering, including code, price, currency, access type, billing interval, and API usage quotas.
- `PaymentTransaction` tracks asynchronous checkout creation and final payment status.
- `UserEntitlement` records which products a user can access, whether acquired as a recurring subscription or one-time purchase, and how much quota has already been consumed.
- `ApiKey` stores issued credentials per user without persisting the raw token, only a hash and prefix.
- `ApiKeyUsageRecord` stores auditable usage events for later billing, prediction, and audit reporting.
- `MarketData` is stored as Delta Lake records with `marketDate` and `dataType` partitions to support efficient time-sliced market data access.

## Architecture

```mermaid
graph TB
    Client[Client Apps] --> Api[Spring Boot REST API]
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
    MarketSvc --> DeltaStore[Delta Lake Market Data Store]
    DeltaStore --> Delta[(Delta Lake Market Data)]
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
        Delta --> LocalFs[Local Delta Files]
    end

    subgraph Containerized
        Db --> H2File[H2 File Store]
        Delta --> DeltaVolume[Shared Delta Volume]
        DeltaVolume --> DeltaContainer[deltaio/delta-docker:4.0.0]
        Api --> Docker[Docker Compose Stack]
    end
```

More architecture documentation:
- [Architecture Overview](./ARCHITECTURE.md)
- [Architecture Decision Records](./docs/adr)
- [Architecture Diagrams](./docs/diagrams/architecture-overview.md)

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/example/springbootjavarefresh/
│   │       ├── controller/     # REST controllers
│   │       ├── entity/         # Domain models and JPA entities
│   │       ├── repository/     # Relational repositories
│   │       ├── service/        # Business logic and Delta Lake storage adapters
│   │       └── SpringBootJavaRefreshApplication.java
│   └── resources/
│       ├── application.properties
│       └── data.sql
└── test/                      # Unit tests
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
./scripts/shutdown.sh
```

Verified workflow:
- `./scripts/build.sh` builds the Java 21 Docker image
- `./scripts/run.sh` starts the Delta Lake container and the app with Docker Compose
- `./scripts/test.sh` runs the Maven test suite in a Java 21 container
- `./scripts/logs.sh app` tails container logs
- `./scripts/shutdown.sh` stops and removes the stack

The Docker image builds with Maven in a build stage and runs on Java 21 JRE in the final stage.

## Contributing

Please read CONTRIBUTING.md for details on our code of conduct and the process for submitting pull requests.

## License

Market Data Lake is licensed under the MIT License. See the LICENSE file for details.
