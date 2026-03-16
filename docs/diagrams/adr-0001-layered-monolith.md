# ADR-0001 Diagram

Related documents:
- [Architecture Overview](../../ARCHITECTURE.md)
- [ADR-0001](../adr/0001-spring-boot-layered-monolith.md)

```mermaid
graph TB
    Client[Clients] --> Controller[Controller Layer]
    Controller --> Service[Service Layer]
    Service --> Repository[Repository Layer]
    Repository --> Entity[Entity Model]
    Repository --> Database[(Relational Database)]
```
