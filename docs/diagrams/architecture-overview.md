# Architecture Overview Diagram

Related documents:
- [Architecture Overview](../../ARCHITECTURE.md)
- [Architecture Decision Records](../adr)

```mermaid
graph TB
    Client[Browser Clients] --> Next[Next.js Web UI]
    Admin[Operators and Admins] --> Api
    Stripe[Stripe Platform] --> Api
    Next --> Api[Spring Boot REST API]
    Api --> Controllers[Controllers]
    Controllers --> Services[Services]
    Services --> Security[JWT Auth and API Key Access]
    Services --> Commerce[Catalog Payments Entitlements]
    Services --> Market[Market Data Service]
    Services --> Legacy[Legacy Subscriptions]
    Security --> Repositories[Spring Data JPA Repositories]
    Commerce --> Repositories
    Legacy --> Repositories
    Market --> DeltaStore[Delta Lake Market Data Store]
    DeltaStore --> Delta[(Delta Lake)]
    Repositories --> H2[(H2 Transaction Store)]
    Delta --> Partitioning[Partitions: marketDate, dataType]
    Scripts[Docker Helper Scripts] --> Api
    Scripts --> Next
    Scripts --> DeltaContainer[deltaio/delta-docker:4.0.0]
```
