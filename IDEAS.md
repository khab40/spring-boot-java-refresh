What we plan to do here for MARKET DATA LAKE (MDL) project: 

1. Data Ingestion layer: different types or classes of the data: tick data, news data, fundamentals, crypto, others.
2. Market Data Model abstraction layer: like dxFeed QD Model of Market events or LOBSTER data model, or LSE data model. Data types of market data model can be L1, L2 or L3. 
3. Add data cleansing and data enrichment via medallion data pattern: bronze (raw), silver, gold layers.
4. Add DeltaLake/Iceberg as a underlying data lake storage instead of Postgre DB. (sample data only for the moment and locally in Docker with externally mounted volumes)
5. Add AirFlow for all workflows orchestration
6. Add rich Web reactive UI using React, next.js, nest.js, Express or FAST API and link it to the backend Java API. 
7. Create all needed forms to signup, signin, list data types and data available, subscribe to the data for stream, or download once or regularly, pay for it, check-put, etc. Administration UIs to control the system and make its audit. 
8. Add Grafana (in Docker) to use as a BI tool to monitor health, endpoints, billing, other reports of the system.
9. Bearer and JTW tokens
10. Add message bus internally like NATS Streaming, Rabbit MQ or another to become micro-service like system.
11. Make meaningfully each key service in a microservice mode. Add internal messages Publisher/Subscriber model to allow microservices to talk to each other.
12. Add Kafka or another similar tool (in Docker) to handle real-time events ingestion, and as another use-case, allow to publish events out of the system as delivery stream to user's end-points with control of their subscription and API key usage.
13. Add SQL query interface like Athena Trino to MDL app.
14. Add MCP wrapper to MDL. with API key and usage tracking.
15. Make MDL Agents friendly. 
16. Add ML/AI capabilities to the system for data analysis, predictions, and insights generation.
17. Implement a robust security framework to protect data and ensure compliance with regulations. SOC2
18. Move common UI logics to core-frontend library module. 
19. Move common backend logics to core-backend module. 