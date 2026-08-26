import Keycloak from "keycloak-js";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? "http://localhost:8081",
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? "shortener",
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? "shortener-dashboard",
});

type AuthState = {
  ready: boolean;
  authenticated: boolean;
  token?: string;
  email?: string;
  name?: string;
  login: () => void;
  logout: () => void;
  getToken: () => Promise<string | undefined>;
};

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [token, setToken] = useState<string>();

  useEffect(() => {
    let cancelled = false;
    keycloak
      .init({ onLoad: "check-sso", pkceMethod: "S256", checkLoginIframe: false })
      .then((ok) => {
        if (cancelled) return;
        setAuthenticated(ok);
        setToken(keycloak.token);
        setReady(true);
      })
      .catch(() => {
        if (!cancelled) setReady(true);
      });

    const refresh = window.setInterval(() => {
      keycloak
        .updateToken(30)
        .then((refreshed) => {
          if (refreshed) setToken(keycloak.token);
        })
        .catch(() => undefined);
    }, 20000);

    return () => {
      cancelled = true;
      window.clearInterval(refresh);
    };
  }, []);

  const login = useCallback(() => {
    void keycloak.login();
  }, []);

  const logout = useCallback(() => {
    void keycloak.logout({ redirectUri: window.location.origin });
  }, []);

  const getToken = useCallback(async () => {
    try {
      await keycloak.updateToken(30);
      setToken(keycloak.token);
      return keycloak.token;
    } catch {
      return keycloak.token;
    }
  }, []);

  const value = useMemo<AuthState>(
    () => ({
      ready,
      authenticated,
      token,
      email: keycloak.tokenParsed?.email as string | undefined,
      name:
        (keycloak.tokenParsed?.name as string | undefined) ??
        (keycloak.tokenParsed?.preferred_username as string | undefined),
      login,
      logout,
      getToken,
    }),
    [ready, authenticated, token, login, logout, getToken]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("AuthProvider missing");
  return ctx;
}
