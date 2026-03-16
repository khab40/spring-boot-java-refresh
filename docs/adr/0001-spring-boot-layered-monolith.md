# ADR-0001: Use a Spring Boot Layered Monolith
Status: Accepted  
Date: 2026-03-16

## Context
The application delivers market data APIs, subscriptions, catalog management, payments, authentication, and API key access control from a single codebase.

## Options Considered
1. Spring Boot layered monolith
2. Multiple microservices split by domain
3. Serverless functions per endpoint group

## Decision
A Spring Boot layered monolith with controller, service, repository, and entity packages will be used.

## Consequences
Positive:
- Fast feature delivery in a single deployable unit
- Simple local development and testing workflow
- Shared domain model across catalog, payments, entitlements, and auth

Negative:
- Tighter coupling across domains over time
- Scaling is coarse-grained compared with service decomposition

## Related Documents
- [Architecture Overview](../../ARCHITECTURE.md)
- [ADR-0001 Diagram](../diagrams/adr-0001-layered-monolith.md)
