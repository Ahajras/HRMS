ALTER TABLE payroll_rule
    ADD COLUMN IF NOT EXISTS quantity_source VARCHAR(30) NOT NULL DEFAULT 'PAYABLE_SCHEDULE';

UPDATE payroll_rule
SET quantity_source = CASE
    WHEN upper(coalesce(pay_item_basis, '')) = 'DAILY_RATE' THEN 'ACTUAL_WORKED'
    ELSE 'PAYABLE_SCHEDULE'
END
WHERE quantity_source IS NULL OR quantity_source = 'PAYABLE_SCHEDULE';
