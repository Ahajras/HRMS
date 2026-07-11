-- =====================================================================
-- V58  Day Zero — record the new time type and the source timesheet day
--   as structured, queryable columns (not just free text inside the
--   reason). Needed so annual/consecutive usage counting (Time Usage tab,
--   payroll threshold rules) can correctly account for a Day Zero
--   correction — previously it was invisible to anything except a human
--   reading the reason text.
-- =====================================================================

ALTER TABLE payroll_adjustment ADD COLUMN IF NOT EXISTS new_time_type_id UUID;
ALTER TABLE payroll_adjustment ADD COLUMN IF NOT EXISTS timesheet_day_id UUID;
