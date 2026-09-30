# PIE — Product Requirements Document

| Field | Value |
| --- | --- |
| **Status** | Final (MVP) |
| **Version** | 1.0 |
| **Product** | PIE (Portfolio Intelligence Engine) |
| **Category** | Wealth Management Intelligence Platform |
| **Intent** | Hybrid — synthetic-data engineering demo with production-credible architecture |
| **Primary users** | Wealth advisors, client service associates, portfolio management teams |
| **Last updated** | 2026-09-26 |

---

## Glossary

| Term | Definition |
| --- | --- |
| **IPS** | Investment Policy Statement — documented objectives, constraints, and allocation/concentration limits for a client |
| **Concentration** | Weight of a single security (or issuer) as a percentage of an account or portfolio |
| **Drift** | Difference between current allocation and IPS target allocation, in percentage points |
| **Provenance** | Traceable link from a fact or claim to its source document, page/chunk, extraction method, and timestamp |
| **Groundedness** | Degree to which generated statements are supported by retrieved evidence or deterministic calculations |
| **Tenant** | Isolation boundary for a firm (or demo firm); users in one tenant cannot access another tenant’s data |
| **As-of date** | Point-in-time date for which portfolio state, holdings, or metrics are valid |
| **Material change** | A change meeting defined thresholds (see §7.5): allocation drift ≥ 5 pp, any IPS limit breach, new/removed security, or cash change > 10% of portfolio market value |
| **HITL** | Human-in-the-loop — advisor retains interpretation and decision authority |

---

## 1. Product Overview

PIE (Portfolio Intelligence Engine) is an AI-powered intelligence platform designed to help wealth-management professionals understand clients, portfolios, and related documentation more efficiently.

Wealth-management professionals often need to synthesize information spread across portfolio statements, investment policy statements (IPS), tax documents, CRM notes, research reports, meeting notes, and other documents before making decisions or meeting with clients.

PIE creates a unified, evidence-backed representation of this information and allows advisors to ask questions, identify changes, prepare for client meetings, and investigate portfolio issues.

**Core product principle:**

> PIE surfaces relevant information, relationships, discrepancies, and evidence. The advisor remains responsible for interpretation and decisions.

PIE is an **advisor intelligence and workflow tool**, not an autonomous financial-advice system.

**v1 positioning:** Ship a strong GitHub / demo experience on fully synthetic data, while designing security, provenance, and architecture so a controlled firm pilot could follow without a rewrite.

---

## 2. Problem Statement

Wealth-management professionals spend significant time on information-gathering and synthesis.

Before a client review, an advisor may need to answer:

* What has changed since the client's last review?
* What are the client's current financial goals?
* What does the latest IPS specify?
* Has the portfolio drifted from its target allocation?
* Which securities represent concentrated positions?
* What major transactions occurred recently?
* What did the client mention during the previous meeting?
* Why did portfolio performance differ from the benchmark?
* Which documents support a particular conclusion?
* Are there discrepancies between different sources of information?

This information is frequently distributed across heterogeneous systems and documents.

Traditional search finds documents but poorly understands relationships. Generic LLM interfaces summarize well but can produce unsupported or fabricated claims.

PIE addresses the gap by combining:

* structured financial data
* document extraction
* semantic retrieval
* keyword retrieval
* temporal reasoning
* knowledge graphs
* deterministic calculations
* LLM-based reasoning
* source-level provenance

---

## 3. Product Vision

> Create an intelligent information layer for wealth-management firms that allows advisors to understand a client's financial context as quickly as they can ask a question.

### MVP model (required)

PIE maintains an evidence-backed model for each client covering:

```text
Client
 ├── Goals
 ├── Accounts
 │    └── Holdings
 ├── Investment Policies
 ├── Financial Events
 ├── Documents
 ├── Advisor Notes
 ├── Relationships
 └── Historical States (as-of snapshots)
```

Every important fact should have:

* a source
* a timestamp / as-of date
* provenance
* confidence information where applicable

### Later (post-MVP)

Continuously updated multi-source sync (CRM, custodian), household graphs, and automated monitoring — see §15.

---

## 4. Target Users & User Stories

### 4.1 Primary user: Wealth Advisor

Responsibilities: managing relationships, reviewing portfolios, preparing meetings, interpreting objectives, communicating changes, coordinating with other professionals.

**Pain points:** time spent gathering information; fragmented records; hard-to-search history; manual document comparison; meeting prep; spotting relevant changes.

### 4.2 Secondary user: Client Service Associate (CSA)

Uses PIE to gather documents, answer advisor information requests, prepare summaries, locate historical records, and verify information via citations.

### 4.3 Secondary user: Portfolio Manager

Uses PIE to investigate portfolio changes, analyze exposures, review performance, investigate allocation drift, and compare portfolio state across time.

### 4.4 P0 user stories (MVP)

| ID | Story |
| --- | --- |
| US-01 | As an advisor, I can create a synthetic client and open a client workspace |
| US-02 | As an advisor, I can upload IPS, statements, meeting notes, and research PDFs for a client |
| US-03 | As an advisor, I can import a holdings CSV with as-of dates |
| US-04 | As an advisor, I can ask natural-language questions and receive an answer with citations and relevant dates |
| US-05 | As an advisor, I can open a citation and inspect the source page or snippet |
| US-06 | As an advisor, I can generate a client-review briefing with review flags |
| US-07 | As an advisor, I can compare two as-of dates and see material portfolio/document changes |
| US-08 | As an advisor, I can view allocation, concentration, performance, and IPS drift from **calculated** metrics |
| US-09 | As a CSA, I can locate documents and verify facts via citations |
| US-10 | As a constrained user, I cannot retrieve another client’s data via API or Ask PIE |

---

## 5. Product Goals

1. **Reduce information-gathering time** — answer questions that previously required searching multiple documents and systems.
2. **Improve discoverability** — natural-language access to relevant client context.
3. **Preserve evidence** — answers traceable to underlying sources.
4. **Understand change over time** — current state and how it changed.
5. **Support advisor workflows** — client review prep, portfolio investigation, document review, meeting prep, post-meeting follow-up.

---

## 6. Non-Goals

The product will **not**:

* autonomously provide personalized investment advice
* execute trades
* make portfolio allocation decisions
* replace a financial advisor
* provide tax or legal advice
* communicate investment recommendations directly to clients
* autonomously modify client records

PIE is an **information retrieval, analysis, and decision-support layer**.

### MVP out of scope (explicit)

* CRM or custodian integration
* OCR of scanned/image-only PDFs (text-layer PDFs only)
* Tax intelligence / tax advice
* Household / multi-entity modeling (individuals, trusts, businesses as first-class)
* Real-time market data feeds
* Production SSO / IdP (JWT or local auth for demo; design for later IdP)
* Client-facing portals or messaging
* Productized multi-tenant onboarding UI (isolation must still be designed and tested)

---

## 7. Assumptions & Constraints

### Assumptions

* All MVP data is **synthetic**; no real PII or real client data.
* Demo runs as a single firm / single tenant; tenant isolation is **implemented and tested**, not productized.
* LLM access uses a provider-agnostic abstraction; local/dev may use a configured cloud model.
* Documents are English-only for MVP.
* Primary formats: PDF (with extractable text) and CSV.
* Synthetic demo client is **John Smith** (see §11).

### Constraints

* The advisor remains the decision-maker; the UI must never present system output as “Sell / Buy / Recommend” advice.
* Financial calculations (allocation, concentration, drift, performance) are performed by deterministic software, not by the LLM inventing numbers.
* Every retrieval path evaluates the caller’s authorization context.

---

## 8. Compliance & Regulatory Posture

PIE is **decision support / information retrieval**, not investment advice.

**MVP requirements:**

* Persistent UI disclaimer on Ask PIE and client-review briefings stating that outputs are for advisor decision support only and are not investment, tax, or legal advice.
* Audit logging of authentication events, Ask PIE queries, document access, and briefing generation.
* No client-facing communication channels.
* Human-in-the-loop boundaries enforced in product copy and validation (see §14).

**Real-data pilot gate (post-MVP, not built in MVP):** firm policy approval, data processing agreement, retention/deletion rules, and production identity provider — documented here so architecture stays pilot-ready.

---

## 9. Core User Experience — Client Workspace

Each client has a workspace containing:

```text
Client
Accounts
Portfolio
Goals
Recent Activity
Documents
Advisor Notes
Alerts / Review Items
```

The advisor can ask questions against the client's complete information context.

Example:

> "What has changed since the client's last annual review?"

PIE identifies material changes and provides evidence.

---

## 10. Functional Requirements

Acceptance criteria use checklist form unless noted.

### 10.1 Client Intelligence (FR-CI)

PIE maintains a structured representation of the client derived from structured data and documents.

**Example (synthetic John Smith):**

```text
John Smith

Goals
 ├── Retirement at age 62
 ├── $180k annual retirement income
 └── Education funding

Accounts
 ├── IRA
 ├── Taxable Brokerage
 └── 401(k)

Portfolio
 ├── Equities
 ├── Fixed Income
 └── Cash

Policies
 └── IPS — June 2026

Recent Events
 ├── 401(k) rollover
 └── New taxable account
```

| ID | Requirement | Acceptance |
| --- | --- | --- |
| FR-CI-01 | Create and manage clients in a workspace | User can create client, open overview, see accounts/docs sections |
| FR-CI-02 | Derive goals, accounts, policies from ingest | After IPS + statements ingest, workspace shows goals and accounts present in sources |
| FR-CI-03 | Facts carry provenance | Inspecting a derived fact shows source document and date |

### 10.2 Ask PIE (FR-Ask)

Users ask natural-language questions about a client.

**Example questions:**

* "What are John's current investment objectives?"
* "What changed in his portfolio over the last six months?"
* "Which holdings exceed the concentration limits in the IPS?"
* "Why did portfolio performance differ from the benchmark?"
* "What did we discuss during the last three meetings?"
* "Show me everything related to the upcoming retirement."

| ID | Requirement | Acceptance |
| --- | --- | --- |
| FR-Ask-01 | Structured response | Response includes: answer, supporting evidence, source documents, relevant dates, and calculations when applicable |
| FR-Ask-02 | Client-scoped retrieval | Tools and retrieval only access the authorized client context |
| FR-Ask-03 | Tool-using agent | Agent may call document, portfolio, graph, history, and calculator tools as needed |
| FR-Ask-04 | No unsupported recommendations | Validation rejects or rewrites outputs that prescribe trades or personalized advice |

### 10.3 Evidence & Citations (FR-Cite)

Every factual response is grounded in available evidence.

**Example:**

XYZ currently represents **17.8%** of the taxable portfolio.

The latest IPS specifies a **10% maximum allocation to an individual security**.

**Sources**

* Portfolio Statement — September 20, 2026, page 4
* Investment Policy Statement — June 14, 2026, page 8

| ID | Requirement | Acceptance |
| --- | --- | --- |
| FR-Cite-01 | Clickable citations | User can open citation to source page or chunk snippet |
| FR-Cite-02 | Claim typing | UI distinguishes directly extracted facts, calculated values, and model-generated interpretations |
| FR-Cite-03 | Citation completeness | Factual claims in the answer have at least one supporting citation or calculator reference |

### 10.4 Client Review Preparation (FR-Review)

Advisor selects **Prepare Client Review**; PIE generates a structured briefing from current and historical information.

**Required briefing sections:**

* Portfolio summary (value, YTD return, benchmark when available)
* Items for advisor review (flags)
* Recent changes
* Client goals
* Recent discussions
* Key documents

| ID | Requirement | Acceptance |
| --- | --- | --- |
| FR-Review-01 | Section completeness | Generated briefing includes all required sections above |
| FR-Review-02 | Flags are evidenced | Each review item cites sources or calculated metrics |
| FR-Review-03 | Latency target | Briefing completes in < 30 seconds under prototype load (NFR) |

### 10.5 Change Detection (FR-Change)

PIE compares client states across two as-of dates and identifies portfolio, circumstance, and document changes.

**Material change defaults:**

* Allocation drift ≥ **5 percentage points** vs IPS target (or prior state when comparing snapshots)
* **Any** IPS limit breach (e.g. concentration)
* **New or removed** security
* Cash change **> 10%** of portfolio market value

Routine changes below these thresholds may be listed separately or omitted from “material” sections.

| ID | Requirement | Acceptance |
| --- | --- | --- |
| FR-Change-01 | Two-date compare | User selects two as-of dates and receives a structured diff |
| FR-Change-02 | Material vs routine | Diff labels material changes using the thresholds above |
| FR-Change-03 | Categories | Diff covers portfolio, client circumstances (when documented), and documents |

### 10.6 Portfolio Investigation (FR-Inv)

For questions such as “Why did the portfolio underperform its benchmark?”, PIE orchestrates retrieval and calculation.

**Intended workflow:**

```text
Question
   ↓
Retrieve portfolio data
   ↓
Retrieve historical holdings
   ↓
Calculate performance
   ↓
Retrieve benchmark data
   ↓
Perform attribution (as available in MVP)
   ↓
Retrieve relevant research
   ↓
Generate explanation
   ↓
Attach evidence
```

| ID | Requirement | Acceptance |
| --- | --- | --- |
| FR-Inv-01 | Deterministic math | Performance, allocation, concentration, drift use backend calculators |
| FR-Inv-02 | LLM explains, does not invent numbers | Numbers in the answer match calculator outputs within rounding tolerance |
| FR-Inv-03 | Evidence attached | Explanation includes citations and/or calculator provenance |

### 10.7 Document Intelligence (FR-Doc)

**Supported initial document types:** PDF statements, IPS, manager reports, meeting notes, CSV portfolio files, research documents, client-provided documents (synthetic).

**Ingestion pipeline:**

```text
Document → Parse → Classify → Extract entities → Extract facts
        → Generate embeddings → Create relationships → Store provenance
```

| ID | Requirement | Acceptance |
| --- | --- | --- |
| FR-Doc-01 | Upload & store | User can upload supported PDFs; metadata and raw file stored with client scope |
| FR-Doc-02 | Parse text-layer PDFs | Text extracted via Apache Tika / PDFBox for text-layer PDFs |
| FR-Doc-03 | Classify | Document type assigned (IPS, statement, notes, research, other) |
| FR-Doc-04 | Chunk + embed | Chunks stored with embeddings in pgvector and linked to document/page |
| FR-Doc-05 | Graph links | Entities/relationships written to Neo4j with provenance |

### 10.8 Basic Portfolio Analysis (FR-Calc)

| ID | Requirement | Acceptance |
| --- | --- | --- |
| FR-Calc-01 | Allocation | Compute asset-class weights from holdings |
| FR-Calc-02 | Concentration | Compute single-security weights; compare to IPS limits when present |
| FR-Calc-03 | Drift | Compute allocation vs IPS targets |
| FR-Calc-04 | Performance | Compute basic return metrics from available snapshots/transactions for demo data |

---

## 11. MVP Scope

### 11.1 MVP capabilities (Must)

1. **Client workspace** — create and manage synthetic clients
2. **Document ingestion** — IPS, portfolio statement, meeting notes, research documents
3. **Structured portfolio data** — CSV import (`account`, `security`/`ticker`, `quantity`, `price`, `market_value`, `as_of_date`, optional `asset_class`)
4. **Ask PIE** — natural-language questions over the client
5. **Evidence** — document/page (or chunk) citations on answers
6. **Client review briefing** — structured review summary
7. **Change detection** — compare two client/portfolio states
8. **Basic portfolio analysis** — allocation, concentration, performance, drift
9. **Authorization demo** — unauthorized users cannot access another client’s data
10. **Evaluation suite** — automated benchmark against known Q&A pairs

### 11.2 MoSCoW

* **Must:** all items in §11.1
* **Could / Won’t for MVP:** CRM integration, custodian sync, tax intelligence, household view, meeting assistant, research assistant, automated monitoring, cross-client NL analytics (§15)

### 11.3 Implementation order

1. Auth + client isolation + audit skeleton
2. Client workspace + document/CSV ingest + provenance store
3. Structured holdings + calculators
4. Hybrid retrieval + Ask PIE + citations
5. Review briefing + change detection
6. Evaluation suite + demo script

### 11.4 MVP data contract — synthetic demo pack

**Client:** John Smith (synthetic)

**Required assets:**

```text
John_Smith_IPS.pdf
John_Smith_Statement_June.pdf
John_Smith_Statement_September.pdf
John_Smith_Meeting_Notes.pdf
John_Smith_Portfolio.csv
```

**CSV columns (required):** `account`, `security` or `ticker`, `quantity`, `price`, `market_value`, `as_of_date`  
**Optional:** `asset_class`

**Minimum IPS extractables:** goals, target allocation ranges, single-security concentration limits.

### 11.5 Example MVP demo script

Advisor uploads the demo pack, then:

1. “Prepare me for John's annual review.” → briefing generated  
2. “Why did you flag XYZ?” → concentration vs IPS with citations  
3. “When did this position become concentrated?” → timeline from historical states  

Target: **2–3 minute** GitHub demo video.

### 11.6 Definition of Done — MVP

The MVP is complete when a user can:

1. Create a synthetic client
2. Upload client documents
3. Import portfolio data
4. Ask natural-language questions
5. Receive evidence-backed answers
6. Click citations to inspect source material
7. Compare portfolio states across two dates
8. Generate a client-review briefing
9. Identify portfolio discrepancies against an IPS
10. Inspect reasoning inputs and sources used by the system
11. Demonstrate that unauthorized users cannot retrieve another client's data
12. Run an automated evaluation suite against a benchmark dataset

---

## 12. Technical Architecture

### 12.1 High-level architecture

```text
                 ┌───────────────────┐
                 │    Web Client     │
                 │   React / Next.js │
                 └─────────┬─────────┘
                           │
                    REST / WebSocket
                           │
                 ┌─────────▼─────────┐
                 │   Spring Boot     │
                 │     API Layer     │
                 └─────────┬─────────┘
                           │
                 ┌─────────▼─────────┐
                 │ Agent Orchestrator │
                 │    LangChain4j     │
                 └─────────┬─────────┘
                           │
          ┌────────────────┼─────────────────┐
          │                │                 │
          ▼                ▼                 ▼
     RAG Agent        Data Agent       Graph Agent
          │                │                 │
          ▼                ▼                 ▼
      pgvector        PostgreSQL          Neo4j
```

This is the **target architecture for MVP**, not deferred design.

### 12.2 Data architecture

**PostgreSQL** stores: clients, accounts, transactions, holdings, documents, extracted facts, users, permissions, audit records, portfolio snapshots (as-of).

**pgvector** stores: document embeddings, semantic chunks, meeting-note embeddings, research embeddings.

**Neo4j** relationships (illustrative):

```text
Client ─OWNS→ Account ─HOLDS→ Security
Client ─HAS_GOAL→ FinancialGoal
Client ─HAS_POLICY→ IPS
Document ─DESCRIBES→ Account
Security ─ISSUED_BY→ Company
```

Temporal edges/properties use `valid_from` / `valid_to` (or equivalent snapshot linkage) for as-of queries.

### 12.3 Retrieval architecture

PIE combines multiple retrieval mechanisms rather than vector search alone:

* **Semantic** — conceptual questions
* **BM25 / keyword** — exact names, tickers, identifiers, terminology
* **Structured (SQL)** — balances, holdings, dates, transactions, calculated metrics
* **Graph** — relationships, multi-hop entity questions
* **Temporal** — historical state, changes between dates, as-of questions

### 12.4 Agent architecture

Example tools:

```text
search_documents()
search_client_facts()
get_portfolio()
get_holdings()
get_transactions()
get_client_history()
query_graph()
calculate_performance()
compare_portfolios()
get_source_document()
```

The model selects tools per question.

**Validation layer checks:**

* factual claims have evidence
* citations exist
* calculations are supported by calculator outputs
* response does not contain unsupported recommendations

---

## 13. Security & Permissions

Wealth-management information is highly sensitive; security is a first-class requirement.

**MVP includes:**

* authentication (JWT or local auth for demo)
* role-based access control
* client-level authorization
* tenant isolation (designed + tested)
* encrypted storage (at rest for demo deployment config)
* encrypted transport (TLS in deployed environments)
* audit logging
* document access controls

Every retrieval request is evaluated against the user's authorization context. A user must never retrieve information about a client they are not permitted to access (**unauthorized exposure target: 0**).

---

## 14. Human-in-the-Loop Design

PIE explicitly distinguishes:

| Category | Example | System role |
| --- | --- | --- |
| **Facts** | XYZ represents 17.8% of the taxable portfolio | Extract / retrieve |
| **Calculations** | Allocation is 7.8 pp above the IPS threshold | Deterministic compute |
| **Interpretations** | This may warrant advisor review | Assist, label as interpretation |
| **Decisions** | Sell XYZ | **Advisor only** — system must not prescribe |

The system assists with the first three. The final category remains with the human advisor.

---

## 15. Future Features (post-MVP)

* **CRM integration** — notes and activities
* **Custodian integration** — synchronized portfolio information
* **Tax intelligence** — surface tax-related questions and documents (not advice)
* **Household view** — individuals, families, trusts, businesses, accounts
* **Meeting assistant** — summaries and action items
* **Research assistant** — link research to client portfolios
* **Automated monitoring** — notify on material changes
* **Natural-language analytics** — e.g. “Which clients are outside documented allocation targets?”

---

## 16. Evaluation

Evaluation is a major part of the project.

Create a synthetic benchmark with questions and known answers. Measure:

**Retrieval:** Recall@K, MRR, nDCG  

**Generation:** answer correctness, citation correctness, groundedness, unsupported claim rate  

**Agent:** tool selection accuracy, unnecessary tool calls, task completion rate  

**System:** latency, token usage, cost per query  

**Architecture comparisons (research track):**

```text
Vector RAG
      vs
Hybrid RAG
      vs
Hybrid RAG + Knowledge Graph
      vs
Hybrid + Graph + Temporal Retrieval
```

---

## 17. Non-Functional Requirements & Success Metrics

Prototype engineering targets (not production SLAs):

* Citation correctness **> 95%**
* Supported factual claims **> 95%**
* Retrieval Recall@10 **> 85%**
* Client-review generation **< 30 s** (p95 prototype)
* Basic question response **< 8 s** (p95 prototype)
* Unauthorized data exposure **0**
* OpenTelemetry traces for retrieval and tool calls
* Testcontainers for PostgreSQL and Neo4j; eval harness runnable in CI for a subset of the benchmark

---

## 18. Project Positioning

PIE is an engineering/research project exploring:

**LLM Agents + RAG + Knowledge Graphs + Temporal Reasoning + Financial Data + Evidence Grounding**

It demonstrates that modern LLM systems integrate with traditional software engineering and data systems rather than treating the LLM as the entire application.

**Central technical thesis:**

> Reliable domain-specific AI requires combining language models with structured data, deterministic computation, retrieval, temporal context, and explicit provenance.

---

## 19. Technology Stack

| Layer | Technology |
| --- | --- |
| Language | Java 21 |
| Backend | Spring Boot |
| LLM orchestration | LangChain4j |
| Database | PostgreSQL |
| Vector DB | pgvector |
| Graph DB | Neo4j |
| Cache | Redis |
| Document parsing | Apache Tika / PDFBox |
| Frontend | React / Next.js |
| Testing | JUnit / Testcontainers |
| Containers | Docker |
| CI | GitHub Actions |
| Observability | OpenTelemetry |
| LLM | Provider-agnostic abstraction |

---

## 20. Risks & Open Questions

| ID | Topic | When | Mitigation direction |
| --- | --- | --- | --- |
| R-01 | Representing facts across document versions | MVP | Versioned facts + as-of; prefer latest IPS unless queried historically |
| R-02 | When to use vector search vs SQL | MVP | Router / agent tools: numbers → SQL/calculators; narrative → hybrid retrieval |
| R-03 | When graph retrieval improves quality | MVP | Ablation in eval suite (hybrid vs hybrid+graph) |
| R-04 | Temporal fact modeling | MVP | Snapshots + `valid_from`/`valid_to` on graph edges |
| R-05 | Measuring hallucinations reliably | MVP | Groundedness + unsupported-claim metrics on labeled benchmark |
| R-06 | Citation correctness evaluation | MVP | Automated citation checks against gold spans |
| R-07 | Conflicting documents | MVP | Surface conflict explicitly with both sources; do not silently pick |
| R-08 | Stale information | MVP | Show as-of dates; flag older sources when newer exist |
| R-09 | Confidence representation | Post-MVP polish | Start with claim type labels; numeric confidence later |
| R-10 | Preventing advice-like outputs | MVP | Validation layer / recommendation detector before response return |

---

## 21. Long-Term Vision

PIE should evolve from document Q&A into a **persistent financial intelligence layer**.

Instead of only asking “What does this document say?”, the advisor asks:

* What's changed?
* Why?
* Show me the evidence.
* What should I review?
* What did we know at that point in time?

**The long-term product is not a chatbot.**

It is a continuously updated, evidence-backed model of the advisor's clients and their financial context.
