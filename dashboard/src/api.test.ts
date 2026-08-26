import { afterEach, describe, expect, it, vi } from "vitest";
import { apiFetch } from "./api";

describe("apiFetch", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("sends bearer token and parses json", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ id: "1", role: "OWNER" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      })
    );
    vi.stubGlobal("fetch", fetchMock);

    const result = await apiFetch<{ id: string; role: string }>(
      "/api/v1/orgs",
      async () => "tok-123"
    );

    expect(result).toEqual({ id: "1", role: "OWNER" });
    expect(fetchMock).toHaveBeenCalledOnce();
    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    const headers = new Headers(init.headers);
    expect(headers.get("Authorization")).toBe("Bearer tok-123");
  });

  it("throws response body on error", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response('{"code":"ACCESS_DENIED"}', { status: 403 })
      )
    );

    await expect(
      apiFetch("/api/v1/orgs", async () => "tok")
    ).rejects.toThrow('{"code":"ACCESS_DENIED"}');
  });
});
