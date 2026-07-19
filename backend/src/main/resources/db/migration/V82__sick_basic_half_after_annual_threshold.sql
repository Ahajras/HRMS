update time_type_payroll_rule ttr
set action = 'DEDUCT',
    percent = 50.00,
    basis = 'PLANNED_SHIFT',
    threshold_days = 14,
    threshold_scope = 'ANNUAL',
    year_basis = 'CALENDAR',
    remarks = 'Sick rule: first 14 annual sick days are fully paid; later S days deduct 50% of one planned shift from basic salary.'
from time_type tt
join payroll_component pc on pc.company_id = tt.company_id
where ttr.time_type_id = tt.id
  and ttr.payroll_component_id = pc.id
  and upper(tt.code) = 'S'
  and (
      upper(coalesce(pc.category, '')) = 'SALARY'
      or upper(coalesce(pc.name, '')) like '%BASIC%'
      or upper(coalesce(pc.name, '')) like '%BASE%'
  );
