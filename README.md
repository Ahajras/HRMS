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

Default seeded admin: **`admin` / `Admin@123`** (change after first login). Set `HRMS_JWT_SECRET` (≥ 32 chars) in production.

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
| Contracts | `GET /contracts?employeeId=`, `POST /contracts`, `GET/PUT/DELETE /contracts/{id}` |
| Assignments | `GET /assignments?employeeId=`, `POST /assignments`, `GET/PUT/DELETE /assignments/{id}` |
| Pay components | `GET /payroll-components?category=`, `POST`, `GET/PUT/DELETE /payroll-components/{id}` |

## Configuration-first principle

Per the FTDD, **no business calculation is hardcoded**. Countries, currencies, organisation levels, payroll components, proration conventions, and (from Phase 3) all calculation rules are configuration. Default configuration ships with sensible values (UAE/GCC as the first country, CALENDAR_30 proration, tiered EOS) which can be changed without code changes.
