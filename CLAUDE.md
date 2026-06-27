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
- P3 — Rule Engine — **NEXT / not started** (do not start without user's go-ahead)
- P4 — Timesheet / Shift (actual worked hours come from here)
- P5 — Leave
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

**Phase 3 — Rule Engine** (FTDD Vol.1 Ch 15 + our spec in Vol.2 Ch 23).

Why it's next: it's the configuration-first heart of the system. Every later
engine (overtime eligibility, proration, daily-vs-monthly pay, deductions,
settlement) reads its rules from here instead of hardcoding them — that's the
whole "no hardcoded business logic" principle.

Before coding P3, read `docs/FTDD_Volume_2.md` Chapter 23. The build will need:
the rule/formula language (23.4), evaluation order & dependency resolution (23.5),
override hierarchy (23.6), conflict resolution (23.7), versioning & activation
(23.8–23.9), and rule testing/simulation (23.10–23.11). Canonical business rules
are in 23.12; worked examples in 23.13.

When the user says go: write the V12+ migration(s) for rule/formula tables, the
`com.hrms.rule` (or payroll-rule) domain entities matching them exactly, repos,
the evaluation service, REST endpoints, then a frontend admin screen to author
and simulate rules. Same pattern as every prior phase.

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
  (common, reference, organization, employee, payroll, security, migration).
  Entities live in the `domain` sub-package.
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

### Sample snapshot note
In the current legacy snapshot most columns are empty (gender, DOB, residence,
etc.); `payresulth.dbf` (25 rows, ~75 cols) is the real employee file. Only ~6
populated fields were missing before V11. Code now reads all meaningful fields, so
future exports populate automatically.

---

## Gotchas (learned the hard way)

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
