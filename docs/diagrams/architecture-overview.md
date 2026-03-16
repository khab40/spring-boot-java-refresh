# Architecture Overview Diagram

Related documents:
- [Architecture Overview](../../ARCHITECTURE.md)
- [Architecture Decision Records](../adr)

```mermaid
graph TB
    Client[Client Apps] --> Api[Spring Boot REST API]
    Admin[Operators and Admins] --> Api
    Stripe[Stripe Platform] --> Api
    Api --> Controllers[Controllers]
    Controllers --> Services[Services]
    Services --> Security[JWT Auth and API Key Access]
    Services --> Commerce[Catalog Payments Entitlements]
    Services --> Legacy[Market Data and Legacy Subscriptions]
    Security --> Repositories[Spring Data JPA Repositories]
    Commerce --> Repositories
    Legacy --> Repositories
    Repositories --> H2[(H2 Local and Test)]
    Repositories --> Pg[(PostgreSQL Docker Runtime)]
    Scripts[Docker Helper Scripts] --> Api
```
