-- Org quotas and ownership helpers
ALTER TABLE organization
  ADD COLUMN IF NOT EXISTS daily_link_quota INT NOT NULL DEFAULT 100,
  ADD COLUMN IF NOT EXISTS max_links INT NOT NULL DEFAULT 1000;

CREATE TABLE IF NOT EXISTS organization_usage_daily (
  organization_id UUID NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  day DATE NOT NULL,
  links_created INT NOT NULL DEFAULT 0,
  PRIMARY KEY (organization_id, day)
);
