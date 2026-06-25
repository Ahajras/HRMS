-- =====================================================================
-- V7  Seed a real company (tenant) and a company-scoped admin user.
--
-- Rationale: the V6 'admin' account is a PLATFORM admin (company_id NULL),
-- so every company-scoped screen forces the operator to supply a Company ID
-- header manually. That is unacceptable for a system handed to real users.
--
-- This migration creates:
--   * a fixed company tenant id (companies have no own table - the company
--     is simply a tenant UUID stamped on users and data),
--   * a company-scoped COMPANY_ADMIN role holding every permission,
--   * a user 'manager' that belongs to that company.
--
-- Logging in as 'manager' carries the company in the JWT, so the UI never
-- shows the Company ID box and no UUID ever needs to be typed.
--
--   username: manager   password: Admin@123   >>> CHANGE AFTER FIRST LOGIN <<<
-- =====================================================================

-- The default company tenant id (re-used by user + role below).
-- (No 'company' table exists yet; the tenant is just this UUID.)

-- ---------------------------------------------------------------------
-- Company-scoped COMPANY_ADMIN role with every permission.
-- ---------------------------------------------------------------------
INSERT INTO role (id, company_id, code, name, description) VALUES
    ('00000000-0000-0000-0000-000000000002',
     '00000000-0000-0000-0000-0000000000c1',
     'COMPANY_ADMIN', 'Company Administrator',
     'Administrator for the default company with all module permissions');

INSERT INTO role_permission (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000002', p.id FROM permission p;

-- ---------------------------------------------------------------------
-- Company-scoped admin user (password: Admin@123 -> same BCrypt hash as V6).
-- ---------------------------------------------------------------------
INSERT INTO app_user (id, company_id, username, email, password_hash, full_name, status) VALUES
    ('00000000-0000-0000-0000-0000000000a2',
     '00000000-0000-0000-0000-0000000000c1',
     'manager', 'manager@hrms.local',
     '$2b$10$c8.zy0Z0WXEWIpXD.EEtd.ieW3RiYodRH8mOCIs/sdn4ai1j5pxvq',
     'Company Administrator', 'ACTIVE');

INSERT INTO user_role (user_id, role_id) VALUES
    ('00000000-0000-0000-0000-0000000000a2', '00000000-0000-0000-0000-000000000002');
