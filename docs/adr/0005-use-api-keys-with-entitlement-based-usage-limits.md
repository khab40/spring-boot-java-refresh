# ADR-0005: Use API Keys with Entitlement-Based Usage Limits and Audit Records
Status: Accepted  
Date: 2026-03-16

## Context
The platform must issue API keys to users, enforce purchased limits for data usage, and preserve usage history for later billing, reporting, forecasting, and audit features.

## Options Considered
1. Dedicated API key entities plus entitlement-based quota tracking and usage records
2. JWT-only access with no separate API key model
3. Flat per-user quotas stored directly on the user record

## Decision
Separate API key and API usage record tables are used, while purchased limits are enforced against the user entitlement tied to a specific data product.

## Consequences
Positive:
- API credentials are independent from bearer login tokens
- Usage is auditable at event level for future billing and reporting
- Quotas stay aligned with products the user actually purchased

Negative:
- Additional persistence and service complexity
- Limit logic spans products, entitlements, keys, and usage records

## Related Documents
- [Architecture Overview](../../ARCHITECTURE.md)
- [ADR-0005 Diagram](../diagrams/adr-0005-api-key-usage-limits.md)
