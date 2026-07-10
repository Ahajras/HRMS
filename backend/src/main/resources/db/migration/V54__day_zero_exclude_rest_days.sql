-- =====================================================================
-- V54  Day Zero fix — rest/holiday days should never have been marked
--   "estimated" in the first place (the weekly schedule is known in
--   advance, so there is nothing uncertain about them). Clean up any
--   day that was wrongly flagged before this fix, so it no longer shows
--   up as a candidate for correction on the Day Zero screen.
-- =====================================================================

UPDATE timesheet_day td
SET estimated = false
FROM time_type tt
WHERE tt.id = td.time_type_id
  AND td.estimated = true
  AND upper(coalesce(tt.category, '')) IN ('REST', 'HOLIDAY');
