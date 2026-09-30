# MVP Implementation Tracker

**Started:** 2026-09-26  
**PRD:** [docs/PRD.md](../PRD.md)

## Implementation order (from PRD)

1. Auth + client isolation + audit — done
2. Client workspace + document/CSV ingest + provenance — done
3. Structured holdings + calculators — done
4. Hybrid retrieval + Ask PIE + citations — done
5. **Review briefing + change detection — done (slice 3)**
6. Evaluation suite + demo script — next

## Slice 3 delivered

- `GET /api/clients/{id}/changes?fromDate=&toDate=` — material vs routine diffs
  - New/removed securities, ≥5pp weight/allocation drift, cash change >10% of portfolio, IPS concentration breaches
- `POST /api/clients/{id}/review/prepare` — client review briefing
  - Portfolio summary, review flags, recent changes, goals, discussions, key documents
- Workspace UI: Prepare Client Review + Change detection date compare

## Try it

1. Import both `demo/John_Smith_Portfolio_June.csv` and `..._September.csv`
2. Upload IPS + meeting notes
3. Click **Prepare Client Review**
4. Compare `2026-06-20` → `2026-09-20` under Change detection

## Next slice

- Evaluation harness (Recall@K, citation correctness, groundedness)
- Demo script / seed pack automation
- Optional LLM composer (LangChain4j) when API key configured
