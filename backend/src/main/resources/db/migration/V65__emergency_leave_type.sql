INSERT INTO time_type (company_id, code, name, category, paid, counts_as_worked, affects_leave, factor, sort_order, status)
SELECT c.company_id, 'EL', 'Emergency Leave', 'EMERGENCY', TRUE, FALSE, FALSE, 1.000, 115, 'ACTIVE'
FROM (
    SELECT DISTINCT company_id FROM time_type
) c
ON CONFLICT (company_id, code) DO UPDATE
SET name = EXCLUDED.name,
    category = EXCLUDED.category,
    paid = EXCLUDED.paid,
    counts_as_worked = EXCLUDED.counts_as_worked,
    affects_leave = EXCLUDED.affects_leave,
    factor = EXCLUDED.factor,
    sort_order = EXCLUDED.sort_order,
    status = EXCLUDED.status;

INSERT INTO leave_type (company_id, code, name, time_type_id, deducts_balance, paid, requires_ticket_default, status)
SELECT t.company_id, 'EMERGENCY', 'Emergency Leave', t.id, FALSE, TRUE, FALSE, 'ACTIVE'
FROM time_type t
WHERE t.code = 'EL'
ON CONFLICT (company_id, code) DO UPDATE
SET name = EXCLUDED.name,
    time_type_id = EXCLUDED.time_type_id,
    deducts_balance = EXCLUDED.deducts_balance,
    paid = EXCLUDED.paid,
    requires_ticket_default = EXCLUDED.requires_ticket_default,
    status = EXCLUDED.status;
