-- =====================================================================
-- V20  Crew code is unique PER PROJECT (not per company)
--      so the same code (e.g. IT001) can exist in different projects.
-- =====================================================================
DROP INDEX IF EXISTS uq_crew_company_code;
CREATE UNIQUE INDEX uq_crew_company_project_code ON crew (company_id, project_id, code);
