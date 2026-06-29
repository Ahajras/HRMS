-- =====================================================================
-- V23  Overtime eligibility: overtime_category reference + employee band
--   overtime_category : configurable category with an OT-eligibility flag.
--     If a category is NOT ot_eligible, overtime worked is NOT counted for
--     employees in it (timesheet zeroes their OT).
--   employee gains: overtime_category_code (FK by code) + band (grade).
--   BAND lookup seeded for the band dropdown. Values are EDITABLE defaults.
-- =====================================================================

CREATE TABLE overtime_category (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id   UUID         NOT NULL,
    code         VARCHAR(20)  NOT NULL,
    name         VARCHAR(150) NOT NULL,
    ot_eligible  BOOLEAN      NOT NULL DEFAULT TRUE,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   VARCHAR(100),
    updated_at   TIMESTAMPTZ,
    updated_by   VARCHAR(100),
    version      BIGINT       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_overtime_category_company_code ON overtime_category (company_id, code);

ALTER TABLE employee ADD COLUMN overtime_category_code VARCHAR(20);
ALTER TABLE employee ADD COLUMN band                   VARCHAR(20);

-- Seed example overtime categories (EDITABLE — confirm against CCC policy).
INSERT INTO overtime_category (company_id, code, name, ot_eligible) VALUES
    ('00000000-0000-0000-0000-0000000000c1', '04', 'Bands 07 & below (eligible)', TRUE),
    ('00000000-0000-0000-0000-0000000000c1', '01', 'Management (not eligible)',   FALSE);

-- Seed band values (grades). Add/adjust as needed.
INSERT INTO lookup_value (company_id, category, code, label, sort_order) VALUES
    (NULL, 'BAND', '01', 'Band 01', 1),
    (NULL, 'BAND', '02', 'Band 02', 2),
    (NULL, 'BAND', '03', 'Band 03', 3),
    (NULL, 'BAND', '04', 'Band 04', 4),
    (NULL, 'BAND', '05', 'Band 05', 5),
    (NULL, 'BAND', '06', 'Band 06', 6),
    (NULL, 'BAND', '07', 'Band 07', 7),
    (NULL, 'BAND', '08', 'Band 08', 8),
    (NULL, 'BAND', '09', 'Band 09', 9),
    (NULL, 'BAND', '10', 'Band 10', 10);
