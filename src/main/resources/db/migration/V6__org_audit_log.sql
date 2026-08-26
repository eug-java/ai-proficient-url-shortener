-- Immutable org audit trail for admin/member/link actions
CREATE TABLE org_audit_log (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  actor_sub VARCHAR(128) NOT NULL,
  action VARCHAR(64) NOT NULL,
  entity_type VARCHAR(64) NOT NULL,
  entity_id VARCHAR(128) NULL,
  details TEXT NULL,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_org_audit_org_created ON org_audit_log (organization_id, created_at DESC);
