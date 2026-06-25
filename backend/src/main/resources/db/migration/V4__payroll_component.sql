-- =====================================================================
-- V4  Payroll Component (master data)
-- FTDD Vol.1 Ch.6 - salary is a collection of components; nothing is special.
-- The calculation engine (formula resolution) arrives in Phase 3 (Rule Engine).
-- =====================================================================

CREATE TABLE payroll_component (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id                UUID         NOT NULL,
    code                      VARCHAR(50)  NOT NULL,
    name                      VARCHAR(150) NOT NULL,
    category                  VARCHAR(30)  NOT NULL,   -- SALARY, ALLOWANCE, DEDUCTION, PROVISION, BONUS, OVERTIME, LEAVE, LOAN, GOVERNMENT, TAX, INSURANCE
    component_type            VARCHAR(20)  NOT NULL,   -- EARNING | DEDUCTION
    payment_frequency         VARCHAR(20)  NOT NULL DEFAULT 'MONTHLY',
    calculation_method        VARCHAR(20)  NOT NULL DEFAULT 'FIXED',  -- FIXED | PERCENTAGE | FORMULA (formula -> Rule Engine)
    rounding_method           VARCHAR(20)  NOT NULL DEFAULT 'HALF_UP',
    rounding_scale            INT          NOT NULL DEFAULT 2,
    currency_code             VARCHAR(3),
    priority                  INT          NOT NULL DEFAULT 100,   -- evaluation / deduction priority
    taxable                   BOOLEAN      NOT NULL DEFAULT FALSE,
    insurable                 BOOLEAN      NOT NULL DEFAULT FALSE,
    wps_included              BOOLEAN      NOT NULL DEFAULT TRUE,
    eos_included              BOOLEAN      NOT NULL DEFAULT FALSE,
    provision_included        BOOLEAN      NOT NULL DEFAULT FALSE,
    leave_included            BOOLEAN      NOT NULL DEFAULT FALSE,
    visible_on_payslip        BOOLEAN      NOT NULL DEFAULT TRUE,
    visible_on_reports        BOOLEAN      NOT NULL DEFAULT TRUE,
    cost_allocation_required  BOOLEAN      NOT NULL DEFAULT FALSE,
    approval_required         BOOLEAN      NOT NULL DEFAULT FALSE,
    effective_from            DATE         NOT NULL,
    effective_to              DATE,
    status                    VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    remarks                   VARCHAR(500),
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by                VARCHAR(100),
    updated_at                TIMESTAMPTZ,
    updated_by                VARCHAR(100),
    version                   BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_component_code UNIQUE (company_id, code),
    CONSTRAINT ck_component_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE INDEX ix_component_company  ON payroll_component (company_id);
CREATE INDEX ix_component_category ON payroll_component (company_id, category);
