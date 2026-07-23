ALTER TABLE crew ADD COLUMN IF NOT EXISTS supervisor_employee_id UUID;
ALTER TABLE crew ADD COLUMN IF NOT EXISTS timekeeper_employee_id UUID;

CREATE INDEX IF NOT EXISTS ix_crew_supervisor ON crew (company_id, supervisor_employee_id);
CREATE INDEX IF NOT EXISTS ix_crew_timekeeper ON crew (company_id, timekeeper_employee_id);
