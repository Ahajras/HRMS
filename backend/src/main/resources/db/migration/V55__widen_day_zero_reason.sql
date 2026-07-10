-- =====================================================================
-- V55  Day Zero — widen payroll_adjustment.reason so it can hold a full
--   per-component breakdown (which component, old vs new type, amount),
--   not just a short one-line label. Needed so a Day Zero correction is
--   fully self-explanatory without re-deriving it from scratch later.
-- =====================================================================

ALTER TABLE payroll_adjustment ALTER COLUMN reason TYPE TEXT;
