# ADR-0011: Use Actuator, Prometheus, and Grafana for Local Observability
Status: Accepted  
Date: 2026-03-20

## Context
Market Data Lake now includes multiple runtime components and business flows that need health visibility and lightweight local monitoring. The platform must expose service health for the Java API, UI, Airflow, Stripe integration readiness, catalog inventory, billing transactions, usage tracking, and subscriptions. JVM metrics such as heap, CPU, disk, and HTTP latency are also required for dashboards and future Grafana-based monitoring.

## Options Considered
1. Spring Boot Actuator with Micrometer, Prometheus, and Grafana
2. OpenTelemetry with an OTLP collector and Grafana-compatible backend
3. Custom health endpoints and log-only monitoring without a metrics system

## Decision
Spring Boot Actuator and Micrometer will be used in the Java API to expose standard health and metrics endpoints. Prometheus will scrape the metrics endpoint, and Grafana will provide local dashboards. A custom actuator endpoint will expose richer operational and business summaries that are useful for audit and BI scenarios but should not become high-cardinality Prometheus metrics.

## Consequences
Positive:
- Reuses standard Spring Boot observability capabilities with minimal custom code
- Provides immediate JVM, HTTP, disk, and application health metrics
- Keeps the local stack lightweight compared with a full tracing pipeline
- Gives Grafana a stable datasource for both operational and business dashboards
- Avoids putting user-level detail into Prometheus labels by keeping richer summaries in a custom actuator endpoint

Negative:
- Prometheus and Grafana add extra local containers and storage
- External service reachability is polled from the API process, so health checks are still HTTP-based rather than event-driven
- Distributed tracing is deferred and may still be needed later for deeper cross-service diagnostics
