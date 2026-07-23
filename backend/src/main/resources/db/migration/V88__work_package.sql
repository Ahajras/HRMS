CREATE TABLE IF NOT EXISTS work_package (
    id              UUID PRIMARY KEY,
    company_id      UUID         NOT NULL,
    project_id      UUID         NOT NULL REFERENCES project (id),
    cost_code_id    UUID         REFERENCES cost_code (id),
    code            VARCHAR(40)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(500),
    planned_start   DATE,
    planned_end     DATE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
    created_at      TIMESTAMPTZ,
    created_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_work_package_dates CHECK (planned_end IS NULL OR planned_start IS NULL OR planned_end >= planned_start)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_work_package_project_code ON work_package (company_id, project_id, code);
CREATE INDEX IF NOT EXISTS ix_work_package_project ON work_package (company_id, project_id);
CREATE INDEX IF NOT EXISTS ix_work_package_cost ON work_package (company_id, cost_code_id);

CREATE TABLE IF NOT EXISTS work_package_requirement (
    id              UUID PRIMARY KEY,
    company_id      UUID         NOT NULL,
    work_package_id UUID         NOT NULL REFERENCES work_package (id) ON DELETE CASCADE,
    job_title_code  VARCHAR(40)  NOT NULL,
    job_title_name  VARCHAR(150),
    required_count  INT          NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ,
    created_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_work_package_requirement ON work_package_requirement (work_package_id, job_title_code);

CREATE TABLE IF NOT EXISTS work_package_crew (
    id              UUID PRIMARY KEY,
    company_id      UUID         NOT NULL,
    work_package_id UUID         NOT NULL REFERENCES work_package (id) ON DELETE CASCADE,
    crew_id         UUID         NOT NULL REFERENCES crew (id),
    planned_start   DATE,
    planned_end     DATE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ,
    created_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_work_package_crew_dates CHECK (planned_end IS NULL OR planned_start IS NULL OR planned_end >= planned_start)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_work_package_crew ON work_package_crew (work_package_id, crew_id);
CREATE INDEX IF NOT EXISTS ix_work_package_crew_crew ON work_package_crew (company_id, crew_id);
