# ADR-0002: Use Delta Lake for Market Data and Spring Data JPA with H2 for Transactional Data
Status: Accepted  
Date: 2026-03-16

## Context
The service manages two materially different persistence workloads. Market data is append-heavy, time-oriented, and should be organized like a data lake. Users, products, payments, entitlements, API keys, and usage records are transactional and fit a relational model. The application also needs simple local and Docker execution.

## Options Considered
1. Delta Lake for market data plus Spring Data JPA with H2 for transactional data
2. Spring Data JPA with a relational database for every domain
3. Delta Lake only for every domain

## Decision
Market data is stored in Delta Lake, partitioned by `marketDate` and `dataType`. Transactional domains continue to use Spring Data JPA with H2 for local, test, and containerized runtime execution.

## Consequences
Positive:
- A lake-oriented storage layout for market data
- Partition pruning by date and data type for common market data access patterns
- Fast local startup and test execution for transactional flows with H2
- Minimal repository boilerplate for transactional domains through Spring Data JPA

Negative:
- The application now operates with two persistence models instead of one
- Delta Lake access adds Spark and Delta runtime dependencies
- Operational discipline is needed to keep lake schemas and transactional schemas aligned where domains interact

## Related Documents
- [Architecture Overview](../../ARCHITECTURE.md)
- [ADR-0002 Diagram](../diagrams/adr-0002-persistence-architecture.md)
