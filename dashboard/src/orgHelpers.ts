export function pickDefaultOrgId(
  orgs: { id: string }[],
  current?: string
): string | undefined {
  if (current && orgs.some((o) => o.id === current)) return current;
  return orgs[0]?.id;
}

export function authDisplayName(input: {
  name?: string;
  preferredUsername?: string;
  email?: string;
}): string {
  return input.name || input.preferredUsername || input.email || "Signed in";
}
