-- =====================================================================
-- V15  Projects, Cost Codes, and project-aware assignments
--   project    : operational unit where employees work (FTDD Vol.1 Ch.2.7)
--   cost_code  : a charge code belonging to a project (cost segregation)
--   assignment : gains project_id + cost_code_id (deploy employee to project)
--   + PAY_STATUS lookup (DAILY / MONTHLY / HOURLY)
-- Company-scoped (multi-tenant).
-- =====================================================================

CREATE TABLE project (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID         NOT NULL,
    code                VARCHAR(40)  NOT NULL,
    name                VARCHAR(200) NOT NULL,
    manager_employee_id UUID         REFERENCES employee (id),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(100),
    version             BIGINT       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_project_company_code ON project (company_id, code);

CREATE TABLE cost_code (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID         NOT NULL,
    project_id  UUID         NOT NULL REFERENCES project (id),
    code        VARCHAR(40)  NOT NULL,
    name        VARCHAR(200) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(100),
    updated_at  TIMESTAMPTZ,
    updated_by  VARCHAR(100),
    version     BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX ix_cost_code_project ON cost_code (project_id);
CREATE UNIQUE INDEX uq_cost_code_project_code ON cost_code (project_id, code);

-- Deploy an employee to a project (and a default cost code) via the assignment.
ALTER TABLE assignment ADD COLUMN project_id   UUID REFERENCES project (id);
ALTER TABLE assignment ADD COLUMN cost_code_id UUID REFERENCES cost_code (id);

-- Pay status / employment type as configurable lookup.
INSERT INTO lookup_value (company_id, category, code, label, sort_order) VALUES
    (NULL, 'PAY_STATUS', 'MONTHLY', 'Monthly', 1),
    (NULL, 'PAY_STATUS', 'DAILY',   'Daily',   2),
    (NULL, 'PAY_STATUS', 'HOURLY',  'Hourly',  3);
