-- =====================================================================
-- V27  Time type for days outside an employee's active service window.
--   Used when a timesheet period overlaps the hire/termination date. These
--   days are not absence, leave, or worked time; they are zero-pay placeholders.
-- =====================================================================
INSERT INTO time_type (company_id, code, name, category, paid, counts_as_worked, affects_leave, factor, sort_order)
VALUES ('00000000-0000-0000-0000-0000000000c1', 'NOT_EMPLOYED', 'Not employed', 'NOT_EMPLOYED', FALSE, FALSE, FALSE, 0.000, 90)
ON CONFLICT (company_id, code) DO NOTHING;
