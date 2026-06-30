-- =====================================================================
-- V25  Crew trades (FTDD Vol.1 Ch.4; legacy crew "Crew Trades" tab):
--      per crew, the required job titles and how many of each are planned.
--      The assigned count is derived from crew members' job titles.
-- =====================================================================
CREATE TABLE crew_trade (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id     UUID         NOT NULL,
    crew_id        UUID         NOT NULL REFERENCES crew (id) ON DELETE CASCADE,
    trade_code     VARCHAR(40)  NOT NULL,
    trade_name     VARCHAR(150),
    planned_count  INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     VARCHAR(100),
    updated_at     TIMESTAMPTZ,
    updated_by     VARCHAR(100),
    version        BIGINT       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_crew_trade ON crew_trade (crew_id, trade_code);
