# Architecture Overview Diagram

Related documents:
- [Architecture Overview](../../ARCHITECTURE.md)
- [Architecture Decision Records](../adr)

```mermaid
graph TB
    Client[Browser Clients] --> Next[Next.js Web UI]
    Admin[Operators and Admins] --> Api
    Stripe[Stripe Platform] --> Api
    Google[Google Identity] --> Api
    Next --> Api[Spring Boot REST API]
    Api --> Controllers[Controllers]
    Controllers --> Services[Services]
    Services --> Security[JWT Auth OAuth2 and API Key Access]
    Services --> Commerce[Catalog Payments Entitlements]
    Services --> Market[Market Data Service]
    Services --> OTD[OTD Delivery Service]
    Services --> Legacy[Legacy Subscriptions]
    OTD --> SQL[Restricted SQL Engine]
    OTD --> Export[Parquet Export]
    OTD --> ObjectStore[Object Storage Service]
    OTD --> Mail[Delivery Email Service]
    Security --> Repositories[Spring Data JPA Repositories]
    Commerce --> Repositories
    Legacy --> Repositories
    OTD --> Repositories
    Market --> StubStore[Stub Market Data Store]
    SQL --> StubStore
    StubStore --> Preview[(Preview Market Data)]
    Repositories --> H2[(H2 Transaction Store)]
    ObjectStore --> MinIO[(MinIO FSS)]
    Scripts[Docker Helper Scripts] --> Api
    Scripts --> Next
    Scripts --> MinIO
```
