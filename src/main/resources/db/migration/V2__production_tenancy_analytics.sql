-- Production tenancy, lifecycle, analytics pipeline foundation

CREATE TABLE organization (
  id UUID PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  slug VARCHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_organization_slug UNIQUE (slug)
);

CREATE TABLE organization_member (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
  user_sub VARCHAR(128) NOT NULL,
  email VARCHAR(320) NULL,
  display_name VARCHAR(160) NULL,
  role VARCHAR(16) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_org_member UNIQUE (organization_id, user_sub),
  CONSTRAINT ck_org_member_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER'))
);
CREATE INDEX idx_org_member_user ON organization_member(user_sub);

ALTER TABLE url_mapping
  ADD COLUMN organization_id UUID NULL REFERENCES organization(id),
  ADD COLUMN created_by_sub VARCHAR(128) NULL,
  ADD COLUMN title VARCHAR(200) NULL,
  ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  ADD COLUMN updated_at TIMESTAMPTZ NULL,
  ADD COLUMN disabled_at TIMESTAMPTZ NULL;

ALTER TABLE url_mapping
  ADD CONSTRAINT ck_url_mapping_status CHECK (status IN ('ACTIVE', 'DISABLED', 'DELETED'));

CREATE INDEX idx_url_mapping_org ON url_mapping(organization_id) WHERE organization_id IS NOT NULL;
CREATE INDEX idx_url_mapping_status ON url_mapping(status);

CREATE TABLE outbox_event (
  id UUID PRIMARY KEY,
  aggregate_type VARCHAR(64) NOT NULL,
  aggregate_id VARCHAR(64) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  event_id UUID NOT NULL,
  payload TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  published_at TIMESTAMPTZ NULL,
  CONSTRAINT uk_outbox_event_id UNIQUE (event_id)
);
CREATE INDEX idx_outbox_unpublished ON outbox_event(created_at) WHERE published_at IS NULL;

CREATE TABLE consumer_inbox (
  event_id UUID PRIMARY KEY,
  consumer_name VARCHAR(64) NOT NULL,
  processed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE click_event (
  id UUID PRIMARY KEY,
  event_id UUID NOT NULL,
  organization_id UUID NOT NULL REFERENCES organization(id),
  url_mapping_id UUID NOT NULL REFERENCES url_mapping(id),
  short_code VARCHAR(32) NOT NULL,
  occurred_at TIMESTAMPTZ NOT NULL,
  ip_hash VARCHAR(64) NULL,
  user_agent VARCHAR(512) NULL,
  referrer VARCHAR(2048) NULL,
  country_code CHAR(2) NULL,
  device_type VARCHAR(32) NULL,
  browser VARCHAR(64) NULL,
  os VARCHAR(64) NULL,
  CONSTRAINT uk_click_event_id UNIQUE (event_id)
);
CREATE INDEX idx_click_event_org_time ON click_event(organization_id, occurred_at DESC);
CREATE INDEX idx_click_event_code_time ON click_event(short_code, occurred_at DESC);

CREATE TABLE link_stats_total (
  url_mapping_id UUID PRIMARY KEY REFERENCES url_mapping(id) ON DELETE CASCADE,
  organization_id UUID NOT NULL REFERENCES organization(id),
  short_code VARCHAR(32) NOT NULL,
  total_clicks BIGINT NOT NULL DEFAULT 0,
  last_clicked_at TIMESTAMPTZ NULL,
  unique_ip_hashes BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE link_stats_daily (
  url_mapping_id UUID NOT NULL REFERENCES url_mapping(id) ON DELETE CASCADE,
  day DATE NOT NULL,
  organization_id UUID NOT NULL REFERENCES organization(id),
  short_code VARCHAR(32) NOT NULL,
  clicks BIGINT NOT NULL DEFAULT 0,
  unique_ip_hashes BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (url_mapping_id, day)
);

CREATE TABLE link_dimension_daily (
  url_mapping_id UUID NOT NULL REFERENCES url_mapping(id) ON DELETE CASCADE,
  day DATE NOT NULL,
  dimension VARCHAR(32) NOT NULL,
  dimension_value VARCHAR(128) NOT NULL,
  clicks BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (url_mapping_id, day, dimension, dimension_value)
);
