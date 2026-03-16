# ADR-0007: Use a Next.js Web UI in a Separate Container
Status: Accepted  
Date: 2026-03-16

## Context
The application needs a reactive user-facing interface for authentication, catalog browsing, checkout orchestration, entitlement inspection, and administration without embedding server-rendered UI concerns into the Spring Boot backend.

## Options Considered
1. A separate Next.js web UI container that consumes the Java API
2. Thymeleaf or server-rendered templates inside the Spring Boot service
3. A second backend-for-frontend service such as NestJS or Express

## Decision
A separate Next.js web UI is used as the main web experience. It runs in its own Docker container and communicates directly with the Spring Boot backend over REST APIs.

## Consequences
Positive:
- Clear separation between UI delivery and backend business logic
- Independent frontend container lifecycle and build pipeline
- A richer client-side experience for catalog, checkout, and admin workflows

Negative:
- Additional dependency management for Node and frontend build tooling
- Cross-origin configuration and API contract stability become more important
- End-to-end verification now spans multiple runtimes and containers

## Related Documents
- [Architecture Overview](../../ARCHITECTURE.md)
- [ADR-0007 Diagram](../diagrams/adr-0007-nextjs-web-ui.md)
