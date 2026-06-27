-- =====================================================================
-- V13  Action sheet number on contract pay items
--   Promotes the action sheet reference to a first-class, free-text column
--   (e.g. legacy D-17/C02/PS/2698/14960). When a new action sheet changes a
--   component, the superseded row keeps its old action_sheet_no and the new
--   ACTIVE row carries the new one; untouched components retain their original.
-- =====================================================================

ALTER TABLE contract_pay_item
    ADD COLUMN action_sheet_no VARCHAR(40);

-- Backfill: legacy import stored the action sheet ref in `remarks`.
-- Copy it across so existing rows group correctly; remarks is left untouched.
UPDATE contract_pay_item
   SET action_sheet_no = NULLIF(TRIM(remarks), '')
 WHERE action_sheet_no IS NULL;

CREATE INDEX ix_pay_item_action_sheet ON contract_pay_item (contract_id, action_sheet_no);
