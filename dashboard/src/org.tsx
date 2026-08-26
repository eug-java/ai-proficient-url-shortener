import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { type Org, useApi } from "./api";
import { pickDefaultOrgId } from "./orgHelpers";
import { slugify } from "./slugify";

type OrgState = {
  orgs: Org[];
  orgId?: string;
  setOrgId: (id: string) => void;
  loading: boolean;
  error?: string;
  refresh: () => Promise<void>;
  createOrg: (name: string) => Promise<void>;
};

const OrgContext = createContext<OrgState | null>(null);

export function OrgProvider({ children }: { children: ReactNode }) {
  const api = useApi();
  const [orgs, setOrgs] = useState<Org[]>([]);
  const [orgId, setOrgId] = useState<string>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(undefined);
    try {
      const list = await api.get<Org[]>("/api/v1/orgs");
      setOrgs(list);
      setOrgId((current) => pickDefaultOrgId(list, current));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load orgs");
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const createOrg = useCallback(
    async (name: string) => {
      const created = await api.post<Org>("/api/v1/orgs", {
        name,
        slug: slugify(name) || `org-${Date.now()}`,
      });
      await refresh();
      setOrgId(created.id);
    },
    [api, refresh]
  );

  const value = useMemo(
    () => ({ orgs, orgId, setOrgId, loading, error, refresh, createOrg }),
    [orgs, orgId, loading, error, refresh, createOrg]
  );

  return <OrgContext.Provider value={value}>{children}</OrgContext.Provider>;
}

export function useOrg() {
  const ctx = useContext(OrgContext);
  if (!ctx) throw new Error("OrgProvider missing");
  return ctx;
}
