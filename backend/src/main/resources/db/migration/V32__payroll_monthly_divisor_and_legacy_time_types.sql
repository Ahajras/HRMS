ALTER TABLE payroll_rule
    ADD COLUMN month_divisor NUMERIC(6,2) NOT NULL DEFAULT 30.00;

UPDATE payroll_rule
SET month_divisor = 30.00;

INSERT INTO time_type (company_id, code, name, category, paid, counts_as_worked, affects_leave, factor, sort_order)
VALUES
    ('00000000-0000-0000-0000-0000000000c1', 'A', 'Accident', 'ACCIDENT', TRUE, FALSE, FALSE, 1.000, 101),
    ('00000000-0000-0000-0000-0000000000c1', 'B', 'Business', 'BUSINESS', TRUE, TRUE, FALSE, 1.000, 102),
    ('00000000-0000-0000-0000-0000000000c1', 'C', 'Compassionate Leave', 'LEAVE', TRUE, FALSE, TRUE, 1.000, 103),
    ('00000000-0000-0000-0000-0000000000c1', 'E', 'Excused Leave', 'LEAVE', TRUE, FALSE, TRUE, 1.000, 104),
    ('00000000-0000-0000-0000-0000000000c1', 'F', 'Field Break', 'FIELD_BREAK', TRUE, FALSE, FALSE, 1.000, 105),
    ('00000000-0000-0000-0000-0000000000c1', 'H', 'Holiday', 'HOLIDAY', TRUE, FALSE, FALSE, 1.000, 106),
    ('00000000-0000-0000-0000-0000000000c1', 'I', 'Incentive', 'INCENTIVE', TRUE, FALSE, FALSE, 1.000, 107),
    ('00000000-0000-0000-0000-0000000000c1', 'K', 'Lunch Break', 'LUNCH_BREAK', FALSE, FALSE, FALSE, 0.000, 108),
    ('00000000-0000-0000-0000-0000000000c1', 'L', 'Annual Leave', 'LEAVE', TRUE, FALSE, TRUE, 1.000, 109),
    ('00000000-0000-0000-0000-0000000000c1', 'N', 'Absent Hours', 'ABSENCE', FALSE, FALSE, FALSE, 0.000, 110),
    ('00000000-0000-0000-0000-0000000000c1', 'R', 'Rest & Recreation', 'RR', TRUE, FALSE, TRUE, 1.000, 111),
    ('00000000-0000-0000-0000-0000000000c1', 'S', 'Sick Leave', 'SICK', TRUE, FALSE, TRUE, 1.000, 112),
    ('00000000-0000-0000-0000-0000000000c1', 'U', 'Unpaid Leave', 'UNPAID', FALSE, FALSE, TRUE, 0.000, 113),
    ('00000000-0000-0000-0000-0000000000c1', 'W', 'Weekend', 'REST', TRUE, FALSE, FALSE, 1.000, 114)
ON CONFLICT (company_id, code) DO UPDATE
SET name = EXCLUDED.name,
    category = EXCLUDED.category,
    paid = EXCLUDED.paid,
    counts_as_worked = EXCLUDED.counts_as_worked,
    affects_leave = EXCLUDED.affects_leave,
    factor = EXCLUDED.factor,
    sort_order = EXCLUDED.sort_order;
