import { useEffect, useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { type AnalyticsSummary, type LinkItem, useApi } from "../api";
import { useAuth } from "../auth";
import { useOrg } from "../org";

type TimeseriesPoint = { day: string; clicks: number };
type BreakdownRow = { value: string; clicks: number };

export function AnalyticsPage() {
  const api = useApi();
  const { getToken } = useAuth();
  const { orgId } = useOrg();
  const [links, setLinks] = useState<LinkItem[]>([]);
  const [code, setCode] = useState("");
  const [summary, setSummary] = useState<AnalyticsSummary>();
  const [series, setSeries] = useState<TimeseriesPoint[]>([]);
  const [referrers, setReferrers] = useState<BreakdownRow[]>([]);
  const [countries, setCountries] = useState<BreakdownRow[]>([]);
  const [error, setError] = useState<string>();

  useEffect(() => {
    if (!orgId) return;
    void api
      .get<LinkItem[]>(`/api/v1/orgs/${orgId}/urls`)
      .then((list) => {
        setLinks(list);
        setCode((c) => c || list[0]?.shortCode || "");
      })
      .catch((e) => setError(e instanceof Error ? e.message : "Failed"));
  }, [api, orgId]);

  useEffect(() => {
    if (!orgId || !code) return;
    setError(undefined);
    void (async () => {
      try {
        const [s, t, r, c] = await Promise.all([
          api.get<AnalyticsSummary>(
            `/api/v1/orgs/${orgId}/urls/${code}/analytics`
          ),
          api.get<TimeseriesPoint[]>(
            `/api/v1/orgs/${orgId}/urls/${code}/analytics/timeseries?days=30`
          ),
          api.get<BreakdownRow[]>(
            `/api/v1/orgs/${orgId}/urls/${code}/analytics/breakdowns/referrer?days=30`
          ),
          api.get<BreakdownRow[]>(
            `/api/v1/orgs/${orgId}/urls/${code}/analytics/breakdowns/country?days=30`
          ),
        ]);
        setSummary(s);
        setSeries(t);
        setReferrers(r);
        setCountries(c);
      } catch (e) {
        setError(e instanceof Error ? e.message : "Analytics failed");
      }
    })();
  }, [api, orgId, code]);

  const exportCsv = async () => {
    if (!orgId || !code) return;
    try {
      const token = await getToken();
      const base = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
      const res = await fetch(
        `${base}/api/v1/orgs/${orgId}/urls/${code}/analytics/export.csv?days=30`,
        {
          headers: token ? { Authorization: `Bearer ${token}` } : {},
        }
      );
      if (!res.ok) {
        throw new Error(await res.text());
      }
      const blob = await res.blob();
      const href = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = href;
      anchor.download = `${code}-analytics.csv`;
      anchor.click();
      URL.revokeObjectURL(href);
    } catch (e) {
      setError(e instanceof Error ? e.message : "CSV export failed");
    }
  };

  return (
    <div>
      <h1 className="page-title">Analytics</h1>
      <p className="page-sub">
        Time series, referrers, geo, and export for a selected link.
      </p>
      <div className="row" style={{ marginBottom: 16 }}>
        <select value={code} onChange={(e) => setCode(e.target.value)}>
          {links.map((l) => (
            <option key={l.id} value={l.shortCode}>
              {l.shortCode}
            </option>
          ))}
        </select>
        <button className="btn secondary" type="button" onClick={exportCsv}>
          Export CSV
        </button>
      </div>
      {error ? <p className="error">{error}</p> : null}
      <div className="stats">
        <div className="stat">
          <div className="label">Total clicks</div>
          <div className="value">{summary?.totalClicks ?? 0}</div>
        </div>
        <div className="stat">
          <div className="label">Approx. unique</div>
          <div className="value">{summary?.uniqueVisitorsApprox ?? 0}</div>
        </div>
        <div className="stat">
          <div className="label">Last click</div>
          <div className="value" style={{ fontSize: "1rem" }}>
            {summary?.lastClickedAt
              ? new Date(summary.lastClickedAt).toLocaleString()
              : "—"}
          </div>
        </div>
        <div className="stat">
          <div className="label">Code</div>
          <div className="value" style={{ fontSize: "1rem" }}>
            {code || "—"}
          </div>
        </div>
      </div>
      <div className="grid-2">
        <div className="card" style={{ height: 320 }}>
          <h2 style={{ marginTop: 0 }}>Clicks / day</h2>
          <ResponsiveContainer width="100%" height="85%">
            <LineChart data={series}>
              <CartesianGrid stroke="#2e3a48" />
              <XAxis dataKey="day" stroke="#8b9aab" />
              <YAxis stroke="#8b9aab" />
              <Tooltip />
              <Line type="monotone" dataKey="clicks" stroke="#3d9cf0" />
            </LineChart>
          </ResponsiveContainer>
        </div>
        <div className="card" style={{ height: 320 }}>
          <h2 style={{ marginTop: 0 }}>Top countries</h2>
          <ResponsiveContainer width="100%" height="85%">
            <BarChart data={countries.slice(0, 8)}>
              <CartesianGrid stroke="#2e3a48" />
              <XAxis dataKey="value" stroke="#8b9aab" />
              <YAxis stroke="#8b9aab" />
              <Tooltip />
              <Bar dataKey="clicks" fill="#3ecf8e" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
      <div className="card" style={{ marginTop: 16 }}>
        <h2 style={{ marginTop: 0 }}>Top referrers</h2>
        <table className="table">
          <thead>
            <tr>
              <th>Referrer</th>
              <th>Clicks</th>
            </tr>
          </thead>
          <tbody>
            {referrers.map((r) => (
              <tr key={r.value}>
                <td>{r.value || "(direct)"}</td>
                <td className="mono">{r.clicks}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
