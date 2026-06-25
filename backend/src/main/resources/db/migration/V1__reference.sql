-- =====================================================================
-- V1  Reference data: countries, currencies, calendars
-- FTDD Vol.1 Ch.2 (Foundation), Vol.2 Ch.26 (currency precision)
-- =====================================================================

CREATE TABLE currency (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code          VARCHAR(3)   NOT NULL,        -- ISO 4217
    name          VARCHAR(100) NOT NULL,
    symbol        VARCHAR(8),
    minor_units   INT          NOT NULL DEFAULT 2,  -- decimal scale (FTDD Vol.2 Ch.24 currency precision)
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    VARCHAR(100),
    updated_at    TIMESTAMPTZ,
    updated_by    VARCHAR(100),
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_currency_code UNIQUE (code)
);

CREATE TABLE country (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                   VARCHAR(2)   NOT NULL,    -- ISO alpha-2
    name                   VARCHAR(100) NOT NULL,
    default_currency_code  VARCHAR(3),
    status                 VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by             VARCHAR(100),
    updated_at             TIMESTAMPTZ,
    updated_by             VARCHAR(100),
    version                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_country_code UNIQUE (code)
);

-- Calendar is generated yearly and defines payroll periods (FTDD Vol.1 Ch.2 Calendar Domain).
CREATE TABLE calendar (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID,                                  -- null = global calendar
    year        INT          NOT NULL,
    name        VARCHAR(100) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(100),
    updated_at  TIMESTAMPTZ,
    updated_by  VARCHAR(100),
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_calendar_company_year UNIQUE (company_id, year)
);

CREATE TABLE calendar_day (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    calendar_id     UUID        NOT NULL REFERENCES calendar (id),
    day_date        DATE        NOT NULL,
    day_name        VARCHAR(20),
    public_holiday  BOOLEAN     NOT NULL DEFAULT FALSE,
    special_holiday BOOLEAN     NOT NULL DEFAULT FALSE,
    working_day     BOOLEAN     NOT NULL DEFAULT TRUE,
    version         BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_calendar_day UNIQUE (calendar_id, day_date)
);

CREATE INDEX ix_calendar_day_calendar ON calendar_day (calendar_id);
