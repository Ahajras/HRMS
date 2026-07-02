DELETE FROM time_type
WHERE code IN ('REGULAR', 'OVERTIME', 'REST', 'HOLIDAY', 'ABSENCE', 'LEAVE', 'SICK', 'ACCIDENT', 'RR', 'UNPAID', 'NOT_EMPLOYED');

UPDATE time_type
SET name = 'Normal Working Hours',
    category = 'REGULAR',
    paid = TRUE,
    counts_as_worked = TRUE,
    affects_leave = FALSE,
    factor = 1.000
WHERE code = 'N';
