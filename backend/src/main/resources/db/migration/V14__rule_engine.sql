-- =====================================================================
-- V14  Rule Engine (slice 1): country-law rule packages
--   rule_package : a country's labour law (or company policy) as data
--   rule         : a single configurable, effective-dated, versioned value
--   company_rule_package : the package each company operates under
--   No business logic is hardcoded — engines read values via the resolver.
--   Seeded Qatar values are EDITABLE DEFAULTS; confirm against current law.
-- =====================================================================

CREATE TABLE rule_package (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id    UUID,
    code          VARCHAR(30)  NOT NULL,
    name          VARCHAR(150) NOT NULL,
    country_code  VARCHAR(2),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    VARCHAR(100),
    updated_at    TIMESTAMPTZ,
    updated_by    VARCHAR(100),
    version       BIGINT       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_rule_package_global  ON rule_package (code) WHERE company_id IS NULL;
CREATE UNIQUE INDEX uq_rule_package_company ON rule_package (company_id, code) WHERE company_id IS NOT NULL;

CREATE TABLE rule (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id     UUID         NOT NULL REFERENCES rule_package (id),
    company_id     UUID,
    code           VARCHAR(50)  NOT NULL,
    category       VARCHAR(40)  NOT NULL,
    name           VARCHAR(150) NOT NULL,
    value_type     VARCHAR(20)  NOT NULL,
    value_number   NUMERIC(18,4),
    value_text     VARCHAR(255),
    unit           VARCHAR(20),
    effective_from DATE         NOT NULL,
    effective_to   DATE,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    description    VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     VARCHAR(100),
    updated_at     TIMESTAMPTZ,
    updated_by     VARCHAR(100),
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_rule_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);
CREATE INDEX ix_rule_package ON rule (package_id);
CREATE INDEX ix_rule_resolve ON rule (package_id, code, status);

CREATE TABLE company_rule_package (
    company_id   UUID PRIMARY KEY,
    package_code VARCHAR(30) NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by   VARCHAR(100)
);

-- ---------------------------------------------------------------------
-- Seed global default packages (company_id NULL).
-- ---------------------------------------------------------------------
INSERT INTO rule_package (company_id, code, name, country_code) VALUES
    (NULL, 'QATAR', 'Qatar Labour Law', 'QA'),
    (NULL, 'UAE',   'UAE Labour Law',   'AE'),
    (NULL, 'SAUDI', 'Saudi Labour Law', 'SA');

-- ---------------------------------------------------------------------
-- Seed Qatar default rule values (EDITABLE — confirm against current law).
-- ---------------------------------------------------------------------
INSERT INTO rule (package_id, code, category, name, value_type, value_number, unit, effective_from, description)
SELECT p.id, v.code, v.category, v.name, v.value_type, v.value_number, v.unit, DATE '2020-01-01', v.description
FROM rule_package p
JOIN (VALUES
    ('STANDARD_HOURS_PER_DAY',     'WORKING_TIME', 'Standard hours per day',            'NUMBER',  8,   'hours', 'Normal daily working hours'),
    ('STANDARD_DAYS_PER_WEEK',     'WORKING_TIME', 'Standard days per week',            'INTEGER', 6,   'days',  'Normal working days per week'),
    ('STANDARD_HOURS_PER_WEEK',    'WORKING_TIME', 'Standard hours per week',           'NUMBER',  48,  'hours', 'Normal weekly working hours'),
    ('RAMADAN_HOURS_PER_DAY',      'WORKING_TIME', 'Ramadan hours per day',             'NUMBER',  6,   'hours', 'Reduced daily hours during Ramadan'),
    ('ANNUAL_LEAVE_DAYS_UNDER_5Y', 'LEAVE',        'Annual leave (service < 5 years)',  'INTEGER', 21,  'days',  'Paid annual leave for service under 5 years'),
    ('ANNUAL_LEAVE_DAYS_5Y_PLUS',  'LEAVE',        'Annual leave (service >= 5 years)', 'INTEGER', 28,  'days',  'Paid annual leave for service of 5 years or more'),
    ('SICK_LEAVE_FULL_PAY_DAYS',   'LEAVE',        'Sick leave at full pay',            'INTEGER', 14,  'days',  'Sick leave days paid at full wage'),
    ('OT_NORMAL_MULTIPLIER',       'OVERTIME',     'Overtime rate (normal)',            'PERCENT', 125, '%',     'Overtime multiplier on normal days'),
    ('OT_RESTDAY_MULTIPLIER',      'OVERTIME',     'Overtime rate (rest day)',          'PERCENT', 150, '%',     'Overtime multiplier on the weekly rest day'),
    ('OT_HOLIDAY_MULTIPLIER',      'OVERTIME',     'Overtime rate (public holiday)',    'PERCENT', 150, '%',     'Overtime multiplier on public holidays'),
    ('EOS_WEEKS_PER_YEAR',         'EOS',          'End of service weeks per year',     'NUMBER',  3,   'weeks', 'Weeks of basic wage per year of service'),
    ('EOS_MIN_SERVICE_YEARS',      'EOS',          'End of service minimum service',    'NUMBER',  1,   'years', 'Minimum continuous service to qualify for EOS'),
    ('UNPAID_DAY_DIVISOR',         'RATE_BASE',    'Unpaid-day rate divisor',           'INTEGER', 30,  'days',  'Divisor to derive daily rate for unpaid-day deductions'),
    ('OVERTIME_RATE_BASE_DIVISOR', 'RATE_BASE',    'Overtime rate divisor',             'INTEGER', 30,  'days',  'Divisor to derive daily/hourly rate for overtime')
) AS v(code, category, name, value_type, value_number, unit, description) ON TRUE
WHERE p.company_id IS NULL AND p.code = 'QATAR';
