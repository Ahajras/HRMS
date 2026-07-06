-- =====================================================================
-- LOAD TEST DATA GENERATOR — PART 3: Assignment, Contract, Pay Items
-- Distributes employees evenly across the 5 load-test projects and
-- gives each a realistic pay structure based on pay_status.
-- =====================================================================

-- 3a) Assignment — links each employee to one of the 5 projects (round robin)
INSERT INTO assignment (employee_id, organization_unit_id, project_id, primary_assignment, effective_from, status)
SELECT
    e.id,
    (SELECT id FROM organization_unit WHERE company_id = '00000000-0000-0000-0000-0000000000c1' AND code = 'LT-ORG'),
    (SELECT id FROM project WHERE company_id = '00000000-0000-0000-0000-0000000000c1'
        AND code = 'LT-P' || (( (substring(e.employee_number from 4)::int - 1) % 5) + 1)),
    true,
    e.hire_date,
    'ACTIVE'
FROM employee e
WHERE e.employee_number LIKE 'LT-%'
  AND NOT EXISTS (SELECT 1 FROM assignment a WHERE a.employee_id = e.id);

-- 3b) Contract — one per employee
INSERT INTO contract (employee_id, contract_type, effective_from, status)
SELECT e.id, 'FIXED_TERM', e.hire_date, 'ACTIVE'
FROM employee e
WHERE e.employee_number LIKE 'LT-%'
  AND NOT EXISTS (SELECT 1 FROM contract c WHERE c.employee_id = e.id);

-- 3c) Base salary / daily rate for every employee (amount varies a bit per employee)
INSERT INTO contract_pay_item (contract_id, employee_id, pay_component_id, amount, effective_from, status)
SELECT
    c.id, e.id,
    (SELECT id FROM payroll_component WHERE company_id = '00000000-0000-0000-0000-0000000000c1' AND name ILIKE '%base salary%' LIMIT 1),
    CASE WHEN e.pay_status = 'MONTHLY' THEN 3000 + (substring(e.employee_number from 4)::int % 12000)
         ELSE 40 + (substring(e.employee_number from 4)::int % 60) END,
    e.hire_date, 'ACTIVE'
FROM employee e
JOIN contract c ON c.employee_id = e.id
WHERE e.employee_number LIKE 'LT-%'
  AND NOT EXISTS (SELECT 1 FROM contract_pay_item cpi WHERE cpi.employee_id = e.id
                    AND cpi.pay_component_id = (SELECT id FROM payroll_component WHERE company_id = '00000000-0000-0000-0000-0000000000c1' AND name ILIKE '%base salary%' LIMIT 1));

-- 3d) Monthly employees get the richer allowance set
INSERT INTO contract_pay_item (contract_id, employee_id, pay_component_id, amount, effective_from, status)
SELECT c.id, e.id, pc.id, 500 + (substring(e.employee_number from 4)::int % 1000), e.hire_date, 'ACTIVE'
FROM employee e
JOIN contract c ON c.employee_id = e.id
CROSS JOIN payroll_component pc
WHERE e.employee_number LIKE 'LT-%'
  AND e.pay_status = 'MONTHLY'
  AND pc.company_id = '00000000-0000-0000-0000-0000000000c1'
  AND pc.name IN ('Accommodation', 'Furniture', 'Car II Allowance', 'Living Allowance', 'Location Allowance')
  AND NOT EXISTS (SELECT 1 FROM contract_pay_item cpi WHERE cpi.employee_id = e.id AND cpi.pay_component_id = pc.id);

-- 3e) Daily employees get simple allowances (messing, housing)
INSERT INTO contract_pay_item (contract_id, employee_id, pay_component_id, amount, effective_from, status)
SELECT c.id, e.id, pc.id, 150 + (substring(e.employee_number from 4)::int % 200), e.hire_date, 'ACTIVE'
FROM employee e
JOIN contract c ON c.employee_id = e.id
CROSS JOIN payroll_component pc
WHERE e.employee_number LIKE 'LT-%'
  AND e.pay_status = 'DAILY'
  AND pc.company_id = '00000000-0000-0000-0000-0000000000c1'
  AND pc.name IN ('Messing Subsidy', 'Home Subsidy')
  AND NOT EXISTS (SELECT 1 FROM contract_pay_item cpi WHERE cpi.employee_id = e.id AND cpi.pay_component_id = pc.id);
