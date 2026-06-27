-- =====================================================================
-- V12  Raw legacy snapshot landing table
--   Stores the FULL legacy row for each employee exactly as exported from
--   the old FoxPro/DBF system — header (payresulth) and all detail lines
--   (payresultd) — as JSONB. This guarantees EVERY legacy column has a place
--   with us, even the ones that are empty in the current snapshot but may be
--   populated in a future export.
--
--   This is a faithful archive, NOT business data: the engines read from the
--   normalized tables (employee, contract, ...). This table exists so nothing
--   from the legacy file is ever lost and so the admin UI can show every field.
--
--   Idempotent: one row per (company_id, employee_number); re-importing a
--   fresh snapshot overwrites it.
-- =====================================================================

CREATE TABLE legacy_employee_raw (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id       UUID NOT NULL,
    employee_id      UUID REFERENCES employee(id) ON DELETE SET NULL,
    employee_number  VARCHAR(50) NOT NULL,   -- legacy BADGE_CD
    source           VARCHAR(50),            -- e.g. "payresulth/payresultd"
    header_json      JSONB,                  -- full header row, all columns
    detail_json      JSONB,                  -- array of all detail (pay) lines
    imported_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by       VARCHAR(100),
    updated_at       TIMESTAMPTZ,
    updated_by       VARCHAR(100),
    version          BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_legacy_employee_raw_company_number
    ON legacy_employee_raw (company_id, employee_number);

CREATE INDEX ix_legacy_employee_raw_employee
    ON legacy_employee_raw (employee_id);
