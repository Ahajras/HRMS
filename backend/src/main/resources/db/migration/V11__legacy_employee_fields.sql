-- =====================================================================
-- V11  Additional meaningful fields carried over from the legacy system
--   Captures employee-file columns that exist in the old FoxPro snapshot and
--   feed later phases (payroll, overtime). All are nullable; manual entry and
--   legacy import populate them. Configuration-first principle is preserved —
--   these are plain attributes, not hardcoded business logic.
--
--   employee : job title, pay status (DAILY/MONTHLY PAID), Arabic name
--   contract : STANDARD/REFERENCE weekly hours/days + overtime category.
--              NOTE: these are nominal reference values only; actual worked
--              hours come from the timesheet/shift module (Phase 4).
-- =====================================================================

-- ---------------------------------------------------------------------
-- Employee: descriptive / classification fields
-- ---------------------------------------------------------------------
ALTER TABLE employee
    ADD COLUMN job_title       VARCHAR(150),   -- legacy TITLEDESC (e.g. CLEANER, COOK)
    ADD COLUMN job_title_code  VARCHAR(20),    -- legacy TITLECODE
    ADD COLUMN pay_status      VARCHAR(30),    -- legacy PAY_STATUS (e.g. DAILY PAID)
    ADD COLUMN arabic_name     VARCHAR(150);   -- legacy ARB_NAME

-- ---------------------------------------------------------------------
-- Contract: employment terms the payroll/overtime engines build on
-- ---------------------------------------------------------------------
ALTER TABLE contract
    ADD COLUMN working_hours_per_week  NUMERIC(5,2),  -- legacy WORKING_HR (e.g. 48) — REFERENCE only
    ADD COLUMN working_days_per_week   INT,           -- legacy WORKING_DY (e.g. 6)  — REFERENCE only
    ADD COLUMN overtime_category       VARCHAR(10),   -- legacy OT_CAT (e.g. 04)
    ADD COLUMN overtime_category_desc  VARCHAR(60);   -- legacy OT_CATDESC (e.g. BANDS 07 & BELOW)
