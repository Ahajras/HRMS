-- V37 introduced project-specific payroll rules, but older databases still
-- have the original company/pay_group active-rule index from V30.
DROP INDEX IF EXISTS uq_payroll_rule_group;
DROP INDEX IF EXISTS uq_payroll_rule_company_paygroup;
DROP INDEX IF EXISTS uq_payroll_rule_company_project_paygroup;

CREATE UNIQUE INDEX uq_payroll_rule_company_project_paygroup
    ON payroll_rule (company_id, COALESCE(project_id, '00000000-0000-0000-0000-000000000000'), pay_group)
    WHERE status = 'ACTIVE';
