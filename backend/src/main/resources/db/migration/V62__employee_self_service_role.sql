-- =====================================================================
-- V62  Employee Self-Service — a new, narrowly-scoped role for regular
--   employees to view their OWN payslips/timesheet and submit their own
--   leave requests. Distinct from TIMEKEEPER (which enters attendance FOR
--   other employees on a project) — this role only ever sees data tied to
--   the logged-in account's own employee record, enforced server-side via
--   the JWT's employee claim, never a client-supplied id.
-- =====================================================================

INSERT INTO permission (code, name, description) VALUES
    ('self.payslip.read', 'View own payslips', 'Self-service: view my own payroll results'),
    ('self.timesheet.read', 'View own timesheet', 'Self-service: view my own timesheet/time card'),
    ('self.leave.read', 'View own leave', 'Self-service: view my own leave requests and balance'),
    ('self.leave.write', 'Submit own leave', 'Self-service: submit my own leave/sick requests');

INSERT INTO role (id, company_id, code, name, description)
SELECT gen_random_uuid(), c.company_id, 'EMPLOYEE_SELF_SERVICE', 'Employee Self-Service',
       'A regular employee''s own account — can only view/submit their own data.'
FROM (SELECT DISTINCT company_id FROM app_user WHERE company_id IS NOT NULL) c
WHERE NOT EXISTS (
    SELECT 1 FROM role r WHERE r.company_id = c.company_id AND r.code = 'EMPLOYEE_SELF_SERVICE'
);

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN permission p ON p.code IN ('self.payslip.read', 'self.timesheet.read', 'self.leave.read', 'self.leave.write')
WHERE r.code = 'EMPLOYEE_SELF_SERVICE'
  AND NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
