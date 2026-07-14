-- Configurable approval demo foundation:
-- - User roles used by approval routing.
-- - Project-level role assignment for approvers such as HR / HR Manager.

insert into role (id, company_id, code, name, description)
select gen_random_uuid(), null, v.code, v.name, v.description
from (values
    ('MANAGER', 'Manager', 'Can be selected as an employee supervisor / approval manager'),
    ('HR', 'HR', 'HR approver candidate'),
    ('HR_MANAGER', 'HR Manager', 'HR manager approver candidate'),
    ('PROJECT_MANAGER', 'Project Manager', 'Project-level approver candidate')
) as v(code, name, description)
where not exists (select 1 from role r where r.code = v.code);

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r
join permission p on p.code in ('employee.read')
where r.code in ('MANAGER', 'HR', 'HR_MANAGER', 'PROJECT_MANAGER')
on conflict do nothing;

insert into role_permission (role_id, permission_id)
select r.id, p.id
from role r
join permission p on p.code in ('employee.write')
where r.code in ('HR', 'HR_MANAGER')
on conflict do nothing;

create table if not exists project_approval_role (
    id uuid primary key,
    company_id uuid not null references company(id),
    project_id uuid not null references project(id),
    role_code varchar(50) not null,
    employee_id uuid not null references employee(id),
    status varchar(20) not null default 'ACTIVE',
    created_at timestamptz not null default now(),
    created_by varchar(100),
    updated_at timestamptz,
    updated_by varchar(100),
    version bigint not null default 0
);

create unique index if not exists uq_project_approval_role
    on project_approval_role(company_id, project_id, role_code, employee_id);

create index if not exists ix_project_approval_role_scope
    on project_approval_role(company_id, project_id, role_code, status);
