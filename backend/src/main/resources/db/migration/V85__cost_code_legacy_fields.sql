ALTER TABLE cost_code
    ADD COLUMN IF NOT EXISTS currency_code VARCHAR(10),
    ADD COLUMN IF NOT EXISTS description VARCHAR(200),
    ADD COLUMN IF NOT EXISTS active BOOLEAN;

UPDATE cost_code
SET currency_code = COALESCE(currency_code, 'QAR'),
    description = COALESCE(description, name),
    active = COALESCE(active, upper(COALESCE(status, 'ACTIVE')) = 'ACTIVE');

ALTER TABLE cost_code
    ALTER COLUMN currency_code SET DEFAULT 'QAR',
    ALTER COLUMN active SET DEFAULT true;
