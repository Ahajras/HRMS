ALTER TABLE payroll_period_project
    ADD COLUMN IF NOT EXISTS pay_group VARCHAR(20) NOT NULL DEFAULT 'ALL';

DROP INDEX IF EXISTS uq_period_project;
CREATE UNIQUE INDEX IF NOT EXISTS uq_period_project_group
    ON payroll_period_project (period_id, project_id, pay_group);
