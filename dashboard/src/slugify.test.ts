import { describe, expect, it } from "vitest";
import { slugify } from "./slugify";

describe("slugify", () => {
  it("normalizes org names", () => {
    expect(slugify("My Workspace!")).toBe("my-workspace");
  });
});
