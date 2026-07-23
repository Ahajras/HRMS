CREATE TABLE IF NOT EXISTS employee_document_notification_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    employee_document_id UUID NOT NULL REFERENCES employee_document(id) ON DELETE CASCADE,
    alert_days INT NOT NULL,
    recipient_email VARCHAR(150) NOT NULL,
    notified_on DATE NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_employee_document_notification_once
        UNIQUE (employee_document_id, alert_days, recipient_email, notified_on)
);

CREATE INDEX IF NOT EXISTS ix_employee_document_notification_doc
    ON employee_document_notification_log(employee_document_id);
