# Architecture Overview

## Purpose
Market Data Lake is a Spring Boot backend for selling and serving market data products.

Its current goals are:
- Manage market data and legacy subscriptions
- Maintain a purchasable data catalog
- Handle users, email verification, authentication, and API key issuance
- Process Stripe-based checkout flows
- Enforce entitlement-based usage limits for data access
- Persist usage records for future billing, forecasting, and audit reporting

## System Context
The main actors and integrations are:
- End users who register, verify email ownership, authenticate, purchase data products, and consume data through API keys
- Internal operators or administrators who manage catalog products and inspect users and entitlements
- Stripe, which handles checkout sessions and sends payment webhooks
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

The main system diagram is available in:
- [Architecture Overview Diagram](./docs/diagrams/architecture-overview.md)

## Key Components
- Authentication layer
JWT-based authentication with Spring Security, password hashing, email verification, and stateless bearer-token validation.
- Web experience layer
A separate Next.js frontend provides signup, signin, catalog, checkout, entitlement, and administration workflows.
- API access layer
API key issuance, lookup, quota enforcement, and usage recording for downstream data access.
- Backend services
Controller, service, repository, and entity layers organized in a layered Spring Boot monolith.
- Commerce and payments
Stripe checkout session creation, payment transaction tracking, and webhook-driven entitlement activation.
- Catalog and entitlement domain
Data products define pricing and quota limits; user entitlements represent purchased access and consumed quota.
- Persistence layer
Transactional domains stay in H2-backed Spring Data JPA repositories, while market data is currently served from an in-memory stub runtime so product and Stripe flows can evolve without a live lake dependency.

## Data Flow
The primary data flows are:

1. A user registers through `/api/auth/register`.
2. The application stores the user, generates a one-time verification token, and sends a verification email.
3. The user verifies the token through `/api/auth/verify-email`.
4. The application validates credentials for verified users, issues a JWT, and issues an API key.
5. Catalog products are created or queried through `/api/catalog/products`.
6. A purchase starts through `/api/payments/checkout`, which creates a payment transaction and asynchronously creates a Stripe Checkout session.
7. Stripe sends webhook events to `/api/payments/webhook`.
8. Successful payment events activate or update `UserEntitlement` records.
9. Clients submit usage through `/api/access/usage`.
10. The application resolves the API key, checks the user entitlement for the target product, enforces quota limits, updates usage counters, and writes an `ApiKeyUsageRecord`.
11. Market data create, read, and delete flows use a stub store that returns preview data and accepts admin-created preview rows.
12. The web UI runs in a separate container, calls the Java API over HTTP, and drives the end-user and admin flows without duplicating backend business rules.

## Deployment Architecture
The current deployment model is simple:
- A separate Next.js web UI container
- A single Spring Boot application process
- A Mailpit container for local SMTP capture and verification testing
- H2 for transactional local, test, and containerized execution
- Docker-based scripts for build, test, run, logs, and shutdown

There is currently no Kubernetes cluster, service mesh, dedicated ingress tier, or multi-service networking topology in this repository.

## Technology Stack
- Frontend: React, Next.js, TypeScript
- Backend: Java 21, Spring Boot, Spring MVC, Spring Data JPA, Spring Security
- Authentication: BCrypt password hashing, email verification, JWT bearer tokens
- Payments: Stripe Checkout and webhook processing
- Data: H2 plus in-memory market-data stubs
- Build and Test: Maven, Docker, Docker Compose
- API Documentation: springdoc OpenAPI / Swagger UI

## Key Architectural Principles
- Layered domain application
The system is organized into controllers, services, repositories, and entities with clear responsibility boundaries.
- Stateless authentication
Bearer-token validation is stateless, and API keys are managed separately from user login tokens.
- Product-driven access control
Purchased products define both pricing and quota limits, and entitlements are the source of truth for access.
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
