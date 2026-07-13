-- Late/short-worked days should show full planned pay for every component,
-- while only basic salary carries the actual shortage deduction for now.
UPDATE time_type_payroll_rule r
SET action = CASE
        WHEN upper(coalesce(pc.category, '')) = 'SALARY' OR upper(coalesce(pc.name, '')) LIKE '%BASE%'
            THEN 'DEDUCT'
        ELSE 'PAY'
    END,
    basis = CASE
        WHEN upper(coalesce(pc.category, '')) = 'SALARY' OR upper(coalesce(pc.name, '')) LIKE '%BASE%'
            THEN 'SHORTAGE'
        ELSE 'PLANNED_SHIFT'
    END,
    percent = 100.00,
    threshold_days = 0,
    threshold_scope = 'NONE',
    year_basis = 'CALENDAR',
    affects_overtime = FALSE,
    process_separately = FALSE,
    remarks = CASE
        WHEN upper(coalesce(pc.category, '')) = 'SALARY' OR upper(coalesce(pc.name, '')) LIKE '%BASE%'
            THEN 'Initialized late rule: pay planned hours and deduct shortage hours from basic salary.'
        ELSE 'Initialized late rule: pay planned shift hours; late deduction is handled by basic salary.'
    END,
    updated_at = now()
FROM time_type tt, payroll_component pc
WHERE r.time_type_id = tt.id
  AND pc.id = r.payroll_component_id
  AND tt.code = 'T';
