CREATE TABLE leave_type (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID         NOT NULL,
    code            VARCHAR(30)  NOT NULL,
    name            VARCHAR(150) NOT NULL,
    time_type_id    UUID         NOT NULL REFERENCES time_type (id),
    deducts_balance BOOLEAN      NOT NULL DEFAULT TRUE,
    paid            BOOLEAN      NOT NULL DEFAULT TRUE,
    requires_ticket_default BOOLEAN NOT NULL DEFAULT FALSE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_leave_type_company_code ON leave_type (company_id, code);

CREATE TABLE leave_request (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id               UUID         NOT NULL,
    employee_id              UUID         NOT NULL REFERENCES employee (id),
    leave_type_id            UUID         NOT NULL REFERENCES leave_type (id),
    start_date               DATE         NOT NULL,
    end_date                 DATE         NOT NULL,
    return_date              DATE,
    total_days               NUMERIC(8,2) NOT NULL DEFAULT 0,
    reason                   VARCHAR(500),
    status                   VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    requires_ticket          BOOLEAN      NOT NULL DEFAULT FALSE,
    ticket_from              VARCHAR(100),
    ticket_to                VARCHAR(100),
    travel_date              DATE,
    return_travel_date       DATE,
    destination              VARCHAR(200),
    travel_remarks           VARCHAR(500),
    contact_phone            VARCHAR(80),
    contact_email            VARCHAR(150),
    address_during_leave     VARCHAR(500),
    emergency_contact_name   VARCHAR(150),
    emergency_contact_phone  VARCHAR(80),
    supervisor_approved_at   TIMESTAMPTZ,
    supervisor_approved_by   VARCHAR(100),
    hr_approved_at           TIMESTAMPTZ,
    hr_approved_by           VARCHAR(100),
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by               VARCHAR(100),
    updated_at               TIMESTAMPTZ,
    updated_by               VARCHAR(100),
    version                  BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_leave_request_dates CHECK (end_date >= start_date)
);

CREATE INDEX ix_leave_request_employee ON leave_request (company_id, employee_id, start_date, end_date);
CREATE INDEX ix_leave_request_status ON leave_request (company_id, status);

CREATE TABLE leave_adjustment (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id    UUID         NOT NULL,
    employee_id   UUID         NOT NULL REFERENCES employee (id),
    leave_type_id UUID         NOT NULL REFERENCES leave_type (id),
    adjustment_type VARCHAR(30) NOT NULL,
    days          NUMERIC(8,2) NOT NULL,
    effective_date DATE        NOT NULL,
    reason        VARCHAR(500),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    VARCHAR(100),
    updated_at    TIMESTAMPTZ,
    updated_by    VARCHAR(100),
    version       BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX ix_leave_adjustment_employee ON leave_adjustment (company_id, employee_id, leave_type_id);

INSERT INTO leave_type (company_id, code, name, time_type_id, deducts_balance, paid, requires_ticket_default)
SELECT t.company_id, 'ANNUAL', 'Annual Leave', t.id, TRUE, TRUE, TRUE
FROM time_type t
WHERE t.code IN ('L', 'LEAVE')
ON CONFLICT (company_id, code) DO NOTHING;

INSERT INTO leave_type (company_id, code, name, time_type_id, deducts_balance, paid, requires_ticket_default)
SELECT t.company_id, 'SICK', 'Sick Leave', t.id, FALSE, TRUE, FALSE
FROM time_type t
WHERE t.code IN ('SICK', 'S')
ON CONFLICT (company_id, code) DO NOTHING;

INSERT INTO leave_type (company_id, code, name, time_type_id, deducts_balance, paid, requires_ticket_default)
SELECT t.company_id, 'UNPAID', 'Unpaid Leave', t.id, FALSE, FALSE, FALSE
FROM time_type t
WHERE t.code = 'U'
ON CONFLICT (company_id, code) DO NOTHING;
