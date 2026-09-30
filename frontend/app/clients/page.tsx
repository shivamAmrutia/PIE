"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, clearSession, loadSession, AuthSession } from "@/lib/api";

type Client = {
  id: string;
  displayName: string;
  externalRef?: string;
};

export default function ClientsPage() {
  const router = useRouter();
  const [session, setSession] = useState<AuthSession | null>(null);
  const [clients, setClients] = useState<Client[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [name, setName] = useState("");

  useEffect(() => {
    const s = loadSession();
    if (!s) {
      router.replace("/");
      return;
    }
    setSession(s);
    api<Client[]>("/api/clients", {}, s.accessToken)
      .then(setClients)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load clients"));
  }, [router]);

  async function createClient() {
    if (!session || !name.trim()) return;
    setError(null);
    try {
      const created = await api<Client>(
        "/api/clients",
        { method: "POST", body: JSON.stringify({ displayName: name.trim() }) },
        session.accessToken
      );
      setClients((prev) => [...prev, created].sort((a, b) => a.displayName.localeCompare(b.displayName)));
      setName("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Create failed");
    }
  }

  function logout() {
    clearSession();
    router.push("/");
  }

  if (!session) return null;

  return (
    <main className="shell">
      <div className="topbar">
        <div>
          <h1 className="brand">PIE</h1>
          <p className="sub">
            Signed in as {session.displayName} ({session.role})
          </p>
        </div>
        <button className="btn secondary" onClick={logout}>
          Sign out
        </button>
      </div>

      <div className="grid two">
        <section className="panel">
          <h2 className="h2">Your clients</h2>
          {error && <p className="error">{error}</p>}
          <ul className="list">
            {clients.map((c) => (
              <li key={c.id} className="row">
                <div>
                  <strong>{c.displayName}</strong>
                  {c.externalRef && <div className="muted">{c.externalRef}</div>}
                </div>
                <Link className="btn secondary" href={`/clients/${c.id}`}>
                  Open
                </Link>
              </li>
            ))}
            {clients.length === 0 && (
              <li className="muted">No accessible clients for this user.</li>
            )}
          </ul>
        </section>

        <section className="panel">
          <h2 className="h2">Create client</h2>
          <label htmlFor="cname">Display name</label>
          <input id="cname" value={name} onChange={(e) => setName(e.target.value)} />
          <div style={{ marginTop: "0.9rem" }}>
            <button className="btn" onClick={createClient}>
              Create
            </button>
          </div>
        </section>
      </div>
    </main>
  );
}
