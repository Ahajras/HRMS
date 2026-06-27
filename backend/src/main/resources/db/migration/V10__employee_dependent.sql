-- =====================================================================
-- V10  Employee dependents (المعالين) + supporting lookups
--   - employee_dependent : 1..* family members per employee (legacy
--     HR_PaEmpFamilyDetails / dependants.dbf). Used for family allowances,
--     air-ticket entitlement and EOS/benefit eligibility downstream.
--   - PERSONAL_NO document type : the legacy "Personal Number" carried on
--     every employee record (payresulth.PERSONALNO) is migrated as a document.
--   - RELATIONSHIP lookup : dropdown source for dependent relationship.
-- All dropdown values are DATA, not code (FTDD configuration-first principle).
-- =====================================================================

CREATE TABLE employee_dependent (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id               UUID         NOT NULL REFERENCES employee (id),
    full_name                 VARCHAR(150) NOT NULL,
    relationship              VARCHAR(30),              -- lookup RELATIONSHIP
    gender                    VARCHAR(10),
    date_of_birth             DATE,
    nationality_country_code  VARCHAR(2),
    is_beneficiary            BOOLEAN      NOT NULL DEFAULT FALSE,
    status                    VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by                VARCHAR(100),
    updated_at                TIMESTAMPTZ,
    updated_by                VARCHAR(100),
    version                   BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX ix_dependent_employee ON employee_dependent (employee_id);

-- ---------------------------------------------------------------------
-- Additional global lookups (overridable per company).
-- ---------------------------------------------------------------------
INSERT INTO lookup_value (company_id, category, code, label, sort_order) VALUES
    (NULL, 'RELATIONSHIP', 'SPOUSE',   'Spouse',   1),
    (NULL, 'RELATIONSHIP', 'CHILD',    'Child',    2),
    (NULL, 'RELATIONSHIP', 'PARENT',   'Parent',   3),
    (NULL, 'RELATIONSHIP', 'SIBLING',  'Sibling',  4),
    (NULL, 'RELATIONSHIP', 'OTHER',    'Other',    5),

    -- Legacy "Personal Number" carried on every employee (migrated as a document).
    (NULL, 'DOCUMENT_TYPE', 'PERSONAL_NO', 'Personal Number', 8);
