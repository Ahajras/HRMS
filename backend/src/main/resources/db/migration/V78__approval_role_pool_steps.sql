drop index if exists ux_approval_instance_step_order;

create index if not exists ix_approval_instance_step_order
    on approval_instance_step(instance_id, step_order);

create unique index if not exists ux_approval_instance_step_approver
    on approval_instance_step(instance_id, step_order, approver_employee_id);
