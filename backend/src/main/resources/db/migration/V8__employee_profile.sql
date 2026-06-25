-- =====================================================================
-- V8  Employee profile completion
--   - lookup_value : generic configurable code lists (dropdown sources)
--   - bank         : bank reference master data
--   - employee     : extra personal fields (configuration-first stays intact)
--   - employee_document       : 1..* identity documents per employee
--   - employee_bank_account   : 1..* salary/bank accounts per employee (WPS)
-- All dropdown values are DATA, not code (FTDD configuration-first principle).
-- =====================================================================

-- ---------------------------------------------------------------------
-- Generic lookup / code-list table. company_id NULL = global default;
-- a company may later override by inserting a same (category, code) row.
-- ---------------------------------------------------------------------
CREATE TABLE lookup_value (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id   UUID,
    category     VARCHAR(50)  NOT NULL,
    code         VARCHAR(50)  NOT NULL,
    label        VARCHAR(150) NOT NULL,
    sort_order   INT          NOT NULL DEFAULT 0,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   VARCHAR(100),
    updated_at   TIMESTAMPTZ,
    updated_by   VARCHAR(100),
    version      BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX ix_lookup_category ON lookup_value (category);
-- one global row per (category, code); companies override with their own row
CREATE UNIQUE INDEX uq_lookup_global
    ON lookup_value (category, code) WHERE company_id IS NULL;
CREATE UNIQUE INDEX uq_lookup_company
    ON lookup_value (company_id, category, code) WHERE company_id IS NOT NULL;

-- ---------------------------------------------------------------------
-- Bank reference master data (used by employee_bank_account / WPS).
-- ---------------------------------------------------------------------
CREATE TABLE bank (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id    UUID,
    code          VARCHAR(20)  NOT NULL,
    name          VARCHAR(150) NOT NULL,
    swift_code    VARCHAR(20),
    country_code  VARCHAR(2),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    VARCHAR(100),
    updated_at    TIMESTAMPTZ,
    updated_by    VARCHAR(100),
    version       BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_bank_global  ON bank (code) WHERE company_id IS NULL;
CREATE UNIQUE INDEX uq_bank_company ON bank (company_id, code) WHERE company_id IS NOT NULL;

-- ---------------------------------------------------------------------
-- Extra personal fields on employee (single-valued).
-- ---------------------------------------------------------------------
ALTER TABLE employee ADD COLUMN middle_name              VARCHAR(100);
ALTER TABLE employee ADD COLUMN marital_status           VARCHAR(20);
ALTER TABLE employee ADD COLUMN address_line             VARCHAR(255);
ALTER TABLE employee ADD COLUMN city                     VARCHAR(100);
ALTER TABLE employee ADD COLUMN country_of_residence_code VARCHAR(2);

-- ---------------------------------------------------------------------
-- Identity documents (passport, national id, residence id, visa, ...).
-- ---------------------------------------------------------------------
CREATE TABLE employee_document (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id          UUID         NOT NULL REFERENCES employee (id),
    document_type        VARCHAR(30)  NOT NULL,   -- lookup DOCUMENT_TYPE
    document_number      VARCHAR(100) NOT NULL,
    issuing_country_code VARCHAR(2),
    issue_date           DATE,
    expiry_date          DATE,
    issuing_authority    VARCHAR(150),
    status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by           VARCHAR(100),
    updated_at           TIMESTAMPTZ,
    updated_by           VARCHAR(100),
    version              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_document_dates CHECK (expiry_date IS NULL OR issue_date IS NULL OR expiry_date >= issue_date)
);

CREATE INDEX ix_document_employee ON employee_document (employee_id);

-- ---------------------------------------------------------------------
-- Bank accounts (one primary salary account for WPS; others optional).
-- ---------------------------------------------------------------------
CREATE TABLE employee_bank_account (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id          UUID         NOT NULL REFERENCES employee (id),
    bank_id              UUID         REFERENCES bank (id),
    account_holder_name  VARCHAR(150),
    iban                 VARCHAR(34),
    account_number       VARCHAR(50),
    currency_code        VARCHAR(3),
    is_primary           BOOLEAN      NOT NULL DEFAULT TRUE,
    status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by           VARCHAR(100),
    updated_at           TIMESTAMPTZ,
    updated_by           VARCHAR(100),
    version              BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX ix_bank_account_employee ON employee_bank_account (employee_id);

-- =====================================================================
-- Seed global lookup values (overridable per company).
-- =====================================================================
INSERT INTO lookup_value (company_id, category, code, label, sort_order) VALUES
    (NULL, 'GENDER', 'MALE',   'Male',   1),
    (NULL, 'GENDER', 'FEMALE', 'Female', 2),

    (NULL, 'MARITAL_STATUS', 'SINGLE',   'Single',   1),
    (NULL, 'MARITAL_STATUS', 'MARRIED',  'Married',  2),
    (NULL, 'MARITAL_STATUS', 'DIVORCED', 'Divorced', 3),
    (NULL, 'MARITAL_STATUS', 'WIDOWED',  'Widowed',  4),

    (NULL, 'EMPLOYEE_STATUS', 'ACTIVE',     'Active',     1),
    (NULL, 'EMPLOYEE_STATUS', 'ON_LEAVE',   'On Leave',   2),
    (NULL, 'EMPLOYEE_STATUS', 'SUSPENDED',  'Suspended',  3),
    (NULL, 'EMPLOYEE_STATUS', 'TERMINATED', 'Terminated', 4),

    (NULL, 'CONTRACT_TYPE', 'PERMANENT',  'Permanent',   1),
    (NULL, 'CONTRACT_TYPE', 'FIXED_TERM', 'Fixed Term',  2),
    (NULL, 'CONTRACT_TYPE', 'PART_TIME',  'Part Time',   3),
    (NULL, 'CONTRACT_TYPE', 'TEMPORARY',  'Temporary',   4),

    (NULL, 'CONTRACT_STATUS', 'ACTIVE',     'Active',     1),
    (NULL, 'CONTRACT_STATUS', 'EXPIRED',    'Expired',    2),
    (NULL, 'CONTRACT_STATUS', 'TERMINATED', 'Terminated', 3),

    (NULL, 'DOCUMENT_TYPE', 'PASSPORT',        'Passport',         1),
    (NULL, 'DOCUMENT_TYPE', 'NATIONAL_ID',     'National ID',      2),
    (NULL, 'DOCUMENT_TYPE', 'RESIDENCE_ID',    'Residence ID/QID', 3),
    (NULL, 'DOCUMENT_TYPE', 'VISA',            'Visa',             4),
    (NULL, 'DOCUMENT_TYPE', 'LABOR_CARD',      'Labour Card',      5),
    (NULL, 'DOCUMENT_TYPE', 'DRIVING_LICENSE', 'Driving License',  6),
    (NULL, 'DOCUMENT_TYPE', 'HEALTH_CARD',     'Health Card',      7);

-- =====================================================================
-- Seed global banks (Qatar majors + a few GCC; SWIFT where known).
-- =====================================================================
INSERT INTO bank (company_id, code, name, swift_code, country_code) VALUES
    (NULL, 'QNB',     'Qatar National Bank',        'QNBAQAQA', 'QA'),
    (NULL, 'CBQ',     'Commercial Bank of Qatar',   'CBQAQAQA', 'QA'),
    (NULL, 'DOHA',    'Doha Bank',                  'DOHBQAQA', 'QA'),
    (NULL, 'QIB',     'Qatar Islamic Bank',         'QISBQAQA', 'QA'),
    (NULL, 'QIIB',    'Qatar International Islamic Bank', 'QIIBQAQA', 'QA'),
    (NULL, 'DUKHAN',  'Dukhan Bank',                'BRWAQAQA', 'QA'),
    (NULL, 'ALRAYAN', 'Masraf Al Rayan',            'MAFRQAQA', 'QA'),
    (NULL, 'AHLI',    'Ahli Bank Qatar',            'ABQQQAQA', 'QA'),
    (NULL, 'IBQ',     'International Bank of Qatar', NULL,       'QA');
