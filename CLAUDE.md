# HRMS — Project Memory / دليل المشروع

> This file is the shared memory for the HRMS project. It travels with the repo
> via git, so opening this folder from **any machine** (work or home) gives full
> context. Read it first at the start of every session.
>
> هذا الملف هو ذاكرة المشروع. ينتقل مع git، فأي جهاز تفتح فيه المجلد ستجد كل
> المعلومات. اقرأه أول كل جلسة.

---

## How to resume on another machine / كيف تكمل من جهاز ثاني

1. On the new machine: `cd "<repo path>" && git pull`
2. Open this folder in Cowork.
3. Tell me: "اقرأ CLAUDE.md وكمّل من وين وقفنا" — I read this file and we continue.
4. To save progress before switching machines: commit & push (see Deploy below).

**Everything important must be committed to git** — chat history does NOT transfer
between machines, but committed files do.

---

## What this project is

Enterprise HRMS — Workforce Management & Payroll. Scale target: 70,000+
employees, multi-company, multi-country. Built incrementally in **10 phases**,
**configuration-first** (no hardcoded business logic — rules live in data/config).

### Phase roadmap & status
- Foundation — **Done** (repo, docker-compose, Spring Boot skeleton, React shell, Flyway baseline)
- P1 — Employee + Org + Master Data — **Done**
- P2 — Security + Auth (JWT) — **Done**
- Legacy import (DBF → Postgres) — **Done**, expanded to full employee file
- P3 — Rule Engine — **Done** (V14 + entities + resolver + CountryLawPage; Qatar law deployed)
- P4 — Timesheet / Shift / Time Type / **Calendar & Period** — **Done** (V16+V17; full backend + frontend; source of actual worked hours)
- P5 — Leave — **NEXT / not started** (do not start without user's go-ahead)
- P6 — Overtime
- P7 — Financial Transactions
- P8 — Payroll
- P9 — Provision
- P10 — Settlement

---

## Design documents (the full blueprint) — in `docs/`

All committed to the repo so they travel between machines. Read these to know the
*detailed* design behind each phase, not just where we stopped.

- `docs/FTDD_Volume_1.pdf` — original spec. **22 engine chapters** (the design of
  every module we're building).
- `docs/Architecture_Review_Report.md` — our 8-perspective review of Vol.1
  (chapter-by-chapter findings + ranked remediation roadmap + open decisions the
  client must answer). Read **PART C** for what must be resolved before building.
- `docs/FTDD_Volume_2.{md,docx}` — the **10 deep technical specs we authored**
  (Chapters 23–32) that fill the gaps the review found.

### The 22 engine modules (FTDD Vol.1) → which phase builds them
| Ch | Engine / module | Built in phase |
|----|-----------------|----------------|
| 1  | System Vision | (context) |
| 2  | Business Domain Model | P1 ✅ |
| 3  | Timesheet Engine | P4 |
| 4  | Calendar & Shift Engine | P4 |
| 5  | Time Type Engine | P4 |
| 6  | Payroll Component Engine | P8 |
| 7  | Payroll Engine | P8 |
| 8  | Leave Management Engine | P5 |
| 9  | Overtime Management Engine | P6 |
| 10 | Provision Management Engine | P9 |
| 11 | Additions & Deductions (Financial Transactions) | P7 |
| 12 | Final Settlement Engine | P10 |
| 13 | Reporting & Analytics Engine | cross-cutting |
| 14 | Integration & Export Engine | cross-cutting |
| 15 | Business Rule Engine | **P3 (NEXT)** |
| 16 | System Architecture & Technical Design | Foundation ✅ |
| 17 | System Data Architecture & Domain Model | P1 ✅ |
| 18 | Database Design Principles | Foundation ✅ |
| 19 | Database Schema Design | per-phase |
| 20 | Physical Database Design | per-phase |
| 21 | Database Implementation Blueprint | per-phase |
| 22 | System Administration, Security & Platform Mgmt | P2 ✅ |

### The 10 specs we authored (FTDD Vol.2, Ch 23–32)
23 Rule Engine · 24 Payroll Calculation · 25 Data Migration & Cutover ·
26 Multi-Currency · 27 Tax & Statutory · 28 Accounting & GL Integration ·
29 Disaster Recovery & HA · 30 QA / Parallel Payroll / Reproducibility ·
31 Security Deep Design · 32 Enterprise Operational Guidelines.

---

## ▶ THE NEXT STEP (do not start without the user's go-ahead)

**Phase 5 — Leave Management Engine** (FTDD Vol.1 Ch 8).

P3 (Rule Engine) and P4 (Timesheet/Shift/Time Type) are both **done**. P4 is the
source of actual worked hours and absences; Leave will consume those plus the
Rule Engine (accrual rates, entitlements). Same build pattern as every phase:
migration → entities → repos → service → controllers → frontend types/api/pages,
then update this file + deploy.

### P4 Timesheet — key files (done)
- Migration: `backend/.../db/migration/V16__timesheet.sql` (tables: shift,
  public_holiday, time_type, timesheet, timesheet_day; seeds for company c1 —
  DAY shift + 6 time types REGULAR/OVERTIME/REST/HOLIDAY/ABSENCE/LEAVE).
- Domain: `com.hrms.timesheet.domain` — Shift, PublicHoliday, TimeType,
  Timesheet, TimesheetDay (plain UUID FKs, no @OneToMany; days via repo).
- Service: `TimesheetService` (generate classifies each day REST/HOLIDAY/REGULAR;
  recompute worked/OT from clock punches; lifecycle DRAFT→SUBMITTED→APPROVED→
  LOCKED, reopen→DRAFT). Plus ShiftService/TimeTypeService/PublicHolidayService.
- REST: `/api/v1/shifts`, `/time-types`, `/public-holidays`, `/timesheets`
  (GET ?year&month, GET/{id}, POST /generate, PUT/{id}/days, POST/{id}/{submit|
  approve|lock|reopen}, DELETE/{id}).
- Frontend: types in `types.ts`; `shiftApi/timeTypeApi/publicHolidayApi/
  timesheetApi` in resources.ts; pages TimesheetPage, ShiftsPage, TimeSetupPage;
  routes in App.tsx + nav in AppLayout.tsx (Timesheets/Shifts/Time Setup).
- Boundary: Shift CLASSIFIES the day; Rule Engine owns the RATES. Single
  project/cost-code per day (defaults from latest assignment).

### P4 Calendar & Period engine (done — V17)
- Migration `V17__payroll_calendar.sql`: tables `payroll_calendar`,
  `payroll_period` (OPEN→LOCKED→CLOSED), `payroll_week`, `employee_shift`
  (roster, effective-dated); ALTER `timesheet` ADD `period_id`; seeds DEFAULT
  monthly calendar (week_start SAT) for company c1.
- Domain/repos/dto: PayrollCalendar, PayrollPeriod, PayrollWeek, EmployeeShift.
- Services: **PayrollCalendarService** (calendar CRUD + `resolve`),
  **PayrollPeriodService** (`generateYear` → 12 periods + weeks; lock/close/reopen;
  delete guarded if timesheets exist), **EmployeeShiftService** (roster CRUD).
- REST: `/api/v1/payroll-calendars`, `/payroll-periods` (GET ?year, GET/{id},
  POST /generate?year[&calendarId], POST/{id}/{lock|close|reopen}, DELETE/{id}),
  `/employee-shifts` (GET ?employeeId, POST, PUT/{id}, DELETE/{id}).
- TimesheetService.generate now takes **periodId** (must be OPEN); derives
  year/month from the period; resolves shift from roster (employee_shift) when
  none passed, else company default. Timesheet carries period_id.
- Frontend: types Payroll{Calendar,Period,Week}, EmployeeShift; calendarApi/
  periodApi/employeeShiftApi; pages **CalendarPage** (init year + lock/close/reopen
  + weeks), **RosterPage** (assign employees→shifts), TimesheetPage now picks a
  **Period** (no year/month dropdowns). Nav grouped (AppLayout NAV_GROUPS):
  Workforce / Time & Attendance / Configuration / Administration.
- ⚠️ **Bean-name clash gotcha:** `com.hrms.reference` already has a
  `CalendarService` + `CalendarController`. The timesheet ones were named
  **PayrollCalendarService / PayrollCalendarController** to avoid duplicate Spring
  bean names. The old `timesheet/web/CalendarController.java` and
  `timesheet/service/CalendarService.java` are now deprecated empty stubs (couldn't
  delete from sandbox — **delete these two files on the PC** when convenient).

### P4 legacy-parity additions (done — V18) — from CCC old timesheet manual
- Migration `V18__sample_week_day_hours.sql`:
  - `shift_day` = **sample week** per shift (per day-of-week: normal_hours,
    declared_ot, weekly_off). This is the legacy PAYCAL per-day calendar.
  - ALTER `timesheet_day` ADD `normal_hours`, `declared_ot_hours`,
    `undeclared_ot_hours` (declared-vs-undeclared OT split, legacy nDec/nUndec).
  - `timesheet_day_cost` = split a day's hours across cost codes (legacy PAYIN
    HR_CC1..HR_CC8). Empty ⇒ the day's own project/cost is the single target.
  - seeds time types **SICK / ACCIDENT / RR / UNPAID** (legacy CM S/A/R/U) +
    sample week for the DAY shift (Sat–Thu 8h+2h OT, Fri off).
- Entities: ShiftDay, TimesheetDayCost; TimesheetDay gained the 3 hours columns.
- ShiftService now manages the sample week (replace-all on save, keeps the
  `weekly_off` CSV in sync). TimesheetService.recomputeDay reads the sample week:
  normal = min(worked, sampleNormal); OT = worked−sampleNormal; declared =
  min(OT, day's declaredOt); undeclared = remainder. Non-working categories
  (ABSENCE/LEAVE/SICK/ACCIDENT/RR/UNPAID) ⇒ 0 worked/OT. saveDays persists costs.
- **Bulk roster assign**: `EmployeeShiftService.bulkAssign` + POST
  `/employee-shifts/bulk` { shiftId, effectiveFrom, employeeIds[] } (skips
  employees already open-ended on that shift). Frontend RosterPage has a
  multi-select "Bulk assign (fast)" panel.
- Frontend: ShiftsPage sample-week table; TimesheetPage shows
  Normal/Decl/Undecl columns + per-day "Cost split" expander.

### P4 Crew + Timekeeper scoping (slice 1 done — V19) — new `com.hrms.crew` context
- Migration `V19__crew.sql`: `crew` (company_id, code, name, project_id,
  foreman_employee_id, parent_crew_id, status), `crew_member` (crew_id, employee_id,
  **shift_id** so one crew can split across shifts, effective-dated),
  `timekeeper_project` (employee_id ↔ project_id, many-to-many).
- Entities/repos/dtos under `com.hrms.crew.*`; plain-UUID FKs.
- **CrewService**: crew CRUD + members (add / **bulkAddMembers** / remove); enriches
  project code, foreman name, member count. **TimekeeperService**: assign/list
  timekeeper→projects + `allowedProjectIds(employeeId)` helper (for slice-2 enforcement).
- REST: `/api/v1/crews` (+ `/{id}/members`, `/{id}/members/bulk`, `/members/{id}`),
  `/api/v1/timekeeper-projects`.
- Frontend: **CrewsPage** (CRUD + expandable members panel w/ bulk add + search),
  **TimekeepersPage** (assign employee→project). Nav under Workforce.
- ⚠️ Cost-split validation added to TimesheetService.saveDays: when a day is split
  across cost codes, the split must equal the worked hours (rejects + clear error;
  frontend shows error + live Σ/worked indicator).

### P4 Crew slice 2 (partly done)
- **DONE — Timekeeper project scope enforced (backend):** TimesheetService resolves
  the current user's employeeId (AppUserRepository via AuthenticatedUser.userId) →
  TimekeeperService.allowedProjectIds. If the user is a timekeeper (≥1
  timekeeper_project rows) listByPeriod/generate/generateBulk/generateByCrew are
  restricted to those projects (employee's project from latest assignment). Empty ⇒
  unrestricted (admins/managers). `restrictedProjects()/employeeProject()/assertProjectAllowed()`.
- **DONE — Generate by crew:** `generateByCrew(crewId, periodId)` → each member on
  their own crew shift. POST `/timesheets/generate-by-crew?crewId&periodId`.
  Frontend TimesheetPage has a Crew picker + "Generate for crew".
- **DONE — Crew code unique per project** (V20: uq on company_id+project_id+code);
  crew member candidate list filtered to the crew's project.
- **DONE — Global API error toast:** client.ts dispatches `api-error`; AppLayout
  Snackbar shows every backend error message (was: most forms swallowed errors).
- **NEXT (slice 2b):** Crew **trades** (job title planned vs assigned, red/green);
  **Timecard report** (per-employee monthly card, printable/PDF).

### Employee card polish (done — V21)
- V21: `employee` ADD `supervisor_employee_id` (FK employee, for future
  timesheet/leave approval) + `photo_url` TEXT (stores a data-URI avatar).
- Employee entity/EmployeeDto/EmployeeService updated (+ derived `supervisorName`).
  GET `/crews/by-employee/{employeeId}` returns the employee's current crew (code +
  foreman) — shown read-only on the employee card.
- EmployeesPage dialog: gradient header with **avatar + photo upload**, name, and
  colored chips (Crew = purple, Supervisor = teal, status green/red). Personal tab
  gained a **Crew & Supervisor** section (supervisor dropdown + read-only crew/foreman)
  before Contact & Address. Crew membership itself is still managed in the Crews screen.

### ▶ Still NOT built from the legacy timesheet (deferred — confirm before doing)
- **VAKHTA** rotation engine (28/28 rotations, field-break F days, employee
  exceptions: recall/extension/shift-change/leave/engagement, back-to-back crews).
  (The crew hierarchy basics now exist; this is the rotation depth on top.)
- Full **hours breakdown for payroll** (weekend-normal Hr_fri, holiday-normal
  Hr_hol, sick/accident/leave/RR/unpaid split, totals per legacy Ch.5) — belongs
  to P6 Overtime + P8 Payroll, driven by the Rule Engine rates.
- Excel/Papyrus import, manhour reports, asset/PMV billing.

---

## Stack

- **Backend:** Java 21, Spring Boot 3, Spring Data JPA, PostgreSQL 16, Flyway,
  Hibernate (`ddl-auto=validate`), Spring Security 6 + JWT, Maven.
- **Frontend:** React 18 + TypeScript + Vite + MUI v6 + @mui/x-data-grid +
  TanStack Query + axios + react-router-dom.
- **Infra:** Docker Compose. Public app on **port 82**.

### Critical conventions
- `ddl-auto=validate` → **entity columns MUST exactly match Flyway migrations.**
  When adding a field: write the migration AND the entity column together.
- Modular monolith by DDD bounded context: `com.hrms.<context>`
  (common, reference, organization, employee, payroll, security, migration,
  rule, timesheet). Entities live in the `domain` sub-package.
- **Multi-tenancy:** company-scoped via `TenantContext` (ThreadLocal<UUID>).
  JWT carries `cid`; platform admins pass `X-Company-Id`.
  Seeded company UUID: `00000000-0000-0000-0000-0000000000c1`.

### Seeded logins (CHANGE before production)
- `manager` / `Admin@123` — company admin (no Company ID box).
- `admin` / `Admin@123` — platform super-admin.
- **Before production:** change seeded passwords AND `HRMS_JWT_SECRET` (≥32 chars).
- **Hard constraint from the user:** never use a fake UUID
  (`11111111-...`) in a real handed-over system — use the `manager` login.

---

## Deploy workflow (every time, same steps)

**On the PC (push):**
```
cd "D:\hajras projects\HRMS"
git add -A
git commit -m "<message>"
git push
```

**On the server (deploy):**
```
cd /opt/hrms
./deploy.sh          # if "Permission denied": bash deploy.sh
```

`deploy.sh` does: git fetch + `reset --hard origin/main` +
`docker compose -f docker-compose.prod.yml up -d --build`.
The server builds the backend **inside Docker** (Maven+JDK21 in container) — so
**local Maven is NOT needed.** Only build the frontend locally if testing UI.
Flyway runs migrations automatically on startup.

---

## Legacy import (DBF → HRMS)

Admin screen "Import from Legacy System". Uploads legacy FoxPro/DBF files (zip or
individual .dbf). Preview (dry-run) + Import (commit). **Idempotent** — re-importing
a fresh export updates rather than duplicates. The legacy system stays running, so
imports are repeatable.

### Idempotent natural keys
- employee = (company_id, employee_number = BADGE_CD)
- contract = "MIG-<BADGE_CD>"
- component = (company_id, code = "LEG<CODE>")
- pay item = (contract, component, effective_from)
- document = (employee, type, number)
- dependent = (employee, full_name)

### Fields imported (V11 expansion)
- **Employee:** job title (+code), pay status (DAILY/MONTHLY PAID), Arabic name,
  marital status, gender, DOB, termination date, status.
- **Documents:** work permit (issue/expiry/authority), national ID, residence
  permit, social security, personal no — each as its own document row.
- **Contract:** real contract type, end date, currency, overtime category (+desc),
  and **working hours/days per week = REFERENCE ONLY**.
- ⚠️ **Working hours/days are reference (نظري) only.** Actual worked hours come
  from the Timesheet/Shift module (Phase 4). Labeled as such in migration, entity,
  TS types, and UI.

### Key files
- Migration: `backend/src/main/resources/db/migration/V11__legacy_employee_fields.sql`
- Entities: `backend/.../employee/domain/Employee.java`, `Contract.java`
- Service: `backend/.../migration/service/LegacyImportService.java`
- DBF reader: javadbf 1.14.1 (Java); dbfread (Python, for analysis)
- Frontend types: `frontend/src/api/types.ts`; UI: `frontend/src/pages/EmployeesPage.tsx`

### Raw legacy snapshot (V12) — every field has a place
- `legacy_employee_raw` table (V12): keeps the FULL legacy row per employee —
  header (payresulth) + all detail lines (payresultd) — as **JSONB**, every
  column preserved even when blank (future exports may populate them). Archive
  only; engines still read the normalized tables.
- Entity `com.hrms.migration.domain.LegacyEmployeeRaw` (`@JdbcTypeCode(SqlTypes.JSON)`),
  repo `LegacyEmployeeRawRepository`, upsert in `LegacyImportService.upsertRaw`
  (idempotent on company_id+employee_number; values normalized to display strings).
- REST: `GET /api/v1/legacy-import/raw/{employeeId}` → `LegacyRawDto` (header map + detail list).
- UI: **"Legacy Data" tab** in EmployeesPage (header key/value grid + detail DataGrid).
  `legacyRawApi` in resources.ts, `LegacyRaw` type in types.ts.

### Sample snapshot note
In the current legacy snapshot most columns are empty (gender, DOB, residence,
etc.); `payresulth.dbf` (25 rows, ~75 cols) is the real employee file. Only ~6
populated fields were missing before V11. Code now reads all meaningful fields, so
future exports populate automatically.

---

## Gotchas (learned the hard way)

- **Adding an entity field is NOT enough — there is a DTO layer.** The REST API
  returns `*Dto` objects, mapped in each `*Service` (`apply()` + `toDto()`). When
  you add a column/entity field you MUST also add it to the matching DTO and BOTH
  mapper methods, or the data saves to the DB but never reaches the UI (it looks
  like "the import didn't populate it" when really it's hidden by the DTO).
  Example: V11 legacy fields imported fine but showed blank until
  `EmployeeDto`/`ContractDto` + `EmployeeService`/`ContractService` were updated.
- **Sandbox bash mount serves STALE repo files.** Use the Read/Edit tools for
  canonical content, not bash `cat`/`grep`, when verifying file contents.
- **No Maven wrapper** in backend (only pom.xml). Don't try `mvnw.cmd` on PC —
  build happens in Docker on the server.
- **PowerShell:** if `npm.ps1 cannot be loaded`, use `npm.cmd` or
  `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.
- **deploy.sh Permission denied:** `chmod +x deploy.sh` or `bash deploy.sh`.
- Can't compile backend in the sandbox (no Maven/JDK21) — verify Java statically.

---

## Open items / backlog (not started — confirm before doing)
- Phase 3 Rule Engine.
- Enforce "only one primary bank account" per employee.
- Generalize/standardize error messages.

---

## User preferences
- Communicate in **simple Arabic**; keep it concise and direct.
- User prefers practical step-by-step commands over deep technical detail.
