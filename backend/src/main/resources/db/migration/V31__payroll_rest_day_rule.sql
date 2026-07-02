ALTER TABLE payroll_rule
    ADD COLUMN rest_day_ot_multiplier NUMERIC(8,4) NOT NULL DEFAULT 1.5000,
    ADD COLUMN weekly_rest_paid BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE payroll_rule
SET rest_day_ot_multiplier = 1.5000,
    weekly_rest_paid = TRUE;
