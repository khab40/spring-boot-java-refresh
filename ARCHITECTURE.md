# Architecture Overview

## Purpose
Market Data Lake is a Spring Boot backend for describing, selling, and serving market data.

Its current goals are:
- Manage market data and legacy subscriptions
- Maintain a catalog of lake datasets and linked sellable offers
- Handle users, email verification, password and Google authentication, and API key issuance
- Process Stripe-based checkout flows
- Enforce entitlement-based usage limits for data access
- Run one-time delivery SQL queries and export entitlement-scoped results as signed Parquet downloads
- Persist usage records for future billing, forecasting, and audit reporting

## System Context
The main actors and integrations are:
- End users who register, verify email ownership, authenticate, browse catalog items, purchase offers, and consume data through API keys
- End users who authenticate either with local credentials or Google OAuth2
- Internal operators or administrators who manage catalog items, sellable offers, and inspect users and entitlements
- Stripe, which handles checkout sessions and sends payment webhooks
- Google Identity, which handles OAuth2 login and returns verified user profile claims
- A stub market-data runtime, which currently serves preview-only market data
- MinIO, which provides S3-compatible local object storage for generated OTD delivery files
- H2, which stores transactional application state

At present, the deployed implementation uses a separate Next.js UI container, a single Spring Boot backend, and a local S3-compatible file storage service. There is no separate API gateway, event streaming platform, production data lake runtime, or ML platform in the deployed implementation.

Architecture decisions are recorded in:
- [ADR-0001](./docs/adr/0001-spring-boot-layered-monolith.md)
- [ADR-0002](./docs/adr/0002-use-delta-lake-for-market-data-and-jpa-for-transactional-data.md)
- [ADR-0003](./docs/adr/0003-use-java-21-and-dockerized-build-test-runtime.md)
- [ADR-0004](./docs/adr/0004-use-asynchronous-stripe-checkout-and-webhooks.md)
- [ADR-0005](./docs/adr/0005-use-api-keys-with-entitlement-based-usage-limits.md)
- [ADR-0006](./docs/adr/0006-use-jwt-based-stateless-authentication.md)
- [ADR-0007](./docs/adr/0007-use-nextjs-web-ui-in-a-separate-container.md)
- [ADR-0008](./docs/adr/0008-use-google-oauth2-login-with-jwt-session-bridging.md)
- [ADR-0009](./docs/adr/0009-separate-catalog-items-from-sellable-offers-and-use-cart-checkout.md)
- [ADR-0010](./docs/adr/0010-use-standalone-airflow-for-future-workflow-orchestration.md)
- [ADR-0011](./docs/adr/0011-use-actuator-prometheus-and-grafana-for-local-observability.md)
- [ADR-0012](./docs/adr/0012-use-s3-compatible-object-storage-and-synchronous-otd-deliveries.md)

The main system diagram is available in:
- [Architecture Overview Diagram](./docs/diagrams/architecture-overview.md)

## Key Components
- Authentication layer
JWT-based authentication with Spring Security, password hashing, email verification, optional Google OAuth2 login, and stateless bearer-token validation.
- Web experience layer
A separate Next.js frontend provides signup, signin, catalog, checkout, entitlement, and administration workflows.
- Workflow orchestration layer
A separate Apache Airflow standalone container is reserved for future ingestion adapters, metadata refresh jobs, and data-lake-facing orchestration.
- Observability layer
Spring Boot Actuator health groups, Micrometer metrics, Prometheus scraping, Grafana dashboards, and a custom monitoring summary endpoint provide runtime visibility across API, UI, Airflow, Stripe, catalog, usage, and subscription flows.
- API access layer
API key issuance, lookup, quota enforcement, and usage recording for downstream data access.
- OTD delivery layer
Restricted SQL query execution over preview market data, Parquet export, signed object delivery, delivery audit history, and entitlement-aware download-volume deduction.
- Backend services
Controller, service, repository, and entity layers organized in a layered Spring Boot monolith.
- Commerce and payments
Stripe checkout session creation, payment transaction tracking, and webhook-driven entitlement activation.
- Catalog and entitlement domain
`DataCatalogItem` defines what exists in the lake, `DataProduct` defines how that item is sold, and `UserEntitlement` represents purchased access and consumed quota.
- Delivery storage layer
Generated one-time delivery artifacts are stored in an S3-compatible file store and surfaced through expiring signed URLs rather than streamed directly from the API process.
- Persistence layer
Transactional domains stay in H2-backed Spring Data JPA repositories, while market data is currently served from an in-memory stub runtime so product, entitlement, and delivery flows can evolve without a live lake dependency.

## Data Flow
The primary data flows are:

1. A user registers through `/api/auth/register`.
2. The application stores the user, generates a one-time verification token, and sends a verification email.
3. The user verifies the token through `/api/auth/verify-email`.
4. The application validates credentials for verified users, issues a JWT, and issues an API key.
5. As an alternative, the user can start `GET /oauth2/authorization/google`, complete Google OAuth2 login, and receive the same application JWT and API key through the frontend callback flow.
6. Catalog items are created or queried through `/api/catalog/items`, and linked sellable offers are managed through `/api/catalog/products`.
7. A purchase starts through `/api/payments/checkout`, which creates a cart-backed payment transaction with transaction items and asynchronously creates a Stripe Checkout session.
8. Stripe sends webhook events to `/api/payments/webhook`.
9. Successful payment events activate or update `UserEntitlement` records.
10. Clients submit usage through `/api/access/usage`.
11. The application resolves the API key, checks the user entitlement for the target product, enforces quota limits, updates usage counters, and writes an `ApiKeyUsageRecord`.
12. Market data create, read, and delete flows use a stub store that returns preview data and accepts admin-created preview rows.
13. The web UI runs in a separate container, calls the Java API over HTTP, and drives the end-user and admin flows without duplicating backend business rules.
14. Users with purchased one-time products can submit a restricted SQL query through `/api/market-data/otd-deliveries`.
15. The application validates the user entitlement, executes the SQL against preview `market_data`, exports the result into one or more Parquet parts, uploads them to MinIO, persists a `DataDelivery` record, deducts consumed batch-download volume, and sends delivery links by email.
16. The UI later reads `/api/market-data/otd-deliveries/mine` to display signed download links and delivery history.
17. Airflow remains operationally separate and is reserved for future DAG-driven ingestion and lake workflows rather than the current request-response path.
18. Prometheus scrapes `/actuator/prometheus`, and Grafana visualizes both standard JVM metrics and MDL-specific business metrics.

## Deployment Architecture
The current deployment model is simple:
- A separate Next.js web UI container
- A single Spring Boot application process
- A MinIO container providing local S3-compatible file storage
- A Mailpit container for local SMTP capture and verification testing
- A standalone Apache Airflow container for future orchestration development
- A Prometheus container for metrics collection
- A Grafana container with provisioned dashboards and datasource
- Optional outbound OAuth2 integration with Google identity endpoints
- H2 for transactional local, test, and containerized execution
- MinIO-backed object storage for generated Parquet delivery files
- Docker-based scripts for build, test, run, logs, and shutdown
- Native host-run scripts for faster backend and frontend iteration without rebuilding containers
- A Render blueprint for lightweight preview deployment of the backend and web UI

There is currently no Kubernetes cluster, service mesh, dedicated ingress tier, or multi-service networking topology in this repository.

## Technology Stack
- Frontend: React, Next.js, TypeScript
- Backend: Java 21, Spring Boot, Spring MVC, Spring Data JPA, Spring Security
- Authentication: BCrypt password hashing, email verification, Spring Security OAuth2 client for Google, JWT bearer tokens
- Payments: Stripe Checkout and webhook processing
- Data: H2 plus in-memory market-data stubs, Parquet exports, MinIO S3-compatible object storage
- Monitoring: Spring Boot Actuator, Micrometer, Prometheus, Grafana
- Build and Test: Maven, Docker, Docker Compose, Apache Airflow standalone
- API Documentation: springdoc OpenAPI / Swagger UI

## Key Architectural Principles
- Layered domain application
The system is organized into controllers, services, repositories, and entities with clear responsibility boundaries.
- Stateless authentication
Bearer-token validation is stateless, and API keys are managed separately from user login tokens.
- Catalog-first commerce separation
Lake metadata, sellable offers, and runtime market-data content are modeled separately so the data shop is not coupled to one physical storage implementation.
- Product-driven access control
Purchased offers define both pricing and quota limits, and entitlements are the source of truth for access.
- Signed artifact delivery
One-time downloads are materialized into explicit files, stored outside the API process, and accessed through expiring signed URLs.
- Auditable usage tracking
Usage is persisted as explicit records rather than inferred only from aggregate counters.
- Pragmatic evolution
The design favors a single deployable service today while leaving room for future billing, forecasting, and reporting expansion.
- Orchestration without premature distribution
Workflow orchestration is introduced as a separate component now, but kept in a lightweight standalone mode until ingestion and lake pipelines justify a heavier Airflow deployment.
- Pragmatic observability
Use standard Spring and Prometheus-compatible instrumentation first, then add richer tracing later only when the operational need justifies it.

## Related ADRs
See [`/docs/adr`](./docs/adr) for architecture decisions.

Related diagrams:
- [Architecture Overview Diagram](./docs/diagrams/architecture-overview.md)
- [ADR-0001 Diagram](./docs/diagrams/adr-0001-layered-monolith.md)
- [ADR-0002 Diagram](./docs/diagrams/adr-0002-persistence-architecture.md)
- [ADR-0003 Diagram](./docs/diagrams/adr-0003-java-docker-workflow.md)
- [ADR-0004 Diagram](./docs/diagrams/adr-0004-stripe-payment-flow.md)
- [ADR-0005 Diagram](./docs/diagrams/adr-0005-api-key-usage-limits.md)
- [ADR-0006 Diagram](./docs/diagrams/adr-0006-jwt-auth-flow.md)
- [ADR-0007 Diagram](./docs/diagrams/adr-0007-nextjs-web-ui.md)
- [ADR-0008 Diagram](./docs/diagrams/adr-0008-google-oauth2-jwt-bridge.md)
- [ADR-0009 Diagram](./docs/diagrams/adr-0009-catalog-items-offers-cart-checkout.md)
- [ADR-0010 Diagram](./docs/diagrams/adr-0010-airflow-orchestration.md)
- [ADR-0011 Diagram](./docs/diagrams/adr-0011-observability-monitoring.md)
- [ADR-0012 Diagram](./docs/diagrams/adr-0012-otd-fss-delivery-flow.md)
