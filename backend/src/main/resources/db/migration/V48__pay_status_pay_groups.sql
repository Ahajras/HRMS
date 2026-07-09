INSERT INTO lookup_value (company_id, category, code, label, sort_order, status) VALUES
    (NULL, 'PAY_STATUS', 'WEEKLY', 'Weekly', 4, 'ACTIVE'),
    (NULL, 'PAY_STATUS', 'BI_WEEKLY', 'Bi-weekly', 5, 'ACTIVE')
ON CONFLICT DO NOTHING;
