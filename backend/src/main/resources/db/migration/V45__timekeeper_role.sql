INSERT INTO permission (code, name, description)
VALUES ('timekeeper.attendance', 'Enter timekeeper attendance', 'Use the daily timekeeper attendance console')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role (id, company_id, code, name, description)
VALUES (
    '00000000-0000-0000-0000-000000000005',
    '00000000-0000-0000-0000-0000000000c1',
    'TIMEKEEPER',
    'Timekeeper',
    'Can enter daily attendance for assigned employees only'
)
ON CONFLICT (company_id, code) DO NOTHING;

INSERT INTO role_permission (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000005', p.id
FROM permission p
WHERE p.code = 'timekeeper.attendance'
ON CONFLICT DO NOTHING;

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code IN ('SUPER_ADMIN', 'COMPANY_ADMIN')
  AND p.code = 'timekeeper.attendance'
ON CONFLICT DO NOTHING;
