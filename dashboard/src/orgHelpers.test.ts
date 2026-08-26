import { describe, expect, it } from "vitest";
import { authDisplayName, pickDefaultOrgId } from "./orgHelpers";

describe("orgHelpers", () => {
  it("keeps current org when still present", () => {
    expect(
      pickDefaultOrgId([{ id: "a" }, { id: "b" }], "b")
    ).toBe("b");
  });

  it("falls back to first org", () => {
    expect(pickDefaultOrgId([{ id: "a" }, { id: "b" }], "missing")).toBe("a");
  });
});

describe("authDisplayName", () => {
  it("prefers name then username then email", () => {
    expect(authDisplayName({ name: "Ada", email: "a@x" })).toBe("Ada");
    expect(authDisplayName({ preferredUsername: "ada", email: "a@x" })).toBe(
      "ada"
    );
    expect(authDisplayName({ email: "a@x" })).toBe("a@x");
  });
});
