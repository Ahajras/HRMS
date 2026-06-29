-- =====================================================================
-- V21  Employee: supervisor (for future timesheet/leave approval) + photo
-- =====================================================================
ALTER TABLE employee ADD COLUMN supervisor_employee_id UUID REFERENCES employee (id);
ALTER TABLE employee ADD COLUMN photo_url TEXT;
