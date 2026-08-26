import { useEffect, useState, type FormEvent } from "react";
import { useApi } from "../api";
import { useOrg } from "../org";

type Member = {
  id: string;
  userSub: string;
  email?: string;
  displayName?: string;
  role: string;
};

export function MembersPage() {
  const api = useApi();
  const { orgId } = useOrg();
  const [members, setMembers] = useState<Member[]>([]);
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("MEMBER");
  const [error, setError] = useState<string>();

  const reload = async () => {
    if (!orgId) return;
    setMembers(await api.get<Member[]>(`/api/v1/orgs/${orgId}/members`));
  };

  useEffect(() => {
    void reload().catch((e) =>
      setError(e instanceof Error ? e.message : "Failed")
    );
  }, [orgId]);

  const onInvite = async (e: FormEvent) => {
    e.preventDefault();
    if (!orgId) return;
    try {
      await api.post(`/api/v1/orgs/${orgId}/invites`, {
        email,
        role,
      });
      setEmail("");
      await reload();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Invite failed");
    }
  };

  const changeRole = async (memberId: string, next: string) => {
    if (!orgId) return;
    await api.patch(`/api/v1/orgs/${orgId}/members/${memberId}`, { role: next });
    await reload();
  };

  const transfer = async (sub: string) => {
    if (!orgId) return;
    if (!window.confirm(`Transfer ownership to ${sub}?`)) return;
    await api.post(`/api/v1/orgs/${orgId}/transfer-ownership`, {
      newOwnerSub: sub,
    });
    await reload();
  };

  return (
    <div>
      <h1 className="page-title">Members</h1>
      <p className="page-sub">
        Invite by email (Keycloak creates the account). Managing members requires
        OWNER.
      </p>
      {error ? <p className="error">{error}</p> : null}
      <div className="grid-2">
        <div className="card">
          <table className="table">
            <thead>
              <tr>
                <th>User</th>
                <th>Email</th>
                <th>Role</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {members.map((m) => (
                <tr key={m.userSub}>
                  <td className="mono">{m.displayName || m.userSub}</td>
                  <td>{m.email || "—"}</td>
                  <td>
                    <select
                      value={m.role}
                      disabled={m.role === "OWNER"}
                      onChange={(e) => void changeRole(m.id, e.target.value)}
                    >
                      <option value="ADMIN">ADMIN</option>
                      <option value="MEMBER">MEMBER</option>
                      <option value="VIEWER">VIEWER</option>
                      <option value="OWNER">OWNER</option>
                    </select>
                  </td>
                  <td>
                    {m.role !== "OWNER" ? (
                      <button
                        className="btn secondary"
                        type="button"
                        onClick={() => void transfer(m.userSub)}
                      >
                        Make owner
                      </button>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <form className="card" onSubmit={onInvite}>
          <h2 style={{ marginTop: 0 }}>Invite by email</h2>
          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="role">Role</label>
            <select
              id="role"
              value={role}
              onChange={(e) => setRole(e.target.value)}
            >
              <option value="ADMIN">ADMIN</option>
              <option value="MEMBER">MEMBER</option>
              <option value="VIEWER">VIEWER</option>
            </select>
          </div>
          <button className="btn" type="submit">
            Invite
          </button>
        </form>
      </div>
    </div>
  );
}
