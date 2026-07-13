-- =====================================================================
-- V66  SIF / WPS export — foundation.
--
--   sponsor: an "Employer Establishment" (EID) for Qatar's Wage
--   Protection System. Multiple projects can share one sponsor — a SIF
--   file covers exactly one sponsor, so exporting several projects that
--   span more than one sponsor produces one file per sponsor.
--
--   employee.payment_method_code: 'BANK' (paid via WPS/SIF) or 'CASH'
--   (paid outside the banking system — excluded from SIF entirely).
--   Bank account + QID/Visa become mandatory only for 'BANK' employees.
--
--   lookup_value additions: PAYMENT_METHOD (BANK/CASH) and
--   SIF_NOTE_REASON (the bank's fixed Notes/Comments choices).
--
--   payroll.sif.generate_unlocked: manager-only permission to generate a
--   SIF preview before the payroll run is finally locked. Regular staff
--   can only generate once the run is LOCKED.
-- =====================================================================

CREATE TABLE sponsor (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id           UUID          NOT NULL,
    code                 VARCHAR(20)   NOT NULL,
    name                 VARCHAR(200)  NOT NULL,
    establishment_eid    VARCHAR(20)   NOT NULL,
    payer_qid            VARCHAR(20),
    payer_bank_code      VARCHAR(10)   NOT NULL,
    payer_iban           VARCHAR(34)   NOT NULL,
    status               VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by           VARCHAR(100),
    updated_at           TIMESTAMPTZ,
    updated_by           VARCHAR(100),
    version              BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_sponsor_code UNIQUE (company_id, code)
);

ALTER TABLE project ADD COLUMN IF NOT EXISTS sponsor_id UUID REFERENCES sponsor (id);

ALTER TABLE employee ADD COLUMN IF NOT EXISTS payment_method_code VARCHAR(20) NOT NULL DEFAULT 'BANK';

INSERT INTO lookup_value (company_id, category, code, label, sort_order) VALUES
    (NULL, 'PAYMENT_METHOD', 'BANK', 'Bank transfer (WPS/SIF)', 1),
    (NULL, 'PAYMENT_METHOD', 'CASH', 'Cash / outside WPS',      2);

INSERT INTO lookup_value (company_id, category, code, label, sort_order) VALUES
    (NULL, 'SIF_NOTE_REASON', 'ALLOWANCES',     'Allowances',    1),
    (NULL, 'SIF_NOTE_REASON', 'ON_LEAVE',       'On Leave',      2),
    (NULL, 'SIF_NOTE_REASON', 'PERSONAL_LOAN',  'Personal Loan', 3);

INSERT INTO permission (code, name, description) VALUES
    ('payroll.sif.generate', 'Generate SIF/WPS file', 'Generate the Qatar WPS salary file — only once the payroll run is locked'),
    ('payroll.sif.generate_unlocked', 'Preview SIF/WPS before lock', 'Manager-only: generate a SIF preview before the payroll run is finally locked');

-- Grant the base permission to whichever roles already have payroll reports access.
INSERT INTO role_permission (role_id, permission_id)
SELECT rp.role_id, p.id
FROM role_permission rp
JOIN permission existing ON existing.id = rp.permission_id AND existing.code = 'payroll.config.read'
JOIN permission p ON p.code = 'payroll.sif.generate'
WHERE NOT EXISTS (
    SELECT 1 FROM role_permission rp2 WHERE rp2.role_id = rp.role_id AND rp2.permission_id = p.id
);

-- Grant the manager-only preview permission to COMPANY_ADMIN only.
INSERT INTO role_permission (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000002', p.id
FROM permission p
WHERE p.code = 'payroll.sif.generate_unlocked';
