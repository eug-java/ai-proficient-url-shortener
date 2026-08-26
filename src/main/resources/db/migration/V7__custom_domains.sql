-- Custom vanity hostnames per organization
CREATE TABLE org_custom_domain (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  hostname VARCHAR(253) NOT NULL,
  verification_token VARCHAR(64) NOT NULL,
  verified BOOLEAN NOT NULL DEFAULT FALSE,
  verified_at TIMESTAMPTZ NULL,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_org_custom_domain_hostname UNIQUE (hostname)
);

CREATE INDEX idx_org_custom_domain_org ON org_custom_domain (organization_id);
