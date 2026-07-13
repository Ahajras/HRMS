-- If T rules were edited back to HOURS, payroll treats the worked hours as a
-- deduction. Force T back to the intended setup: every component deducts only
-- SHORTAGE hours (planned - worked). ACTUAL_WORKED payroll rules skip the
-- duplicate deduction in code and only pay worked hours.

UPDATE time_type_payroll_rule r
SET action = 'DEDUCT',
    percent = 100.00,
    basis = 'SHORTAGE',
    affects_overtime = FALSE,
    process_separately = FALSE,
    threshold_days = 0,
    threshold_scope = 'NONE',
    year_basis = 'CALENDAR',
    remarks = 'Initialized late rule: deduct planned-vs-worked shortage hours.',
    updated_at = now(),
    updated_by = 'V72__force_late_rules_shortage_basis'
FROM time_type tt
WHERE r.time_type_id = tt.id
  AND tt.code = 'T';
