-- Seed configurable dropdowns used by the employee form.
-- Values are global lookup defaults; admins can edit/add rows in Lookup/Reference data later.

INSERT INTO lookup_value (company_id, category, code, label, sort_order, status)
SELECT NULL, 'AIRPORT', code, code, 100 + row_number() OVER (ORDER BY code), 'ACTIVE'
FROM (
    SELECT DISTINCT upper(trim(work_airport_code)) AS code
    FROM employee
    WHERE nullif(trim(coalesce(work_airport_code, '')), '') IS NOT NULL

    UNION

    SELECT DISTINCT upper(trim(home_airport_code)) AS code
    FROM employee
    WHERE nullif(trim(coalesce(home_airport_code, '')), '') IS NOT NULL

    UNION

    SELECT DISTINCT upper(trim(from_airport_code)) AS code
    FROM ticket_fare
    WHERE nullif(trim(coalesce(from_airport_code, '')), '') IS NOT NULL

    UNION

    SELECT DISTINCT upper(trim(to_airport_code)) AS code
    FROM ticket_fare
    WHERE nullif(trim(coalesce(to_airport_code, '')), '') IS NOT NULL
) src
WHERE NOT EXISTS (
    SELECT 1
    FROM lookup_value lv
    WHERE lv.company_id IS NULL
      AND lv.category = 'AIRPORT'
      AND lv.code = src.code
);

INSERT INTO lookup_value (company_id, category, code, label, sort_order, status)
SELECT NULL, 'JOB_TITLE', code, label, 100 + row_number() OVER (ORDER BY label, code), 'ACTIVE'
FROM (
    SELECT DISTINCT ON (code)
           code,
           label
    FROM (
        SELECT
            upper(trim(coalesce(nullif(job_title_code, ''), regexp_replace(coalesce(job_title, ''), '[^A-Za-z0-9]+', '_', 'g')))) AS code,
            trim(job_title) AS label
        FROM employee
        WHERE nullif(trim(coalesce(job_title, '')), '') IS NOT NULL
    ) raw
    WHERE nullif(code, '') IS NOT NULL
    ORDER BY code, label
) src
WHERE NOT EXISTS (
    SELECT 1
    FROM lookup_value lv
    WHERE lv.company_id IS NULL
      AND lv.category = 'JOB_TITLE'
      AND lv.code = src.code
);
