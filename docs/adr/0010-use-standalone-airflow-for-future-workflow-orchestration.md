# ADR-0010: Use Standalone Airflow for Future Workflow Orchestration
Status: Accepted  
Date: 2026-03-20

## Context
Market Data Lake needs a future orchestration component for ingestion adapters, lake synchronization jobs, metadata refreshes, and downstream data workflows. The current repository does not yet need a distributed scheduler or worker fleet, and the development stack should remain lightweight enough for local laptop usage.

## Options Considered
1. Apache Airflow standalone in a single container
2. Full Apache Airflow Compose stack with separate scheduler, webserver, database, and broker
3. No orchestrator yet

## Decision
Apache Airflow will be added as a separate standalone container using the official slim image, mounted DAG and plugin folders, and default local metadata storage. It will be treated as a future orchestration component for ingestion adapters and lake-facing workflows rather than an immediate runtime dependency of the Java API.

## Consequences
Positive:
- Provides a real workflow orchestrator now without forcing an immediate microservice split
- Keeps local resource usage lower than a full Airflow multi-container deployment
- Creates a clean place for future Delta Lake, Iceberg, Snowflake, and ingestion DAG work

Negative:
- Standalone mode is not a production-grade clustered Airflow deployment
- Local metadata and executor behavior are intentionally limited compared with a full Airflow stack
