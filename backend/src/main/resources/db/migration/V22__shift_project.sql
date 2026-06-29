-- =====================================================================
-- V22  Shift belongs to a project (so shifts can be scoped per project,
--      and the Shift Roster can filter shifts by the selected project).
-- =====================================================================
ALTER TABLE shift ADD COLUMN project_id UUID REFERENCES project (id);
CREATE INDEX ix_shift_project ON shift (company_id, project_id);
