"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { api, saveSession, AuthSession } from "@/lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("advisor@pie.demo");
  const [password, setPassword] = useState("password");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const session = await api<AuthSession>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      saveSession(session);
      router.push("/clients");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="shell">
      <div className="topbar">
        <div>
          <h1 className="brand">PIE</h1>
          <p className="sub">
            Portfolio Intelligence Engine — evidence-backed context for wealth advisors.
            Not investment advice.
          </p>
        </div>
      </div>

      <div className="grid two">
        <section className="panel">
          <h2 className="h2">Sign in</h2>
          <form onSubmit={onSubmit}>
            <div style={{ marginBottom: "0.9rem" }}>
              <label htmlFor="email">Email</label>
              <input
                id="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="username"
              />
            </div>
            <div style={{ marginBottom: "1rem" }}>
              <label htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
              />
            </div>
            <button className="btn" type="submit" disabled={loading}>
              {loading ? "Signing in…" : "Continue"}
            </button>
            {error && <p className="error">{error}</p>}
          </form>
          <p className="disclaimer">
            Demo users: advisor@pie.demo / csa@pie.demo / outsider@pie.demo — password{" "}
            <code>password</code>. Outsider has no John Smith access.
          </p>
        </section>

        <section className="panel">
          <h2 className="h2">What you can do in MVP slice 1</h2>
          <ul className="list">
            <li>Authenticate with JWT and tenant-scoped identity</li>
            <li>List only clients you are authorized to see</li>
            <li>Open a client workspace with accounts and documents</li>
            <li>Import holdings CSV and run deterministic portfolio analysis</li>
          </ul>
        </section>
      </div>
    </main>
  );
}
