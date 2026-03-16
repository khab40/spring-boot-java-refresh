# ADR-0004: Use Asynchronous Stripe Checkout with Webhook-Based Payment Finalization
Status: Accepted  
Date: 2026-03-16

## Context
The application sells data products and subscriptions and must handle payment session creation, delayed payment outcomes, and entitlement activation.

## Options Considered
1. Stripe Checkout with asynchronous session creation and webhook finalization
2. Synchronous direct card processing inside the application
3. Manual offline payment tracking

## Decision
Stripe Checkout is used for payment initiation, session creation is triggered asynchronously, and webhook events finalize transactions and grant entitlements.

## Consequences
Positive:
- Offloads payment UI and card-handling complexity to Stripe
- Supports async outcomes such as delayed payment confirmation
- Clean separation between payment initiation and entitlement granting

Negative:
- Requires webhook handling and external event coordination
- Local testing is more complex than purely synchronous flows

## Related Documents
- [Architecture Overview](../../ARCHITECTURE.md)
- [ADR-0004 Diagram](../diagrams/adr-0004-stripe-payment-flow.md)
