create table if not exists approval_workflow (
    id uuid primary key default gen_random_uuid(),
    company_id uuid not null,
    process_code varchar(60) not null,
    project_id uuid,
    pay_group varchar(30) not null default 'ALL',
    name varchar(150) not null,
    status varchar(20) not null default 'ACTIVE',
    created_at timestamptz not null default now(),
    created_by varchar(100),
    updated_at timestamptz,
    updated_by varchar(100),
    version bigint not null default 0
);

create unique index if not exists ux_approval_workflow_scope
    on approval_workflow(company_id, process_code, coalesce(project_id, '00000000-0000-0000-0000-000000000000'::uuid), pay_group);

create table if not exists approval_workflow_step (
    id uuid primary key default gen_random_uuid(),
    workflow_id uuid not null references approval_workflow(id) on delete cascade,
    step_order int not null,
    name varchar(150) not null,
    approver_type varchar(40) not null,
    approver_role_code varchar(60),
    approver_employee_id uuid,
    status varchar(20) not null default 'ACTIVE',
    created_at timestamptz not null default now(),
    created_by varchar(100),
    updated_at timestamptz,
    updated_by varchar(100),
    version bigint not null default 0
);

create unique index if not exists ux_approval_workflow_step_order
    on approval_workflow_step(workflow_id, step_order);

create table if not exists approval_instance (
    id uuid primary key default gen_random_uuid(),
    company_id uuid not null,
    workflow_id uuid not null references approval_workflow(id),
    process_code varchar(60) not null,
    entity_type varchar(60) not null,
    entity_id uuid not null,
    project_id uuid,
    employee_id uuid,
    status varchar(20) not null default 'PENDING',
    current_step_order int,
    submitted_by varchar(100),
    submitted_at timestamptz not null default now(),
    completed_at timestamptz,
    created_at timestamptz not null default now(),
    created_by varchar(100),
    updated_at timestamptz,
    updated_by varchar(100),
    version bigint not null default 0
);

create unique index if not exists ux_approval_instance_entity_active
    on approval_instance(company_id, entity_type, entity_id)
    where status in ('PENDING', 'APPROVED');

create table if not exists approval_instance_step (
    id uuid primary key default gen_random_uuid(),
    instance_id uuid not null references approval_instance(id) on delete cascade,
    step_order int not null,
    name varchar(150) not null,
    approver_type varchar(40) not null,
    approver_role_code varchar(60),
    approver_employee_id uuid,
    status varchar(20) not null default 'WAITING',
    decided_by varchar(100),
    decided_at timestamptz,
    remarks text,
    created_at timestamptz not null default now(),
    created_by varchar(100),
    updated_at timestamptz,
    updated_by varchar(100),
    version bigint not null default 0
);

create unique index if not exists ux_approval_instance_step_order
    on approval_instance_step(instance_id, step_order);

insert into approval_workflow (company_id, process_code, project_id, pay_group, name, created_by)
select p.company_id, 'TIMESHEET_SUBMIT', p.id, 'ALL', 'Timesheet approval - ' || p.code, 'migration'
from project p
where p.code = 'DEMO'
on conflict do nothing;

insert into approval_workflow_step (workflow_id, step_order, name, approver_type, created_by)
select w.id, 1, 'Direct supervisor', 'SUPERVISOR', 'migration'
from approval_workflow w
where w.process_code = 'TIMESHEET_SUBMIT'
  and not exists (select 1 from approval_workflow_step s where s.workflow_id = w.id and s.step_order = 1);

insert into approval_workflow_step (workflow_id, step_order, name, approver_type, approver_role_code, created_by)
select w.id, 2, 'Project HR manager', 'PROJECT_ROLE', 'HR_MANAGER', 'migration'
from approval_workflow w
where w.process_code = 'TIMESHEET_SUBMIT'
  and not exists (select 1 from approval_workflow_step s where s.workflow_id = w.id and s.step_order = 2);
