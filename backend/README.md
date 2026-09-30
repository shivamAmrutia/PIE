# PIE Backend

## Requirements

- Java 17+ (21 preferred per PRD)
- Maven 3.8+
- Docker (Postgres/pgvector, Neo4j, Redis)

## Quick start

```bash
# From repo root
docker compose up -d
cd backend && mvn spring-boot:run
```

API: http://localhost:8080  
Swagger: http://localhost:8080/swagger-ui.html

### Demo users

| Email | Password | Role |
| --- | --- | --- |
| advisor@pie.demo | password | ADVISOR |
| csa@pie.demo | password | CSA |
| outsider@pie.demo | password | ADVISOR (no John Smith access) |
