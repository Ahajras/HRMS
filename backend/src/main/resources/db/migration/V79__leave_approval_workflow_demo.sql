insert into approval_workflow (company_id, process_code, project_id, pay_group, name, created_by)
select p.company_id, 'LEAVE_REQUEST', p.id, 'ALL', 'Leave approval - ' || p.code, 'migration'
from project p
where p.code = 'DEMO'
on conflict do nothing;

insert into approval_workflow_step (workflow_id, step_order, name, approver_type, created_by)
select w.id, 1, 'Direct supervisor', 'SUPERVISOR', 'migration'
from approval_workflow w
where w.process_code = 'LEAVE_REQUEST'
  and not exists (select 1 from approval_workflow_step s where s.workflow_id = w.id and s.step_order = 1);

insert into approval_workflow_step (workflow_id, step_order, name, approver_type, approver_role_code, created_by)
select w.id, 2, 'Project HR manager', 'PROJECT_ROLE', 'HR_MANAGER', 'migration'
from approval_workflow w
where w.process_code = 'LEAVE_REQUEST'
  and not exists (select 1 from approval_workflow_step s where s.workflow_id = w.id and s.step_order = 2);
