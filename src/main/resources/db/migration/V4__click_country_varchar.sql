ALTER TABLE click_event
  ALTER COLUMN country_code TYPE VARCHAR(2) USING country_code::text;
