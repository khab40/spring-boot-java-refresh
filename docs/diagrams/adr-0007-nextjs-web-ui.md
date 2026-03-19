# ADR-0007 Diagram

Related documents:
- [Architecture Overview](../../ARCHITECTURE.md)
- [ADR-0007](../adr/0007-use-nextjs-web-ui-in-a-separate-container.md)

```mermaid
graph LR
    Browser[Browser] --> Next[Next.js Web UI]
    Next --> Auth[Auth and Session Views]
    Next --> Catalog[Catalog and Checkout Views]
    Next --> Admin[Admin and Audit Views]
    Next --> Api[Spring Boot REST API]
    Api --> Stub[(Stub Market Data Runtime)]
    Api --> H2[(H2 Transaction Store)]
```
