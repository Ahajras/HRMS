-- =====================================================================
-- V2  Organization: configurable hierarchy tree
-- FTDD Vol.2 Ch.32.7 - the org is a configurable tree of unit *types*,
-- not a fixed set of named levels. Resolves review finding X-09.
-- =====================================================================

-- Defines the levels an organisation uses (Company, Business Unit, Division,
-- Department, Section, Team, ...). Optional levels need not be instantiated.
CREATE TABLE org_unit_type (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id   UUID,                                 -- null = global default level set
    code         VARCHAR(50)  NOT NULL,                -- e.g. COMPANY, BUSINESS_UNIT, DEPARTMENT
    name         VARCHAR(100) NOT NULL,
    level_order  INT          NOT NULL,                -- 1 = Company (root) ... larger = deeper
    mandatory    BOOLEAN      NOT NULL DEFAULT FALSE,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   VARCHAR(100),
    updated_at   TIMESTAMPTZ,
    updated_by   VARCHAR(100),
    version      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_org_unit_type_code UNIQUE (company_id, code)
);

-- The actual organisation tree. parent_id forms the hierarchy; type_id says which level.
CREATE TABLE organization_unit (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID         NOT NULL,
    parent_id       UUID         REFERENCES organization_unit (id),
    type_id         UUID         NOT NULL REFERENCES org_unit_type (id),
    code            VARCHAR(50)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    effective_from  DATE         NOT NULL,
    effective_to    DATE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_org_unit_code UNIQUE (company_id, code),
    CONSTRAINT ck_org_unit_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE INDEX ix_org_unit_company ON organization_unit (company_id);
CREATE INDEX ix_org_unit_parent  ON organization_unit (parent_id);
CREATE INDEX ix_org_unit_type    ON organization_unit (type_id);
