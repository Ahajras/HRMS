-- =====================================================================
-- LOAD TEST DATA GENERATOR — PART 2: Employees
-- Change the number in generate_series(1, 40000) below to control scale.
-- Every row is tagged with employee_number 'LT-#####' so it is trivial
-- to identify and delete later (see 04_cleanup.sql).
-- =====================================================================

INSERT INTO employee (company_id, employee_number, first_name, last_name, hire_date, status, pay_status)
SELECT
    '00000000-0000-0000-0000-0000000000c1',
    'LT-' || lpad(gs::text, 5, '0'),
    'LoadTest',
    'Employee' || gs,
    DATE '2024-01-01' + ((gs % 500) || ' days')::interval,
    'ACTIVE',
    CASE WHEN gs % 10 < 7 THEN 'MONTHLY' ELSE 'DAILY' END   -- 70% monthly, 30% daily
FROM generate_series(1, 40000) AS gs;
