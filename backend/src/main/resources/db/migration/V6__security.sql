-- =====================================================================
-- V6  Security: users, roles, permissions (FTDD Vol.2 Ch.31).
--
-- Authentication is JWT-based; authorisation is permission-driven via roles.
-- A user belongs to a company (NULL = platform/global super-admin who may
-- act across companies). Roles and permissions grant fine-grained access.
--
-- Seeds:
--   * a full permission catalogue for the Phase 1 modules,
--   * a global SUPER_ADMIN role holding every permission,
--   * a default admin user  (username: admin / password: Admin@123).
--     >>> CHANGE THIS PASSWORD AFTER FIRST LOGIN <<<
-- =====================================================================

CREATE TABLE app_user (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID,                                  -- NULL = platform super-admin
    employee_id     UUID REFERENCES employee (id),         -- optional link to an employee
    username        VARCHAR(100) NOT NULL,
    email           VARCHAR(150),
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(150),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, DISABLED, LOCKED
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_app_user_username UNIQUE (username)
);

CREATE INDEX ix_app_user_company ON app_user (company_id);

CREATE TABLE role (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id   UUID,                                     -- NULL = global role
    code         VARCHAR(50)  NOT NULL,
    name         VARCHAR(100) NOT NULL,
    description  VARCHAR(255),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   VARCHAR(100),
    updated_at   TIMESTAMPTZ,
    updated_by   VARCHAR(100),
    version      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_role_code UNIQUE (company_id, code)
);

CREATE TABLE permission (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code         VARCHAR(100) NOT NULL,
    name         VARCHAR(150),
    description  VARCHAR(255),
    version      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_permission_code UNIQUE (code)
);

CREATE TABLE user_role (
    user_id  UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role_id  UUID NOT NULL REFERENCES role (id)     ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE role_permission (
    role_id        UUID NOT NULL REFERENCES role (id)       ON DELETE CASCADE,
    permission_id  UUID NOT NULL REFERENCES permission (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- ---------------------------------------------------------------------
-- Seed permission catalogue (one per resource + action).
-- ---------------------------------------------------------------------
INSERT INTO permission (code, name, description) VALUES
    ('employee.read',          'View employees',          'Read employee, contract and assignment records'),
    ('employee.write',         'Manage employees',        'Create/update employee, contract and assignment records'),
    ('organization.read',      'View organisation',       'Read organisation units and levels'),
    ('organization.write',     'Manage organisation',     'Create/update organisation units and levels'),
    ('reference.read',         'View reference data',     'Read currencies, countries, calendars'),
    ('reference.write',        'Manage reference data',   'Create/update currencies, countries, calendars'),
    ('payroll.config.read',    'View payroll config',     'Read payroll components and configuration'),
    ('payroll.config.write',   'Manage payroll config',   'Create/update payroll components and configuration'),
    ('security.user.read',     'View users',              'Read application users'),
    ('security.user.write',    'Manage users',            'Create/update application users and assign roles'),
    ('security.role.read',     'View roles',              'Read roles and permissions'),
    ('security.role.write',    'Manage roles',            'Create/update roles and assign permissions');

-- ---------------------------------------------------------------------
-- Seed global SUPER_ADMIN role with every permission.
-- ---------------------------------------------------------------------
INSERT INTO role (id, company_id, code, name, description) VALUES
    ('00000000-0000-0000-0000-000000000001', NULL, 'SUPER_ADMIN', 'Super Administrator',
     'Platform administrator with all permissions across all companies');

INSERT INTO role_permission (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000001', p.id FROM permission p;

-- ---------------------------------------------------------------------
-- Seed default admin user (password: Admin@123 -> BCrypt).
-- ---------------------------------------------------------------------
INSERT INTO app_user (id, company_id, username, email, password_hash, full_name, status) VALUES
    ('00000000-0000-0000-0000-0000000000a1', NULL, 'admin', 'admin@hrms.local',
     '$2b$10$c8.zy0Z0WXEWIpXD.EEtd.ieW3RiYodRH8mOCIs/sdn4ai1j5pxvq', 'System Administrator', 'ACTIVE');

INSERT INTO user_role (user_id, role_id) VALUES
    ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-000000000001');
