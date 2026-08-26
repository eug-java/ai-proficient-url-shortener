-- Outbound webhooks for cross-service integration
CREATE TABLE org_webhook_endpoint (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  target_url TEXT NOT NULL,
  secret VARCHAR(128) NOT NULL,
  events TEXT NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_org_webhook_org ON org_webhook_endpoint (organization_id);

-- Machine API keys for service-to-service callers (maps to a synthetic user sub)
CREATE TABLE org_api_key (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  name VARCHAR(120) NOT NULL,
  key_prefix VARCHAR(16) NOT NULL,
  key_hash VARCHAR(64) NOT NULL,
  created_by_sub VARCHAR(128) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  last_used_at TIMESTAMPTZ NULL,
  revoked_at TIMESTAMPTZ NULL,
  CONSTRAINT uk_org_api_key_hash UNIQUE (key_hash)
);

CREATE INDEX idx_org_api_key_prefix ON org_api_key (key_prefix);
