# Legacy HR (FoxPro/DBF) → HRMS migration

Loads the DBF snapshot produced by the legacy HR stored procedure
(`Sp_ImportHRMSData` output) into the new HRMS PostgreSQL database.

## What it migrates

| Legacy DBF        | → HRMS table(s)                                  |
|-------------------|--------------------------------------------------|
| `payresulth.dbf`  | `employee`, `contract`, `employee_document` (PERSONALNO) |
| `payresultd.dbf`  | `payroll_component` (master), `contract_pay_item`        |
| `dependants.dbf`  | `employee_dependent`                              |
| `nation.dbf`      | nationality codes (legacy 3-letter → ISO alpha-2)|
| `currency.dbf`    | reference only                                   |

## Design

- **Current-state cutover.** Only each employee's *current* active state is
  migrated. The legacy action-sheet history / cancellation / closing logic is
  **not** reproduced; the action-sheet number is preserved in
  `contract_pay_item.remarks` ("Action Sheet …") for traceability.
- **Idempotent.** Re-running with a fresh snapshot updates existing rows
  instead of duplicating, using these natural keys:
  - employee → `(company_id, employee_number = BADGE_CD)`
  - contract → `contract_number = MIG-<BADGE_CD>`
  - payroll_component → `(company_id, code = LEG<CODE>)`
  - contract_pay_item → `(contract_id, pay_component_id, effective_from)`
  - employee_document → `(employee_id, document_type, document_number)`
  - employee_dependent → `(employee_id, full_name)`
- Every row is stamped `created_by` / `updated_by = legacy-migration`.
- All data is loaded under the default company tenant
  `00000000-0000-0000-0000-0000000000c1` (override with `HRMS_COMPANY_ID`).

## Prerequisites

1. Apply the Flyway migrations first (the app does this on startup, or run
   Flyway manually). The migration tool requires schema **V1…V10**, including
   `V10__employee_dependent.sql`, which adds the dependents table plus the
   `PERSONAL_NO` document type and `RELATIONSHIP` lookups.
2. `pip install -r requirements.txt`

## Run

```bash
# 1) Preview only — reads the DBF, prints the plan, writes nothing
python legacy_migrate.py --src ../20170418 --dry-run

# 2) Load into Postgres
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/hrms
export SPRING_DATASOURCE_USERNAME=hrms
export SPRING_DATASOURCE_PASSWORD=hrms
python legacy_migrate.py --src ../20170418
```

`--src` is the folder holding the `.dbf` files (e.g. the dated snapshot
`20170418`). Connection settings fall back to `PGHOST/PGPORT/PGDATABASE/
PGUSER/PGPASSWORD` and finally to `localhost:5432/hrms`.

## Recurring use

Because you regenerate the DBF by running the legacy stored procedure yourself,
the tool is safe to re-run after each export: existing employees are updated,
new ones inserted. Run `--dry-run` first to review the plan and any warnings
(e.g. unmapped nationality codes).

## Extending nationality mapping

The legacy system uses its own 3-letter country codes (BAN, NEP, PHI, …) which
are **not** ISO 3166 alpha-3. `nationality_map.py` converts them to ISO
alpha-2. Codes not in the table are migrated as `NULL` and reported as warnings
— add them to `LEGACY_NATION_TO_ISO2` as needed.

## Verified

Tested end-to-end against PostgreSQL 16 with all ten migrations applied, using
the `20170418` snapshot (25 daily-paid employees). Both a first load and an
immediate re-run were checked: the re-run produced zero duplicates (updates /
skips only), confirming idempotency.
