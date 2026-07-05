alter table if exists timesheet_day
    add column if not exists leave_request_id uuid references leave_request(id);
