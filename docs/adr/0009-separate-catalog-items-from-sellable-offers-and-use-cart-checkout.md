# ADR-0009: Separate Catalog Items from Sellable Offers and Use Cart Checkout
Status: Accepted  
Date: 2026-03-19

## Context
Market data stored or queryable from a lake is not the same thing as a sellable purchase unit. The application needs to show catalog metadata first, then let users choose one or more commercial offers for checkout without conflating lake content, pricing, and runtime preview rows.

## Options Considered
1. Separate catalog item metadata from sellable offers and use cart-based Stripe checkout
2. Keep one `DataProduct` model for both lake metadata and pricing
3. Push catalog, pricing, and cart orchestration entirely into the frontend

## Decision
The backend now models `DataCatalogItem` as the lake-facing metadata entity and keeps `DataProduct` as the sellable offer linked to a catalog item. Checkout is driven by cart line items, while runtime `MarketData` remains a separate preview or delivery concern.

## Consequences
Positive:
- The catalog clearly shows what data exists independently from how it is sold
- Stripe checkout can represent multiple selected offers in one transaction
- Runtime market-data preview can evolve independently from the commercial catalog

Negative:
- More domain objects and DTO mappings must be maintained
- Cart checkout must enforce compatible pricing modes in a single Stripe session

## Related Documents
- [Architecture Overview](../../ARCHITECTURE.md)
- [ADR-0009 Diagram](../diagrams/adr-0009-catalog-items-offers-cart-checkout.md)
