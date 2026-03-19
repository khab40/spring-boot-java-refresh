# ADR-0002: Use Delta Lake for Market Data and Spring Data JPA with H2 for Transactional Data
Status: Superseded  
Date: 2026-03-16

## Context
The service manages two materially different persistence workloads. Market data is append-heavy, time-oriented, and should be organized like a data lake. Users, products, payments, entitlements, API keys, and usage records are transactional and fit a relational model. The application also needs simple local and Docker execution.

## Options Considered
1. Delta Lake for market data plus Spring Data JPA with H2 for transactional data
2. Spring Data JPA with a relational database for every domain
3. Delta Lake only for every domain

## Decision
The original direction was to store market data in Delta Lake, partitioned by `marketDate` and `dataType`, while transactional domains used Spring Data JPA with H2. That direction is currently isolated from active runtime, development, and deployment in favor of a stubbed market-data runtime while the team concentrates on UI, catalog, checkout, and entitlement flows.

## Consequences
Positive:
- A clear long-term direction for lake-style market data storage
- A useful future partitioning model by date and data type
- Fast local startup and test execution for transactional flows with H2
- Minimal repository boilerplate for transactional domains through Spring Data JPA

Negative:
- The original plan introduced a second persistence model into the application
- Delta Lake access added Spark and Delta runtime dependencies
- The team later isolated this work to speed up UI and Stripe delivery

## Related Documents
- [Architecture Overview](../../ARCHITECTURE.md)
- [ADR-0002 Diagram](../diagrams/adr-0002-persistence-architecture.md)
