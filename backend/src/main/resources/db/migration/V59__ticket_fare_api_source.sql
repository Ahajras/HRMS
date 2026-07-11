ALTER TABLE ticket_fare ADD COLUMN IF NOT EXISTS source VARCHAR(20) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE ticket_fare ADD COLUMN IF NOT EXISTS provider VARCHAR(50);
ALTER TABLE ticket_fare ADD COLUMN IF NOT EXISTS provider_offer_id VARCHAR(120);
ALTER TABLE ticket_fare ADD COLUMN IF NOT EXISTS fetched_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS ix_ticket_fare_source ON ticket_fare(company_id, source, provider);
