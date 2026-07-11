-- =====================================================================
-- V57  Audit Tools — a manager-only screen for safe, guarded cleanup
--   actions (e.g. deleting a still-pending Day Zero adjustment, or a
--   payroll run that hasn't been approved/locked yet), so this no longer
--   has to be done by hand against the database.
-- =====================================================================

INSERT INTO permission (code, name, description) VALUES
    ('audit.tools', 'Audit tools', 'Manager-only data cleanup utilities (Day Zero adjustments, draft payroll runs)');

-- Grant to the existing company-admin role only (not automatically to any
-- other role, even ones with broad permissions already).
INSERT INTO role_permission (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000002', p.id
FROM permission p
WHERE p.code = 'audit.tools';
