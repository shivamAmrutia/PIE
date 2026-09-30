"use client";

import { FormEvent, useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { api, loadSession, AuthSession } from "@/lib/api";

type Workspace = {
  client: { id: string; displayName: string; externalRef?: string };
  accounts: { id: string; name: string; accountType: string }[];
  documents: { id: string; filename: string; docType: string; createdAt: string }[];
  holdingAsOfDates: string[];
  latestHoldings: {
    ticker: string;
    accountName: string;
    marketValue: number;
    assetClass?: string;
    asOfDate: string;
  }[];
  disclaimer: string;
};

type Analysis = {
  asOfDate: string;
  totalMarketValue: number;
  allocation: { assetClass: string; marketValue: number; weightPct: number }[];
  concentration: { ticker: string; marketValue: number; weightPct: number }[];
  claimType: string;
  provenanceNote: string;
};

type AskResponse = {
  answer: string;
  claims: { text: string; claimType: string; evidenceRefs: string[] }[];
  citations: {
    documentId: string;
    chunkId?: string;
    filename: string;
    docType: string;
    pageNumber?: number;
    snippet: string;
    claimType: string;
  }[];
  toolsUsed: string[];
  disclaimer: string;
  adviceFilterApplied: boolean;
  validationNote?: string;
  engine: string;
};

type ChangeItem = {
  category: string;
  severity: string;
  summary: string;
  detail: string;
  evidence: string;
};

type ChangeDetection = {
  fromDate: string;
  toDate: string;
  fromTotal: number;
  toTotal: number;
  totalChangePct: number;
  materialChanges: ChangeItem[];
  routineChanges: ChangeItem[];
  documentChanges: ChangeItem[];
};

type ReviewBriefing = {
  clientName: string;
  asOfDate: string;
  portfolioValue: number;
  periodReturnPct?: number | null;
  benchmarkNote: string;
  reviewItems: {
    severity: string;
    title: string;
    detail: string;
    evidence: string;
    claimType: string;
  }[];
  recentChanges: ChangeItem[];
  goals: string[];
  recentDiscussions: string[];
  keyDocuments: { filename: string; docType: string }[];
  disclaimer: string;
};

export default function ClientWorkspacePage() {
  const params = useParams<{ clientId: string }>();
  const router = useRouter();
  const [session, setSession] = useState<AuthSession | null>(null);
  const [workspace, setWorkspace] = useState<Workspace | null>(null);
  const [analysis, setAnalysis] = useState<Analysis | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [question, setQuestion] = useState("Which holdings exceed the concentration limits in the IPS?");
  const [askResult, setAskResult] = useState<AskResponse | null>(null);
  const [asking, setAsking] = useState(false);
  const [briefing, setBriefing] = useState<ReviewBriefing | null>(null);
  const [changes, setChanges] = useState<ChangeDetection | null>(null);
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [busy, setBusy] = useState(false);

  async function refresh(s: AuthSession) {
    const ws = await api<Workspace>(
      `/api/clients/${params.clientId}/workspace`,
      {},
      s.accessToken
    );
    setWorkspace(ws);
    if (ws.holdingAsOfDates?.length >= 2) {
      setToDate(ws.holdingAsOfDates[0]);
      setFromDate(ws.holdingAsOfDates[1]);
    } else if (ws.holdingAsOfDates?.length === 1) {
      setToDate(ws.holdingAsOfDates[0]);
    }
    try {
      const a = await api<Analysis>(
        `/api/clients/${params.clientId}/portfolio/analysis`,
        {},
        s.accessToken
      );
      setAnalysis(a);
    } catch {
      setAnalysis(null);
    }
  }

  useEffect(() => {
    const s = loadSession();
    if (!s) {
      router.replace("/");
      return;
    }
    setSession(s);
    refresh(s).catch((err) =>
      setError(err instanceof Error ? err.message : "Failed to load workspace")
    );
  }, [params.clientId, router]);

  async function onImport(file: File | null) {
    if (!file || !session) return;
    setUploading(true);
    setError(null);
    try {
      const body = new FormData();
      body.append("file", file);
      await api(
        `/api/clients/${params.clientId}/holdings/import`,
        { method: "POST", body },
        session.accessToken
      );
      await refresh(session);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Import failed");
    } finally {
      setUploading(false);
    }
  }

  async function onUploadDoc(file: File | null) {
    if (!file || !session) return;
    setUploading(true);
    setError(null);
    try {
      const body = new FormData();
      body.append("file", file);
      await api(
        `/api/clients/${params.clientId}/documents`,
        { method: "POST", body },
        session.accessToken
      );
      await refresh(session);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Upload failed");
    } finally {
      setUploading(false);
    }
  }

  async function onAsk(e: FormEvent) {
    e.preventDefault();
    if (!session || !question.trim()) return;
    setAsking(true);
    setError(null);
    try {
      const result = await api<AskResponse>(
        `/api/clients/${params.clientId}/ask`,
        { method: "POST", body: JSON.stringify({ question }) },
        session.accessToken
      );
      setAskResult(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Ask PIE failed");
    } finally {
      setAsking(false);
    }
  }

  async function prepareReview() {
    if (!session) return;
    setBusy(true);
    setError(null);
    try {
      const result = await api<ReviewBriefing>(
        `/api/clients/${params.clientId}/review/prepare`,
        { method: "POST" },
        session.accessToken
      );
      setBriefing(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Review briefing failed");
    } finally {
      setBusy(false);
    }
  }

  async function runChangeDetection() {
    if (!session) return;
    setBusy(true);
    setError(null);
    try {
      const qs = new URLSearchParams();
      if (fromDate) qs.set("fromDate", fromDate);
      if (toDate) qs.set("toDate", toDate);
      const result = await api<ChangeDetection>(
        `/api/clients/${params.clientId}/changes?${qs.toString()}`,
        {},
        session.accessToken
      );
      setChanges(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Change detection failed");
    } finally {
      setBusy(false);
    }
  }

  if (!session) return null;

  return (
    <main className="shell">
      <div className="topbar">
        <div>
          <p className="muted">
            <Link href="/clients">← Clients</Link>
          </p>
          <h1 className="brand">{workspace?.client.displayName || "Client"}</h1>
          <p className="sub">
            Client workspace — Ask PIE, review briefing, and change detection across as-of dates.
          </p>
        </div>
      </div>

      {error && <p className="error">{error}</p>}

      <section className="panel" style={{ marginBottom: "1rem" }}>
        <div className="row">
          <h2 className="h2" style={{ margin: 0 }}>
            Client review
          </h2>
          <button className="btn" onClick={prepareReview} disabled={busy}>
            {busy && !briefing ? "Preparing…" : "Prepare Client Review"}
          </button>
        </div>
        {briefing && (
          <div style={{ marginTop: "1rem" }}>
            <p>
              <strong>{briefing.clientName}</strong> · as of {briefing.asOfDate} ·{" "}
              {Number(briefing.portfolioValue).toLocaleString(undefined, {
                style: "currency",
                currency: "USD",
              })}
              {briefing.periodReturnPct != null && (
                <> · period change {Number(briefing.periodReturnPct).toFixed(2)}%</>
              )}
            </p>
            <p className="muted">{briefing.benchmarkNote}</p>

            <h3>Items for advisor review</h3>
            <ul className="list">
              {briefing.reviewItems.map((item, idx) => (
                <li key={idx}>
                  <div className="row">
                    <strong>{item.title}</strong>
                    <span className="pill">{item.severity}</span>
                  </div>
                  <div className="muted">{item.detail}</div>
                  <div className="muted">Evidence: {item.evidence}</div>
                </li>
              ))}
            </ul>

            <h3>Recent material changes</h3>
            <ul className="list">
              {briefing.recentChanges.map((c, idx) => (
                <li key={idx}>
                  {c.summary}
                  <div className="muted">{c.detail}</div>
                </li>
              ))}
              {briefing.recentChanges.length === 0 && (
                <li className="muted">Import two as-of CSVs to populate period changes.</li>
              )}
            </ul>

            <h3>Goals</h3>
            <ul className="list">
              {briefing.goals.map((g, idx) => (
                <li key={idx}>{g}</li>
              ))}
              {briefing.goals.length === 0 && <li className="muted">Upload an IPS to extract goals.</li>}
            </ul>

            <h3>Recent discussions</h3>
            <ul className="list">
              {briefing.recentDiscussions.map((d, idx) => (
                <li key={idx} className="muted">
                  {d}
                </li>
              ))}
              {briefing.recentDiscussions.length === 0 && (
                <li className="muted">Upload meeting notes to populate this section.</li>
              )}
            </ul>

            <h3>Key documents</h3>
            <ul className="list">
              {briefing.keyDocuments.map((d, idx) => (
                <li key={idx} className="row">
                  <span>{d.filename}</span>
                  <span className="pill">{d.docType}</span>
                </li>
              ))}
            </ul>
            <p className="disclaimer">{briefing.disclaimer}</p>
          </div>
        )}
      </section>

      <section className="panel" style={{ marginBottom: "1rem" }}>
        <h2 className="h2">Change detection</h2>
        <div className="grid two">
          <div>
            <label htmlFor="fromDate">From as-of</label>
            <input
              id="fromDate"
              type="date"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
            />
          </div>
          <div>
            <label htmlFor="toDate">To as-of</label>
            <input
              id="toDate"
              type="date"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
            />
          </div>
        </div>
        <div style={{ marginTop: "0.8rem" }}>
          <button className="btn secondary" onClick={runChangeDetection} disabled={busy}>
            Compare snapshots
          </button>
        </div>
        {workspace && workspace.holdingAsOfDates.length > 0 && (
          <p className="muted" style={{ marginTop: "0.6rem" }}>
            Available: {workspace.holdingAsOfDates.join(", ")}
          </p>
        )}
        {changes && (
          <div style={{ marginTop: "1rem" }}>
            <p>
              {changes.fromDate} → {changes.toDate} · portfolio{" "}
              {Number(changes.fromTotal).toLocaleString(undefined, {
                style: "currency",
                currency: "USD",
              })}{" "}
              →{" "}
              {Number(changes.toTotal).toLocaleString(undefined, {
                style: "currency",
                currency: "USD",
              })}{" "}
              ({Number(changes.totalChangePct).toFixed(2)}%)
            </p>
            <h3>Material</h3>
            <ul className="list">
              {changes.materialChanges.map((c, idx) => (
                <li key={idx}>
                  <div className="row">
                    <strong>{c.summary}</strong>
                    <span className="pill">{c.category}</span>
                  </div>
                  <div className="muted">{c.detail}</div>
                </li>
              ))}
              {changes.materialChanges.length === 0 && <li className="muted">None</li>}
            </ul>
            <h3>Routine</h3>
            <ul className="list">
              {changes.routineChanges.map((c, idx) => (
                <li key={idx}>
                  {c.summary}
                  <div className="muted">{c.detail}</div>
                </li>
              ))}
              {changes.routineChanges.length === 0 && <li className="muted">None</li>}
            </ul>
            <h3>Documents</h3>
            <ul className="list">
              {changes.documentChanges.map((c, idx) => (
                <li key={idx}>{c.summary}</li>
              ))}
              {changes.documentChanges.length === 0 && <li className="muted">None in window</li>}
            </ul>
          </div>
        )}
      </section>

      <section className="panel" style={{ marginBottom: "1rem" }}>
        <h2 className="h2">Ask PIE</h2>
        <form onSubmit={onAsk}>
          <label htmlFor="ask">Question</label>
          <input
            id="ask"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="Ask about goals, IPS, concentration, meetings…"
          />
          <div style={{ marginTop: "0.8rem" }}>
            <button className="btn" type="submit" disabled={asking}>
              {asking ? "Retrieving…" : "Ask"}
            </button>
          </div>
        </form>

        {askResult && (
          <div style={{ marginTop: "1.2rem" }}>
            <pre
              style={{
                whiteSpace: "pre-wrap",
                fontFamily: "var(--font-body)",
                margin: 0,
                lineHeight: 1.5,
              }}
            >
              {askResult.answer}
            </pre>

            <div style={{ marginTop: "0.75rem" }}>
              {askResult.toolsUsed.map((t) => (
                <span key={t} className="pill" style={{ marginRight: "0.35rem" }}>
                  {t}
                </span>
              ))}
              <span className="pill">{askResult.engine}</span>
            </div>

            {askResult.adviceFilterApplied && (
              <p className="muted">Validation: {askResult.validationNote}</p>
            )}

            <h3 style={{ marginTop: "1rem" }}>Claims</h3>
            <ul className="list">
              {askResult.claims.map((c, idx) => (
                <li key={idx} className="row">
                  <span>{c.text}</span>
                  <span className="pill">{c.claimType}</span>
                </li>
              ))}
            </ul>

            <h3 style={{ marginTop: "1rem" }}>Citations</h3>
            <ul className="list">
              {askResult.citations.map((c, idx) => (
                <li key={idx}>
                  <strong>{c.filename}</strong>
                  {c.pageNumber ? ` · p.${c.pageNumber}` : ""} · {c.claimType}
                  <div className="muted">{c.snippet}</div>
                </li>
              ))}
              {askResult.citations.length === 0 && (
                <li className="muted">No document citations for this answer.</li>
              )}
            </ul>

            <p className="disclaimer">{askResult.disclaimer}</p>
          </div>
        )}
      </section>

      <div className="grid two">
        <section className="panel">
          <h2 className="h2">Accounts</h2>
          <ul className="list">
            {workspace?.accounts.map((a) => (
              <li key={a.id} className="row">
                <span>{a.name}</span>
                <span className="pill">{a.accountType}</span>
              </li>
            ))}
          </ul>

          <h2 className="h2" style={{ marginTop: "1.5rem" }}>
            Documents
          </h2>
          <ul className="list">
            {workspace?.documents.map((d) => (
              <li key={d.id} className="row">
                <span>{d.filename}</span>
                <span className="pill">{d.docType}</span>
              </li>
            ))}
            {workspace && workspace.documents.length === 0 && (
              <li className="muted">No documents yet.</li>
            )}
          </ul>

          <div style={{ marginTop: "1rem" }}>
            <label>Upload document</label>
            <input
              type="file"
              disabled={uploading}
              onChange={(e) => onUploadDoc(e.target.files?.[0] || null)}
            />
          </div>
          <div style={{ marginTop: "1rem" }}>
            <label>Import holdings CSV</label>
            <input
              type="file"
              accept=".csv"
              disabled={uploading}
              onChange={(e) => onImport(e.target.files?.[0] || null)}
            />
          </div>
        </section>

        <section className="panel">
          <h2 className="h2">Portfolio analysis</h2>
          {!analysis && (
            <p className="muted">Import a holdings CSV to see allocation and concentration.</p>
          )}
          {analysis && (
            <>
              <p>
                As of <strong>{analysis.asOfDate}</strong> · Total{" "}
                <strong>
                  {analysis.totalMarketValue.toLocaleString(undefined, {
                    style: "currency",
                    currency: "USD",
                  })}
                </strong>
              </p>
              <span className="pill">{analysis.claimType}</span>
              <p className="muted" style={{ marginTop: "0.5rem" }}>
                {analysis.provenanceNote}
              </p>

              <h3 style={{ marginTop: "1.2rem" }}>Allocation</h3>
              <table className="table">
                <thead>
                  <tr>
                    <th>Asset class</th>
                    <th>Weight %</th>
                  </tr>
                </thead>
                <tbody>
                  {analysis.allocation.map((a) => (
                    <tr key={a.assetClass}>
                      <td>{a.assetClass}</td>
                      <td>{Number(a.weightPct).toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>

              <h3 style={{ marginTop: "1.2rem" }}>Concentration</h3>
              <table className="table">
                <thead>
                  <tr>
                    <th>Ticker</th>
                    <th>Weight %</th>
                  </tr>
                </thead>
                <tbody>
                  {analysis.concentration.slice(0, 8).map((c) => (
                    <tr key={c.ticker}>
                      <td>{c.ticker}</td>
                      <td>{Number(c.weightPct).toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </>
          )}

          {workspace && <p className="disclaimer">{workspace.disclaimer}</p>}
        </section>
      </div>
    </main>
  );
}
