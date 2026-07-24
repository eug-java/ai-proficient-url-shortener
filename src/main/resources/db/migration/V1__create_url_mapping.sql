CREATE TABLE url_mapping (
  id UUID PRIMARY KEY,
  short_code VARCHAR(32) NOT NULL,
  original_url VARCHAR(2048) NOT NULL,
  expires_at TIMESTAMPTZ NULL,
  created_at TIMESTAMPTZ NOT NULL,
  total_clicks BIGINT NOT NULL DEFAULT 0,
  last_accessed_at TIMESTAMPTZ NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_url_mapping_short_code UNIQUE (short_code)
);
CREATE INDEX idx_url_mapping_expires_at ON url_mapping(expires_at) WHERE expires_at IS NOT NULL;
