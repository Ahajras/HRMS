alter table if exists employee
    add column if not exists timekeeper_employee_id uuid references employee(id);

create index if not exists ix_employee_timekeeper
    on employee (company_id, timekeeper_employee_id);
