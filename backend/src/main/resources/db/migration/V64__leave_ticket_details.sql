ALTER TABLE leave_request
    ADD COLUMN IF NOT EXISTS passport_number VARCHAR(80),
    ADD COLUMN IF NOT EXISTS dependent_count INTEGER;
