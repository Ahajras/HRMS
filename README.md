# HRMS — Workforce Management & Payroll System

Enterprise Workforce Management & Payroll platform for organisations with 70,000+ employees, multi-company and multi-country, built per the Functional & Technical Design Document (FTDD Volume 1 + Volume 2).

This repository is the implementation. It is built **incrementally** — each phase produces a working, compiling slice.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3, Spring Data JPA, Spring Security, Spring Batch |
| Database | PostgreSQL 16, Flyway migrations |
| Cache / sessions | Redis |
| Messaging | RabbitMQ |
| Object storage | MinIO (documents, payslips) |
| Frontend | React 18, TypeScript, Vite, Material UI, MUI X DataGrid, TanStack Query |
| Build | Maven (backend), npm/Vite (frontend), Docker Compose (infra) |

## Architecture

A **modular monolith** organised by bounded context (DDD). Each module owns its domain, repositories, services, and REST surface. Cross-module access goes through service interfaces, never directly into another module's tables.

```
hrms/
├── backend/                 Spring Boot application (modular monolith)
│   └── src/main/java/com/hrms/
│       ├── common/          base entities, tenant context, error handling, web wrappers
│       ├── reference/       countries, currencies, calendars
│       ├── organization/    configurable org hierarchy tree
│       ├── employee/        employees, contracts, assignments
│       └── payroll/         payroll components (master data; engine in later phases)
├── frontend/                React + TypeScript + MUI
├── infra/                   docker-compose for local infrastructure
└── docs/                    FTDD references
```

## Build roadmap (10 phases)

| Phase | Module | Status |
|---|---|---|
| Foundation | Repo, infra, backend skeleton, DB baseline, React shell | Done |
| 1 | Employee + Organization + Master Data | Done |
| 2 | Security + Authentication | Done |
| 3 | Rule Engine | Planned |
| 4 | Timesheet | Planned |
| 5 | Leave | Planned |
| 6 | Overtime | Planned |
| 7 | Financial Transactions | Planned |
| 8 | Payroll | Planned |
| 9 | Provision | Planned |
| 10 | Settlement | Planned |

## Current status / continue here

**Done:** Foundation, Phase 1 (Employee + Organization + Master Data), Phase 2 (Security + Auth). Deployed and running on the VPS (port 82) via `docker-compose.prod.yml`.

**Seeded logins:** `manager / Admin@123` (company admin — use for normal work, no Company ID box) and `admin / Admin@123` (platform super-admin). Change passwords after first login.

**Recent fixes:** seeded a real company + `manager` user (V7) so no UUID typing; org-level dropdown now shows global defaults; audit author now stores the username (was overflowing `created_by` VARCHAR(100) and causing 409/500 on every authenticated create).

**Next step:** Phase 3 — Rule Engine. This is the prerequisite for hourly/absence/lateness deductions (monthly staff keep a FIXED base; absence is a separate FORMULA deduction = hours × derived hourly rate, fed by Timesheet in Phase 4).

**Deploy workflow:** edit on PC → `git add -A && git commit && git push` → on server `cd /opt/hrms && bash deploy.sh`. See `SERVER.md` for ops commands.

## Running locally

### 1. Start infrastructure
```bash
cd infra
docker compose up -d
```
This starts PostgreSQL (5432), Redis (6379), RabbitMQ (5672 / mgmt 15672), MinIO (9000 / console 9001).

### 2. Run the backend
```bash
cd backend
./mvnw spring-boot:run
```
Flyway applies migrations automatically on startup. API base: `http://localhost:8080/api`.

### 3. Run the frontend
```bash
cd frontend
npm install
npm run dev
```
App: `http://localhost:5173`.

## Authentication (Phase 2)

All endpoints except `POST /api/v1/auth/login` and `/actuator/health` require a JWT bearer token. Log in to obtain one; the token carries the user's company (tenant) and permissions, so the tenant is derived from the authenticated principal rather than a client header. Platform/super-admin accounts (no company) may still target a company via the `X-Company-Id` header.

Seeded accounts (change passwords after first login): **`manager` / `Admin@123`** — a company administrator tied to a real company; use this for normal work (no Company ID field). **`admin` / `Admin@123`** — a platform super-admin not tied to any company (shows the Company ID field for cross-company work). Set `HRMS_JWT_SECRET` (≥ 32 chars) in production.

| Resource | Endpoints |
|---|---|
| Auth | `POST /auth/login`, `GET /auth/me` |
| Users | `GET/POST /users`, `GET/PUT/DELETE /users/{id}` (perm: `security.user.*`) |
| Roles | `GET/POST /roles`, `GET /roles/permissions`, `GET/PUT/DELETE /roles/{id}` (perm: `security.role.*`) |

## Phase 1 API surface

All endpoints are under `/api/v1`. For platform admins, company scoping is supplied via the `X-Company-Id` header (set in the top-bar field in the UI); for company users it comes from the login token.

| Resource | Endpoints |
|---|---|
| Currencies | `GET/POST /currencies`, `GET/PUT/DELETE /currencies/{id}` |
| Countries | `GET/POST /countries`, `GET/PUT/DELETE /countries/{id}` |
| Calendars | `GET/POST /calendars`, `GET/PUT/DELETE /calendars/{id}` |
| Org levels | `GET/POST /org-unit-types`, `GET/PUT/DELETE /org-unit-types/{id}` |
| Org units | `GET/POST /organization-units`, `GET /organization-units/tree`, `GET/PUT/DELETE /organization-units/{id}` |
| Employees | `GET/POST /employees` (paged), `GET/PUT/DELETE /employees/{id}` |
| Employee documents | `GET /employee-documents?employeeId=`, `POST`, `GET/PUT/DELETE /employee-documents/{id}` |
| Employee bank accounts | `GET /employee-bank-accounts?employeeId=`, `POST`, `GET/PUT/DELETE /employee-bank-accounts/{id}` |
| Contracts | `GET /contracts?employeeId=`, `POST /contracts`, `GET/PUT/DELETE /contracts/{id}` |
| Contract pay items | `GET /contract-pay-items?contractId=`, `POST` (auto-supersedes prior active), `GET/PUT/DELETE /contract-pay-items/{id}` |
| Lookups | `GET /lookups?category=` (configurable dropdown source) |
| Banks | `GET/POST /banks`, `GET/PUT/DELETE /banks/{id}` |
| Assignments | `GET /assignments?employeeId=`, `POST /assignments`, `GET/PUT/DELETE /assignments/{id}` |
| Pay components | `GET /payroll-components?category=`, `POST`, `GET/PUT/DELETE /payroll-components/{id}` |

## Configuration-first principle

Per the FTDD, **no business calculation is hardcoded**. Countries, currencies, organisation levels, payroll components, proration conventions, and (from Phase 3) all calculation rules are configuration. Default configuration ships with sensible values (UAE/GCC as the first country, CALENDAR_30 proration, tiered EOS) which can be changed without code changes.
