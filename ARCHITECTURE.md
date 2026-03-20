# Architecture Overview

## Purpose
Market Data Lake is a Spring Boot backend for describing, selling, and serving market data.

Its current goals are:
- Manage market data and legacy subscriptions
- Maintain a catalog of lake datasets and linked sellable offers
- Handle users, email verification, password and Google authentication, and API key issuance
- Process Stripe-based checkout flows
- Enforce entitlement-based usage limits for data access
- Persist usage records for future billing, forecasting, and audit reporting

## System Context
The main actors and integrations are:
- End users who register, verify email ownership, authenticate, browse catalog items, purchase offers, and consume data through API keys
- End users who authenticate either with local credentials or Google OAuth2
- Internal operators or administrators who manage catalog items, sellable offers, and inspect users and entitlements
- Stripe, which handles checkout sessions and sends payment webhooks
- Google Identity, which handles OAuth2 login and returns verified user profile claims
- A stub market-data runtime, which currently serves preview-only market data
- H2, which stores transactional application state

At present, the application is a single backend service. There is no separate API gateway, event streaming platform, data lake, or ML platform in the deployed implementation.

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

The main system diagram is available in:
- [Architecture Overview Diagram](./docs/diagrams/architecture-overview.md)

## Key Components
- Authentication layer
JWT-based authentication with Spring Security, password hashing, email verification, optional Google OAuth2 login, and stateless bearer-token validation.
- Web experience layer
A separate Next.js frontend provides signup, signin, catalog, checkout, entitlement, and administration workflows.
- API access layer
API key issuance, lookup, quota enforcement, and usage recording for downstream data access.
- Backend services
Controller, service, repository, and entity layers organized in a layered Spring Boot monolith.
- Commerce and payments
Stripe checkout session creation, payment transaction tracking, and webhook-driven entitlement activation.
- Catalog and entitlement domain
`DataCatalogItem` defines what exists in the lake, `DataProduct` defines how that item is sold, and `UserEntitlement` represents purchased access and consumed quota.
- Persistence layer
Transactional domains stay in H2-backed Spring Data JPA repositories, while market data is currently served from an in-memory stub runtime so product and Stripe flows can evolve without a live lake dependency.

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

## Deployment Architecture
The current deployment model is simple:
- A separate Next.js web UI container
- A single Spring Boot application process
- A Mailpit container for local SMTP capture and verification testing
- Optional outbound OAuth2 integration with Google identity endpoints
- H2 for transactional local, test, and containerized execution
- Docker-based scripts for build, test, run, logs, and shutdown

There is currently no Kubernetes cluster, service mesh, dedicated ingress tier, or multi-service networking topology in this repository.

## Technology Stack
- Frontend: React, Next.js, TypeScript
- Backend: Java 21, Spring Boot, Spring MVC, Spring Data JPA, Spring Security
- Authentication: BCrypt password hashing, email verification, Spring Security OAuth2 client for Google, JWT bearer tokens
- Payments: Stripe Checkout and webhook processing
- Data: H2 plus in-memory market-data stubs
- Build and Test: Maven, Docker, Docker Compose
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
- Auditable usage tracking
Usage is persisted as explicit records rather than inferred only from aggregate counters.
- Pragmatic evolution
The design favors a single deployable service today while leaving room for future billing, forecasting, and reporting expansion.

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
