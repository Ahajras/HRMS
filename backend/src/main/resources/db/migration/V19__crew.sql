-- =====================================================================
-- V19  Crew / Foreman + Timekeeper scoping  (FTDD Vol.1 Ch.4; legacy PAYREF)
--
--   A crew belongs to a PROJECT and is led by a foreman. Employees are crew
--   members; each member can be on a different shift (so one crew can be split
--   across shifts — e.g. some members morning, some evening).
--
--   A timekeeper (an employee who logs in on site) is linked to one or more
--   PROJECTS via timekeeper_project; later slices enforce that a timekeeper can
--   only enter attendance for employees in their project(s).
-- =====================================================================

CREATE TABLE crew (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID         NOT NULL,
    code                VARCHAR(30)  NOT NULL,
    name                VARCHAR(150) NOT NULL,
    project_id          UUID         REFERENCES project (id),
    foreman_employee_id UUID         REFERENCES employee (id),
    parent_crew_id      UUID         REFERENCES crew (id),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(100),
    version             BIGINT       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_crew_company_code ON crew (company_id, code);
CREATE INDEX ix_crew_project ON crew (company_id, project_id);

-- ---------------------------------------------------------------------
-- crew_member : an employee in a crew, on a given shift, effective-dated.
-- ---------------------------------------------------------------------
CREATE TABLE crew_member (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id     UUID        NOT NULL,
    crew_id        UUID        NOT NULL REFERENCES crew (id) ON DELETE CASCADE,
    employee_id    UUID        NOT NULL REFERENCES employee (id),
    shift_id       UUID        REFERENCES shift (id),
    effective_from DATE        NOT NULL,
    effective_to   DATE,
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     VARCHAR(100),
    updated_at     TIMESTAMPTZ,
    updated_by     VARCHAR(100),
    version        BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ck_crew_member_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);
CREATE INDEX ix_crew_member_crew ON crew_member (crew_id);
CREATE INDEX ix_crew_member_emp ON crew_member (company_id, employee_id);

-- ---------------------------------------------------------------------
-- timekeeper_project : which projects a timekeeper (employee) may work on.
-- ---------------------------------------------------------------------
CREATE TABLE timekeeper_project (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id   UUID        NOT NULL,
    employee_id  UUID        NOT NULL REFERENCES employee (id),
    project_id   UUID        NOT NULL REFERENCES project (id),
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   VARCHAR(100),
    updated_at   TIMESTAMPTZ,
    updated_by   VARCHAR(100),
    version      BIGINT      NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_timekeeper_project ON timekeeper_project (company_id, employee_id, project_id);
CREATE INDEX ix_timekeeper_project_emp ON timekeeper_project (company_id, employee_id);
