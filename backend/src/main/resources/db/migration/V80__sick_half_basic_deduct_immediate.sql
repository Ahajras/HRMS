update time_type_payroll_rule ttr
set action = 'DEDUCT',
    percent = 50.00,
    basis = 'HOURS',
    threshold_days = 0,
    threshold_scope = 'NONE',
    remarks = 'Sick-half rule: deduct 50% of basic salary from the first SH day.'
from time_type tt
join payroll_component pc on pc.company_id = tt.company_id
where ttr.time_type_id = tt.id
  and ttr.payroll_component_id = pc.id
  and upper(tt.code) = 'SH'
  and (
      upper(coalesce(pc.category, '')) = 'SALARY'
      or upper(coalesce(pc.name, '')) like '%BASIC%'
      or upper(coalesce(pc.name, '')) like '%BASE%'
  );
