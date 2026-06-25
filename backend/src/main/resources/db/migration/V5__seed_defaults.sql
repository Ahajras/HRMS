-- =====================================================================
-- V5  Seed default configuration (sensible defaults; all overridable).
-- Default first country = UAE (AE), base currency = AED, per agreed defaults.
-- =====================================================================

-- Currencies (ISO 4217; note non-2 minor units for KWD/BHD/OMR)
INSERT INTO currency (code, name, symbol, minor_units) VALUES
    ('AED', 'UAE Dirham',        'د.إ', 2),
    ('SAR', 'Saudi Riyal',       '﷼',  2),
    ('QAR', 'Qatari Riyal',      '﷼',  2),
    ('KWD', 'Kuwaiti Dinar',     'د.ك', 3),
    ('BHD', 'Bahraini Dinar',    '.د.ب',3),
    ('OMR', 'Omani Rial',        '﷼',  3),
    ('EGP', 'Egyptian Pound',    'E£',  2),
    ('JOD', 'Jordanian Dinar',   'د.ا', 3),
    ('USD', 'US Dollar',         '$',   2),
    ('EUR', 'Euro',              '€',   2);

-- Countries (ISO alpha-2)
INSERT INTO country (code, name, default_currency_code) VALUES
    ('AE', 'United Arab Emirates', 'AED'),
    ('SA', 'Saudi Arabia',         'SAR'),
    ('QA', 'Qatar',                'QAR'),
    ('KW', 'Kuwait',               'KWD'),
    ('BH', 'Bahrain',              'BHD'),
    ('OM', 'Oman',                 'OMR'),
    ('EG', 'Egypt',                'EGP'),
    ('JO', 'Jordan',               'JOD');

-- Default global organisation hierarchy levels (FTDD Vol.2 Ch.32.7).
-- Company and Employee leaf are mandatory; intermediate levels are optional.
INSERT INTO org_unit_type (company_id, code, name, level_order, mandatory) VALUES
    (NULL, 'COMPANY',       'Company',       1, TRUE),
    (NULL, 'BUSINESS_UNIT', 'Business Unit', 2, FALSE),
    (NULL, 'DIVISION',      'Division',      3, FALSE),
    (NULL, 'DEPARTMENT',    'Department',    4, FALSE),
    (NULL, 'SECTION',       'Section',       5, FALSE),
    (NULL, 'TEAM',          'Team',          6, FALSE);
