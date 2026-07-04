-- =====================================================================
-- V37  Per-project payroll structure.
--   A payroll_rule can now belong to a specific project. project_id NULL
--   means the company-wide default. The engine picks the project's rule
--   first and falls back to the default when the project has none.
--   SAFE: only ADD COLUMN (nullable); existing rules become the default.
-- =====================================================================
ALTER TABLE payroll_rule ADD COLUMN project_id UUID;

-- Existing unique constraint (company + pay_group) must now allow one rule
-- per (company, project, pay_group). Drop the old one if present, add new.
DROP INDEX IF EXISTS uq_payroll_rule_company_paygroup;
CREATE UNIQUE INDEX IF NOT EXISTS uq_payroll_rule_company_project_paygroup
    ON payroll_rule (company_id, COALESCE(project_id, '00000000-0000-0000-0000-000000000000'), pay_group);
