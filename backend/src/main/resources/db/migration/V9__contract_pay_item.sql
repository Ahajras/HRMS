-- =====================================================================
-- V9  Contract pay items (effective-dated salary structure)
--   Each contract carries financial line items chosen from payroll_component
--   (basic salary, housing allowance, ...). Items are effective-dated: a raise
--   creates a NEW version and supersedes the previous one (which is retained as
--   history, status INACTIVE). History is never overwritten (FTDD effective dating).
-- =====================================================================

CREATE TABLE contract_pay_item (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id       UUID          NOT NULL REFERENCES contract (id),
    employee_id       UUID          NOT NULL REFERENCES employee (id),
    pay_component_id  UUID          NOT NULL REFERENCES payroll_component (id),
    amount            NUMERIC(18,4) NOT NULL,
    currency_code     VARCHAR(3),
    effective_from    DATE          NOT NULL,
    effective_to      DATE,
    status            VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | INACTIVE
    remarks           VARCHAR(255),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        VARCHAR(100),
    updated_at        TIMESTAMPTZ,
    updated_by        VARCHAR(100),
    version           BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ck_pay_item_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE INDEX ix_pay_item_contract  ON contract_pay_item (contract_id);
CREATE INDEX ix_pay_item_employee  ON contract_pay_item (employee_id);
CREATE INDEX ix_pay_item_component ON contract_pay_item (pay_component_id);
-- fast lookup of the current active item for a (contract, component)
CREATE INDEX ix_pay_item_active ON contract_pay_item (contract_id, pay_component_id)
    WHERE status = 'ACTIVE';
