-- =====================================================================
-- V3  Employee, Contract, Assignment
-- FTDD Vol.1 Ch.2 - one employee may have multiple assignments over a career;
-- historical assignments are mandatory (effective-dated).
-- =====================================================================

CREATE TABLE employee (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id                UUID         NOT NULL,
    employee_number           VARCHAR(50)  NOT NULL,
    first_name                VARCHAR(100) NOT NULL,
    last_name                 VARCHAR(100) NOT NULL,
    nationality_country_code  VARCHAR(2),
    date_of_birth             DATE,
    gender                    VARCHAR(10),
    hire_date                 DATE         NOT NULL,
    termination_date          DATE,
    email                     VARCHAR(150),
    phone                     VARCHAR(30),
    status                    VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by                VARCHAR(100),
    updated_at                TIMESTAMPTZ,
    updated_by                VARCHAR(100),
    version                   BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_employee_number UNIQUE (company_id, employee_number)
);

CREATE INDEX ix_employee_company ON employee (company_id);
CREATE INDEX ix_employee_status  ON employee (company_id, status);

CREATE TABLE contract (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id         UUID         NOT NULL REFERENCES employee (id),
    contract_number     VARCHAR(50),
    contract_type       VARCHAR(30)  NOT NULL,   -- PERMANENT, FIXED_TERM, PART_TIME, etc.
    effective_from      DATE         NOT NULL,
    effective_to        DATE,
    base_currency_code  VARCHAR(3),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(100),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_contract_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE INDEX ix_contract_employee ON contract (employee_id);

CREATE TABLE assignment (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id              UUID         NOT NULL REFERENCES employee (id),
    organization_unit_id     UUID         NOT NULL REFERENCES organization_unit (id),
    position_title           VARCHAR(150),
    supervisor_employee_id   UUID         REFERENCES employee (id),
    primary_assignment       BOOLEAN      NOT NULL DEFAULT TRUE,
    effective_from           DATE         NOT NULL,
    effective_to             DATE,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by               VARCHAR(100),
    updated_at               TIMESTAMPTZ,
    updated_by               VARCHAR(100),
    version                  BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_assignment_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE INDEX ix_assignment_employee ON assignment (employee_id);
CREATE INDEX ix_assignment_org_unit ON assignment (organization_unit_id);
