-- T should behave like other deduction time types on the payslip:
-- show the planned entitlement, then deduct the shortage hours.

UPDATE time_type_payroll_rule r
SET action = 'DEDUCT',
    percent = 100.00,
    basis = 'SHORTAGE',
    affects_overtime = FALSE,
    process_separately = FALSE,
    threshold_days = 0,
    threshold_scope = 'NONE',
    year_basis = 'CALENDAR',
    remarks = 'Initialized late rule: pay planned hours and deduct shortage hours.',
    updated_at = now(),
    updated_by = 'V74__late_rules_deduct_shortage_display'
FROM time_type tt
WHERE r.time_type_id = tt.id
  AND tt.code = 'T';
