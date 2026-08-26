import { useEffect, useState } from "react";
import { type LinkItem, useApi } from "../api";
import { useOrg } from "../org";

export function OverviewPage() {
  const api = useApi();
  const { orgId } = useOrg();
  const [links, setLinks] = useState<LinkItem[]>([]);
  const [error, setError] = useState<string>();

  useEffect(() => {
    if (!orgId) return;
    void api
      .get<LinkItem[]>(`/api/v1/orgs/${orgId}/urls`)
      .then(setLinks)
      .catch((e) => setError(e instanceof Error ? e.message : "Failed"));
  }, [api, orgId]);

  const totalClicks = links.reduce((sum, l) => sum + (l.totalClicks ?? 0), 0);
  const active = links.filter((l) => l.status === "ACTIVE").length;

  return (
    <div>
      <h1 className="page-title">Overview</h1>
      <p className="page-sub">Workspace health for the selected organization.</p>
      {error ? <p className="error">{error}</p> : null}
      <div className="stats">
        <div className="stat">
          <div className="label">Links</div>
          <div className="value">{links.length}</div>
        </div>
        <div className="stat">
          <div className="label">Active</div>
          <div className="value">{active}</div>
        </div>
        <div className="stat">
          <div className="label">Total clicks</div>
          <div className="value">{totalClicks}</div>
        </div>
        <div className="stat">
          <div className="label">Disabled</div>
          <div className="value">
            {links.filter((l) => l.status === "DISABLED").length}
          </div>
        </div>
      </div>
      <div className="card">
        <h2 style={{ marginTop: 0 }}>Recent links</h2>
        <table className="table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Destination</th>
              <th>Clicks</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {links.slice(0, 8).map((l) => (
              <tr key={l.id}>
                <td className="mono">{l.shortCode}</td>
                <td>{l.originalUrl}</td>
                <td className="mono">{l.totalClicks}</td>
                <td>{l.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
