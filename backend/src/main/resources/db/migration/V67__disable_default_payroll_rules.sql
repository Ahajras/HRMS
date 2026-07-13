-- Payroll rules must be project-specific. Older seed data created company-wide
-- default rules with project_id NULL; keep them for audit history but stop the
-- payroll engine and configuration screen from treating them as usable rules.
UPDATE payroll_rule
SET status = 'INACTIVE',
    updated_at = now(),
    updated_by = 'V67__disable_default_payroll_rules'
WHERE project_id IS NULL
  AND status = 'ACTIVE';
