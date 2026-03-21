# ADR-0012: Use S3-Compatible Object Storage and Synchronous OTD Deliveries
Status: Accepted  
Date: 2026-03-21

## Context
The platform now sells one-time download products in addition to subscriptions. Those products need a delivery path that can run an entitlement-scoped SQL query against the market-data preview runtime, export the result as Parquet, store one or more output files durably, and return a time-limited download link in both the UI and email notifications.

The delivery mechanism must be cheap to run locally, compatible with the existing Docker-first development model, and close enough to cloud object storage semantics that the same application contract can later be moved to managed infrastructure such as Amazon S3.

## Options Considered
1. Store generated files directly on the API container filesystem and expose them through Spring MVC
2. Use S3-compatible object storage with signed URLs and keep the query-to-delivery flow synchronous inside the Spring Boot application
3. Defer delivery generation to an asynchronous workflow engine before introducing any object storage

## Decision
Use an S3-compatible object storage service for generated delivery artifacts and keep the first OTD implementation synchronous in the Spring Boot application.

For local runtime this repository uses MinIO as the File Storage Service. The API runs a restricted SQL query over the current `market_data` preview dataset, exports the result into Parquet parts, uploads them to object storage, persists a `DataDelivery` audit record, deducts consumed entitlement volume, and generates signed download links for UI and email delivery.

## Consequences
Positive:
- Adds a realistic storage contract for downloadable products without taking a hard dependency on AWS
- Keeps local development simple because MinIO runs as a single free Docker container
- Makes future migration to managed S3-style storage straightforward because application code already targets signed object operations rather than local files
- Creates an auditable delivery history with explicit SQL text, row counts, file counts, and consumed volume
- Keeps the first implementation operationally simple by avoiding background orchestration until demand or workload size requires it

Negative:
- The API process now owns query execution, export, storage upload, quota deduction, and email dispatch in one synchronous request path
- Large deliveries can hold request threads longer than a future asynchronous workflow design would
- Local runtime now includes another stateful service that needs credentials, persistence, and operational checks
- The SQL engine is intentionally restricted and not yet a general Athena or Trino replacement
