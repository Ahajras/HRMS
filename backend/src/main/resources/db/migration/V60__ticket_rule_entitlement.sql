ALTER TABLE provision_rule ADD COLUMN IF NOT EXISTS ticket_quantity NUMERIC(18,4) NOT NULL DEFAULT 1;
ALTER TABLE provision_rule ADD COLUMN IF NOT EXISTS ticket_expiry_months INTEGER NOT NULL DEFAULT 0;
ALTER TABLE provision_rule ADD COLUMN IF NOT EXISTS ticket_entitlement_mode VARCHAR(30) NOT NULL DEFAULT 'ON_CYCLE_DATE';

UPDATE provision_rule
SET formula_expression = 'ticket_amount * ticket_quantity / ticket_cycle_months'
WHERE upper(provision_type) = 'TICKET'
  AND formula_expression = 'ticket_amount / ticket_cycle_months';

UPDATE provision_rule
SET pay_group = 'MONTHLY'
WHERE upper(provision_type) = 'TICKET'
  AND upper(pay_group) = 'ALL'
  AND project_id IS NULL;
