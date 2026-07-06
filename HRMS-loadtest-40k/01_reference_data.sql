-- =====================================================================
-- LOAD TEST DATA GENERATOR — PART 1: Reference data
-- Company: 00000000-0000-0000-0000-0000000000c1 (the seeded company)
-- Safe & idempotent: every insert here checks "if not exists" first.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) One organization unit to hang test employees off (org chart accuracy
--    doesn't matter for a load test — we just need a valid, existing unit).
-- ---------------------------------------------------------------------
INSERT INTO org_unit_type (company_id, code, name, level_order, mandatory)
SELECT '00000000-0000-0000-0000-0000000000c1', 'LT-TYPE', 'Load Test Unit Type', 1, false
WHERE NOT EXISTS (
    SELECT 1 FROM org_unit_type WHERE company_id = '00000000-0000-0000-0000-0000000000c1' AND code = 'LT-TYPE'
);

INSERT INTO organization_unit (company_id, type_id, code, name, effective_from)
SELECT '00000000-0000-0000-0000-0000000000c1',
       (SELECT id FROM org_unit_type WHERE company_id = '00000000-0000-0000-0000-0000000000c1' AND code = 'LT-TYPE'),
       'LT-ORG', 'Load Test Org Unit', DATE '2020-01-01'
WHERE NOT EXISTS (
    SELECT 1 FROM organization_unit WHERE company_id = '00000000-0000-0000-0000-0000000000c1' AND code = 'LT-ORG'
);

-- ---------------------------------------------------------------------
-- 2) Five test projects, each with a different pay structure — this is
--    exactly the "per-project payroll" feature we built, under real load.
-- ---------------------------------------------------------------------
INSERT INTO project (company_id, code, name)
SELECT '00000000-0000-0000-0000-0000000000c1', v.code, v.name
FROM (VALUES
    ('LT-P1', 'Load Test Project 1 — Fixed 30-day divisor'),
    ('LT-P2', 'Load Test Project 2 — Actual month days'),
    ('LT-P3', 'Load Test Project 3 — Fixed 26-day divisor'),
    ('LT-P4', 'Load Test Project 4 — Default company rule'),
    ('LT-P5', 'Load Test Project 5 — Default company rule')
) AS v(code, name)
WHERE NOT EXISTS (
    SELECT 1 FROM project p WHERE p.company_id = '00000000-0000-0000-0000-0000000000c1' AND p.code = v.code
);

-- ---------------------------------------------------------------------
-- 3) Payroll rules: MONTHLY rule per project (varying divisor behaviour),
--    plus one company-wide default for MONTHLY and DAILY as a fallback
--    (this is what LT-P4 / LT-P5 employees will actually use).
-- ---------------------------------------------------------------------
INSERT INTO payroll_rule (company_id, project_id, pay_group, pay_item_basis, month_divisor, divisor_mode)
SELECT '00000000-0000-0000-0000-0000000000c1', p.id, 'MONTHLY', 'FIXED_AMOUNT', r.divisor, r.mode
FROM project p
JOIN (VALUES ('LT-P1', 30.00, 'FIXED'), ('LT-P2', 30.00, 'ACTUAL_MONTH'), ('LT-P3', 26.00, 'FIXED')) AS r(code, divisor, mode)
  ON r.code = p.code
WHERE p.company_id = '00000000-0000-0000-0000-0000000000c1'
  AND NOT EXISTS (
      SELECT 1 FROM payroll_rule pr WHERE pr.project_id = p.id AND pr.pay_group = 'MONTHLY' AND pr.status = 'ACTIVE'
  );

INSERT INTO payroll_rule (company_id, project_id, pay_group, pay_item_basis, month_divisor, divisor_mode)
SELECT '00000000-0000-0000-0000-0000000000c1', NULL, 'MONTHLY', 'FIXED_AMOUNT', 30.00, 'FIXED'
WHERE NOT EXISTS (
    SELECT 1 FROM payroll_rule WHERE company_id = '00000000-0000-0000-0000-0000000000c1'
      AND project_id IS NULL AND pay_group = 'MONTHLY' AND status = 'ACTIVE'
);

INSERT INTO payroll_rule (company_id, project_id, pay_group, pay_item_basis, month_divisor, divisor_mode)
SELECT '00000000-0000-0000-0000-0000000000c1', NULL, 'DAILY', 'DAILY_RATE', 30.00, 'FIXED'
WHERE NOT EXISTS (
    SELECT 1 FROM payroll_rule WHERE company_id = '00000000-0000-0000-0000-0000000000c1'
      AND project_id IS NULL AND pay_group = 'DAILY' AND status = 'ACTIVE'
);

-- ---------------------------------------------------------------------
-- 4) Payroll components: reuse existing ones by name if present (real
--    server already has these); create a Load-Test-tagged copy only if
--    genuinely missing (e.g. on a clean test database).
-- ---------------------------------------------------------------------
DO $$
DECLARE
  v_company UUID := '00000000-0000-0000-0000-0000000000c1';
  v_needed  TEXT[][] := ARRAY[
    ARRAY['BASE SALARY',      'SALARY',    'LT-BASE'],
    ARRAY['Accommodation',    'ALLOWANCE', 'LT-ACC'],
    ARRAY['Furniture',        'ALLOWANCE', 'LT-FUR'],
    ARRAY['Car II Allowance', 'ALLOWANCE', 'LT-CAR2'],
    ARRAY['Living Allowance', 'ALLOWANCE', 'LT-LIV'],
    ARRAY['Location Allowance','ALLOWANCE','LT-LOC'],
    ARRAY['Messing Subsidy',  'ALLOWANCE', 'LT-MESS'],
    ARRAY['Home Subsidy',     'ALLOWANCE', 'LT-HOME']
  ];
  v_row TEXT[];
BEGIN
  FOREACH v_row SLICE 1 IN ARRAY v_needed LOOP
    IF NOT EXISTS (
      SELECT 1 FROM payroll_component
      WHERE company_id = v_company AND name ILIKE '%' || v_row[1] || '%'
    ) THEN
      INSERT INTO payroll_component (company_id, code, name, category, component_type, effective_from)
      VALUES (v_company, v_row[3], v_row[1], v_row[2], 'EARNING', DATE '2020-01-01');
    END IF;
  END LOOP;
END $$;
