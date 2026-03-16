# ADR-0002: Use Spring Data JPA with H2 for Local Development and PostgreSQL for Containerized Runtime
Status: Accepted  
Date: 2026-03-16

## Context
The service needs relational storage for users, products, payments, entitlements, API keys, and usage records, while remaining easy to run locally and in Docker.

## Options Considered
1. Spring Data JPA with H2 locally and PostgreSQL in containerized environments
2. PostgreSQL only for every environment
3. NoSQL document database

## Decision
Spring Data JPA is used for persistence, with H2 for local and test execution and PostgreSQL for containerized runtime deployments.

## Consequences
Positive:
- Fast local startup and test execution with H2
- Production-oriented relational model with PostgreSQL
- Minimal repository boilerplate through Spring Data JPA

Negative:
- H2 and PostgreSQL behavior can differ in edge cases
- Schema evolution will need stronger migration discipline later

## Related Documents
- [Architecture Overview](../../ARCHITECTURE.md)
- [ADR-0002 Diagram](../diagrams/adr-0002-persistence-architecture.md)
