import { NavLink, Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth";
import { AnalyticsPage } from "./pages/AnalyticsPage";
import { LinksPage } from "./pages/LinksPage";
import { MembersPage } from "./pages/MembersPage";
import { OverviewPage } from "./pages/OverviewPage";
import { OrgProvider, useOrg } from "./org";

function LoginGate() {
  const auth = useAuth();
  if (!auth.ready) {
    return (
      <div className="login-gate">
        <div className="login-card">
          <h1>Shortener</h1>
          <p className="muted">Connecting to identity…</p>
        </div>
      </div>
    );
  }
  if (!auth.authenticated) {
    return (
      <div className="login-gate">
        <div className="login-card">
          <h1>Shortener</h1>
          <p className="muted">
            Sign in with Keycloak to manage organizations, links, and analytics.
          </p>
          <button className="btn" type="button" onClick={auth.login}>
            Sign in
          </button>
        </div>
      </div>
    );
  }
  return (
    <OrgProvider>
      <Shell />
    </OrgProvider>
  );
}

function Shell() {
  const auth = useAuth();
  const { orgs, orgId, setOrgId, loading, error, createOrg } = useOrg();

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <p className="brand">Shortener</p>
        <div className="field">
          <label htmlFor="org">Organization</label>
          <select
            id="org"
            value={orgId ?? ""}
            onChange={(e) => setOrgId(e.target.value)}
            disabled={loading || orgs.length === 0}
          >
            {orgs.map((o) => (
              <option key={o.id} value={o.id}>
                {o.name} ({o.role})
              </option>
            ))}
          </select>
        </div>
        <button
          className="btn secondary"
          type="button"
          onClick={() => {
            const name = window.prompt("Organization name");
            if (name) void createOrg(name);
          }}
        >
          New organization
        </button>
        <nav className="nav" style={{ marginTop: 20 }}>
          <NavLink to="/" end>
            Overview
          </NavLink>
          <NavLink to="/links">Links</NavLink>
          <NavLink to="/analytics">Analytics</NavLink>
          <NavLink to="/members">Members</NavLink>
        </nav>
        <div style={{ marginTop: 28 }}>
          <p className="muted" style={{ marginBottom: 8 }}>
            {auth.name ?? auth.email}
          </p>
          <button className="btn secondary" type="button" onClick={auth.logout}>
            Sign out
          </button>
        </div>
        {error ? <p className="error">{error}</p> : null}
      </aside>
      <main className="main">
        {!orgId ? (
          <div className="card">
            <h1 className="page-title">Create your first organization</h1>
            <p className="page-sub">
              Organizations own links, members, and analytics.
            </p>
            <button
              className="btn"
              type="button"
              onClick={() => {
                const name = window.prompt("Organization name", "My workspace");
                if (name) void createOrg(name);
              }}
            >
              Create organization
            </button>
          </div>
        ) : (
          <Routes>
            <Route path="/" element={<OverviewPage />} />
            <Route path="/links" element={<LinksPage />} />
            <Route path="/analytics" element={<AnalyticsPage />} />
            <Route path="/members" element={<MembersPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        )}
      </main>
    </div>
  );
}

export function App() {
  return <LoginGate />;
}
