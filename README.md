# PIE — Portfolio Intelligence Engine

AI-powered advisor intelligence for wealth management: evidence-backed answers over clients, portfolios, and documents — not autonomous financial advice.

**Start here:** [Product Requirements Document (v1.0)](docs/PRD.md)

**Planning notes:** [Finalize PIE PRD plan](docs/plans/finalize-pie-prd.md) · [MVP implementation tracker](docs/plans/mvp-implementation.md)

## Quick start

```bash
# Infrastructure (Postgres/pgvector, Neo4j, Redis)
docker compose up -d

# API
cd backend && mvn spring-boot:run

# UI
cd frontend && npm install && npm run dev
```

- UI: http://localhost:3000  
- API: http://localhost:8080  
- Swagger: http://localhost:8080/swagger-ui.html  

### Demo users

| Email | Password | Notes |
| --- | --- | --- |
| advisor@pie.demo | password | Access to John Smith |
| csa@pie.demo | password | Read access to John Smith |
| outsider@pie.demo | password | Same tenant, **no** John Smith access |

Synthetic holdings CSVs live in [`demo/`](demo/).

