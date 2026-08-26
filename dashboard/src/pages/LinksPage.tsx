import { useEffect, useState, type FormEvent } from "react";
import { type LinkItem, useApi } from "../api";
import { useOrg } from "../org";

export function LinksPage() {
  const api = useApi();
  const { orgId } = useOrg();
  const [links, setLinks] = useState<LinkItem[]>([]);
  const [originalUrl, setOriginalUrl] = useState("https://example.com");
  const [customAlias, setCustomAlias] = useState("");
  const [title, setTitle] = useState("");
  const [error, setError] = useState<string>();

  const reload = async () => {
    if (!orgId) return;
    const list = await api.get<LinkItem[]>(`/api/v1/orgs/${orgId}/urls`);
    setLinks(list);
  };

  useEffect(() => {
    void reload().catch((e) =>
      setError(e instanceof Error ? e.message : "Failed to load links")
    );
  }, [orgId]);

  const onCreate = async (e: FormEvent) => {
    e.preventDefault();
    if (!orgId) return;
    setError(undefined);
    try {
      await api.post(`/api/v1/orgs/${orgId}/urls`, {
        originalUrl,
        customAlias: customAlias || null,
        title: title || null,
      });
      setCustomAlias("");
      setTitle("");
      await reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Create failed");
    }
  };

  const disable = async (code: string) => {
    if (!orgId) return;
    await api.post(`/api/v1/orgs/${orgId}/urls/${code}/disable`);
    await reload();
  };

  const remove = async (code: string) => {
    if (!orgId) return;
    if (!window.confirm(`Delete ${code}?`)) return;
    await api.del(`/api/v1/orgs/${orgId}/urls/${code}`);
    await reload();
  };

  const rename = async (code: string, current?: string) => {
    if (!orgId) return;
    const next = window.prompt("New title", current ?? "");
    if (next === null) return;
    const link = links.find((l) => l.shortCode === code);
    await api.patch(`/api/v1/orgs/${orgId}/urls/${code}`, {
      originalUrl: link?.originalUrl,
      title: next,
      expiresAt: link?.expiresAt ?? null,
    });
    await reload();
  };

  return (
    <div>
      <h1 className="page-title">Links</h1>
      <p className="page-sub">Create and manage short links for this org.</p>
      {error ? <p className="error">{error}</p> : null}
      <div className="grid-2">
        <div className="card">
          <h2 style={{ marginTop: 0 }}>All links</h2>
          <table className="table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Title / URL</th>
                <th>Clicks</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {links.map((l) => (
                <tr key={l.id}>
                  <td className="mono">{l.shortCode}</td>
                  <td>
                    <div>{l.title || "—"}</div>
                    <div className="muted">{l.originalUrl}</div>
                  </td>
                  <td className="mono">{l.totalClicks}</td>
                  <td className="row">
                    <button
                      className="btn secondary"
                      type="button"
                      onClick={() => void rename(l.shortCode, l.title)}
                    >
                      Edit
                    </button>
                    {l.status === "ACTIVE" ? (
                      <button
                        className="btn secondary"
                        type="button"
                        onClick={() => void disable(l.shortCode)}
                      >
                        Disable
                      </button>
                    ) : (
                      <span className="muted">{l.status}</span>
                    )}
                    <button
                      className="btn danger"
                      type="button"
                      onClick={() => void remove(l.shortCode)}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <form className="card" onSubmit={onCreate}>
          <h2 style={{ marginTop: 0 }}>Create link</h2>
          <div className="field">
            <label htmlFor="url">Destination URL</label>
            <input
              id="url"
              value={originalUrl}
              onChange={(e) => setOriginalUrl(e.target.value)}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="alias">Custom alias (optional)</label>
            <input
              id="alias"
              value={customAlias}
              onChange={(e) => setCustomAlias(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="title">Title (optional)</label>
            <input
              id="title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </div>
          <button className="btn" type="submit">
            Create
          </button>
        </form>
      </div>
    </div>
  );
}
