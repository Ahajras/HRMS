-- Company profile plus two company-scoped demo roles/users.

CREATE TABLE company_profile (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id     UUID         NOT NULL,
    company_name   VARCHAR(200) NOT NULL,
    legal_name     VARCHAR(250),
    tax_number     VARCHAR(80),
    registration_no VARCHAR(80),
    email          VARCHAR(150),
    phone          VARCHAR(80),
    website        VARCHAR(150),
    address_line   VARCHAR(500),
    logo_url       TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     VARCHAR(100),
    updated_at     TIMESTAMPTZ,
    updated_by     VARCHAR(100),
    version        BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_company_profile_company ON company_profile (company_id);

INSERT INTO company_profile (company_id, company_name, legal_name, email, phone, website, address_line)
VALUES (
    '00000000-0000-0000-0000-0000000000c1',
    'HRMS Demo Company',
    'HRMS Demo Company W.L.L.',
    'info@hrms.local',
    '+974 0000 0000',
    'https://hrms.local',
    'Doha, Qatar'
)
ON CONFLICT (company_id) DO NOTHING;

INSERT INTO role (id, company_id, code, name, description) VALUES
    ('00000000-0000-0000-0000-000000000003',
     '00000000-0000-0000-0000-0000000000c1',
     'HR_MANAGER', 'HR Manager',
     'HR role for employees, organisation, time setup, roster, and attendance review'),
    ('00000000-0000-0000-0000-000000000004',
     '00000000-0000-0000-0000-0000000000c1',
     'ACCOUNTS_PAYROLL', 'Accounts Payroll',
     'Accounts role for payroll runs, payroll reports, and payroll configuration')
ON CONFLICT (company_id, code) DO NOTHING;

INSERT INTO role_permission (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000003', p.id
FROM permission p
WHERE p.code IN (
    'employee.read',
    'employee.write',
    'organization.read',
    'reference.read'
)
ON CONFLICT DO NOTHING;

INSERT INTO role_permission (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000004', p.id
FROM permission p
WHERE p.code IN (
    'employee.read',
    'reference.read',
    'payroll.config.read',
    'payroll.config.write'
)
ON CONFLICT DO NOTHING;

-- Demo password for both users: Admin@123
INSERT INTO app_user (id, company_id, username, email, password_hash, full_name, status) VALUES
    ('00000000-0000-0000-0000-0000000000a3',
     '00000000-0000-0000-0000-0000000000c1',
     'hr.demo', 'hr.demo@hrms.local',
     '$2b$10$c8.zy0Z0WXEWIpXD.EEtd.ieW3RiYodRH8mOCIs/sdn4ai1j5pxvq',
     'HR Demo User', 'ACTIVE'),
    ('00000000-0000-0000-0000-0000000000a4',
     '00000000-0000-0000-0000-0000000000c1',
     'accounts.demo', 'accounts.demo@hrms.local',
     '$2b$10$c8.zy0Z0WXEWIpXD.EEtd.ieW3RiYodRH8mOCIs/sdn4ai1j5pxvq',
     'Accounts Demo User', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_role (user_id, role_id) VALUES
    ('00000000-0000-0000-0000-0000000000a3', '00000000-0000-0000-0000-000000000003'),
    ('00000000-0000-0000-0000-0000000000a4', '00000000-0000-0000-0000-000000000004')
ON CONFLICT DO NOTHING;
