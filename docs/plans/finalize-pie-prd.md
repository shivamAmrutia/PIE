# Finalize PIE PRD

**Status:** Executed
**Locked decisions:** Hybrid (C) + Keep full MVP
**Created:** 2026-09-26
**Completed:** 2026-09-26

## Overview

Produce a finalized, production-credible PRD for PIE that keeps the full MVP scope, frames v1 as a synthetic-data demo with architecture suitable for a later firm pilot, and fills the gaps (acceptance criteria, assumptions, compliance posture, priorities) that turn the current draft into a build-ready requirements document.

## Todos

- [x] Write `docs/PRD.md` v1.0 with locked hybrid + full MVP decisions, new sections (glossary, assumptions, compliance, user stories, FRs+ACs, NFRs, data contract, MoSCoW, risks)
- [x] Add root `README.md` with project one-liner and link to `docs/PRD.md`

## Decisions locked

- **Intent:** Hybrid — v1 ships as a synthetic-data engineering demo; architecture, security model, and requirements stay production-credible for a later controlled pilot.
- **Scope:** Keep the full MVP (workspace, ingestion, CSV portfolio, Ask PIE, evidence, review briefing, change detection, basic portfolio analysis, authz demo, evaluation suite).

## Deliverable

Create [`docs/PRD.md`](../PRD.md) as **Version 1.0 / Status: Final (MVP)**. The empty repo has no existing docs; this becomes the source of truth before implementation.

## What stays (lightly edited)

Keep the draft’s strength intact:

- Product overview, problem, vision, non-goals, human-in-the-loop boundaries
- Feature sections 8–14 (Client Intelligence, Ask PIE, Evidence, Review Prep, Change Detection, Portfolio Investigation, Document Intelligence)
- Architecture sections 15–18 and stack (§28) — treat as **target architecture for MVP**, not “future only”
- Evaluation, success metrics, DoD, positioning, long-term vision

Edits while preserving content:

- Replace aspirational “eventually” language in vision with **MVP vs later** labels where needed
- Normalize dates/examples to the synthetic **John Smith** demo narrative
- Prefer bullet lists over markdown tables for success metrics where helpful
- Collapse “Open Technical Questions” into an **Open Questions / Risks** appendix with owners (Engineering) and “resolve during MVP” vs “post-MVP”

## Sections to add or harden

### 1. Document control

- Status, version **1.0**, authors placeholder, last updated
- Glossary: IPS, concentration, drift, provenance, groundedness, tenant, as-of date, material change

### 2. Assumptions & constraints

Explicit assumptions, e.g.:

- All MVP data is **synthetic**; no real PII / client data
- Single firm / single tenant for demo; multi-tenant isolation **designed and tested**, not productized onboarding
- LLM via provider-agnostic abstraction; local/dev can use a configured cloud model
- English-only documents for MVP
- PDF + CSV primary formats; OCR of scanned PDFs out of MVP unless Tika/PDFBox text layer is present

Constraints:

- Advisor remains decision-maker; UI never presents “Sell / Buy / Recommend” as system advice
- Deterministic finance math outside the LLM whenever possible

### 3. Compliance & regulatory posture (production-credible, demo-scoped)

Short section stating:

- PIE is **decision support / information retrieval**, not investment advice
- Required disclaimers in UI for Ask PIE and review briefings
- Audit logging of queries and document access
- No client-facing channels in MVP
- Real-data pilot would require firm policy, DPA, and retention rules (out of MVP build, noted as gate)

### 4. Personas → prioritized user stories

Keep three personas; add **P0 user stories** mapped to MVP DoD, e.g.:

- As an advisor, I can upload IPS/statements/notes and import holdings CSV for a client
- As an advisor, I can ask NL questions and get answer + citations + dates
- As an advisor, I can open a citation to the source page/snippet
- As an advisor, I can generate a client-review briefing with review flags
- As an advisor, I can compare two as-of dates and see material portfolio/document changes
- As an advisor, I can see allocation, concentration, and IPS drift from calculated (not LLM) metrics
- As a CSA, I can locate documents and verify facts via citations
- As a security constraint, a user without client access cannot retrieve that client’s data via API or Ask PIE

### 5. Functional requirements with acceptance criteria

Rewrite features 7–14 as numbered **FRs** with measurable acceptance criteria (Given/When/Then or checklist). Examples:

- **FR-Ask-01:** Response includes answer, evidence list, source refs (doc id, page or chunk), and as-of/relevant dates when applicable
- **FR-Cite-01:** Citations distinguish extracted vs calculated vs interpreted claims in the UI
- **FR-Calc-01:** Allocation/concentration/drift/performance use backend calculators; LLM may explain results but not invent numbers
- **FR-Review-01:** Briefing includes portfolio summary, review items, recent changes, goals, discussions, key docs
- **FR-Change-01:** Diff across two portfolio snapshots flags material vs routine (concrete defaults: **5 pp allocation drift**, **any IPS limit breach**, **new/removed security**, **cash change > 10% of portfolio**)

### 6. Non-functional requirements

Promote metrics into NFRs:

- Latency: basic Q&A < 8s p95; review briefing < 30s p95 (prototype targets)
- Citation correctness / groundedness / Recall@10 targets as in draft
- Unauthorized exposure: **0**
- Observability: OpenTelemetry traces for retrieval + tool calls
- Testability: Testcontainers for Postgres/Neo4j; eval harness runnable in CI for a subset

### 7. MVP data contract (synthetic demo pack)

Specify the required demo assets and schemas so implementation is unambiguous:

- Client: John Smith (synthetic)
- Files: IPS, June statement, September statement, meeting notes, portfolio CSV
- CSV columns: `account`, `security`/`ticker`, `quantity`, `price`, `market_value`, `as_of_date` (and optional `asset_class`)
- Minimum extractable IPS fields: goals, target allocation ranges, concentration limits

### 8. Priority / MoSCoW for full MVP

All current MVP items stay **Must**; Future Features stay **Could/Won’t for MVP**. Within Must, order implementation dependencies:

1. Auth + client isolation + audit skeleton
2. Client workspace + document/CSV ingest + provenance store
3. Structured holdings + calculators
4. Hybrid retrieval + Ask PIE + citations
5. Review briefing + change detection
6. Evaluation suite + demo script

### 9. Out of scope clarification

Keep Non-Goals; add explicit MVP outs: CRM/custodian sync, OCR-heavy scans, tax advice, household multi-entity modeling, real-time market data feeds, production SSO (use local/JWT auth for demo; design for later IdP).

### 10. Risks

Convert open questions into tracked risks with mitigation direction (e.g. citation eval harness, separate “recommendation detector” validation layer, temporal snapshot tables + graph edges with `valid_from`/`valid_to`).

## Structure of the final `docs/PRD.md`

1. Document control + glossary
2. Overview / problem / vision / positioning
3. Users & user stories
4. Goals, non-goals, assumptions, compliance posture
5. UX: Client workspace
6. Functional requirements (intelligence, Ask, evidence, review, change, investigation, documents)
7. MVP scope, demo script, data contract, DoD
8. Architecture (system, data, retrieval, agents) — production-credible
9. Security & HITL
10. NFRs, evaluation, success metrics
11. Future features
12. Risks / open questions
13. Stack

## What we will not do in this step

- No application code, Docker, or scaffolding yet
- No separate tech design doc beyond what already belongs in the PRD architecture sections
- No shrinking MVP features

## After approval

On execute: write the finalized [`docs/PRD.md`](../PRD.md) only (plus a one-line pointer in a root [`README.md`](../../README.md) linking to it).
