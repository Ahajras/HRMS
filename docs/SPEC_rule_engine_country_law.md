# SPEC — Rule Engine (Phase 3, slice 1): Country Law reference

> Hand to Cowork: "implement docs/SPEC_rule_engine_country_law.md".
> Follow existing repo conventions: Flyway, `ddl-auto=validate` (entities must match
> columns exactly), `EffectiveDated`, the supersede pattern already used by
> `ContractPayItem`, MUI v6 + TanStack Query, multi-tenant via `TenantContext`.

## 1. Concept (grounded in FTDD Vol.1 Ch.15 + Vol.2 Ch.23)

Business logic is **never hardcoded**. Labour law lives as data in a **Rule
Package** per country (QATAR, UAE, SAUDI, …). Each company selects the country it
operates under; the system loads that package's **default rule values**, and HR can
**edit any value going forward** — an edit creates a new effective-dated version and
the previous value is retained as history (never overwritten), exactly like
`contract_pay_item` supersession.

New left-nav entry under a **Reference** group: **Country Law**. There the user
picks the active country, sees the rule values grouped by category, and edits a
value (with an effective date) to override the default forward in time.

This slice delivers the rule *store + editable country-law UI + a resolver* the
later engines (payroll, leave, overtime, EOS) will read from. The full formula
language, dependency graph, and simulation (Vol.2 §23.4–23.11) are **out of scope
for this slice** — see §6.

## 2. Data model — migration `V14__rule_engine.sql`

```sql
-- A country's labour law (or company policy) as a named package.
-- company_id NULL = global default template (seeded); a company may hold its own.
CREATE TABLE rule_package (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id    UUID,
    code          VARCHAR(30)  NOT NULL,        -- QATAR | UAE | SAUDI | COMPANY_POLICY ...
    name          VARCHAR(150) NOT NULL,
    country_code  VARCHAR(2),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    VARCHAR(100),
    updated_at    TIMESTAMPTZ,
    updated_by    VARCHAR(100),
    version       BIGINT       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_rule_package_global  ON rule_package (code) WHERE company_id IS NULL;
CREATE UNIQUE INDEX uq_rule_package_company ON rule_package (company_id, code) WHERE company_id IS NOT NULL;

-- A single configurable value within a package, effective-dated and versioned.
-- An edit supersedes: the old row gets effective_to + status INACTIVE; the new row
-- is ACTIVE from its effective_from. History is never overwritten.
CREATE TABLE rule (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id     UUID         NOT NULL REFERENCES rule_package (id),
    company_id     UUID,                          -- NULL on global defaults; set on company overrides
    code           VARCHAR(50)  NOT NULL,         -- e.g. ANNUAL_LEAVE_DAYS_UNDER_5Y
    category       VARCHAR(40)  NOT NULL,         -- LEAVE | OVERTIME | EOS | WORKING_TIME | RATE_BASE
    name           VARCHAR(150) NOT NULL,
    value_type     VARCHAR(20)  NOT NULL,         -- NUMBER | PERCENT | INTEGER | TEXT
    value_number   NUMERIC(18,4),
    value_text     VARCHAR(255),
    unit           VARCHAR(20),                   -- days | hours | % | (null)
    effective_from DATE         NOT NULL,
    effective_to   DATE,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | INACTIVE
    description    VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by     VARCHAR(100),
    updated_at     TIMESTAMPTZ,
    updated_by     VARCHAR(100),
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_rule_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);
CREATE INDEX ix_rule_package  ON rule (package_id);
CREATE INDEX ix_rule_resolve  ON rule (package_id, code, status);

-- Which package each company operates under (the selected country law).
CREATE TABLE company_rule_package (
    company_id   UUID PRIMARY KEY,
    package_code VARCHAR(30) NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by   VARCHAR(100)
);
```

### Seed the global default packages (company_id NULL)
Seed three packages (QATAR active-by-intent, UAE, SAUDI) and the QATAR rule values.
**These are editable defaults — the user must confirm them against current Qatar
Labour Law.** Use effective_from `2020-01-01`, status ACTIVE.

QATAR rules to seed (code · category · type · value · unit):
- `STANDARD_HOURS_PER_DAY` · WORKING_TIME · NUMBER · 8 · hours
- `STANDARD_DAYS_PER_WEEK` · WORKING_TIME · INTEGER · 6 · days
- `STANDARD_HOURS_PER_WEEK` · WORKING_TIME · NUMBER · 48 · hours
- `RAMADAN_HOURS_PER_DAY` · WORKING_TIME · NUMBER · 6 · hours
- `ANNUAL_LEAVE_DAYS_UNDER_5Y` · LEAVE · INTEGER · 21 · days
- `ANNUAL_LEAVE_DAYS_5Y_PLUS` · LEAVE · INTEGER · 28 · days
- `SICK_LEAVE_FULL_PAY_DAYS` · LEAVE · INTEGER · 14 · days
- `OT_NORMAL_MULTIPLIER` · OVERTIME · PERCENT · 125 · %
- `OT_RESTDAY_MULTIPLIER` · OVERTIME · PERCENT · 150 · %
- `OT_HOLIDAY_MULTIPLIER` · OVERTIME · PERCENT · 150 · %
- `EOS_WEEKS_PER_YEAR` · EOS · NUMBER · 3 · weeks
- `EOS_MIN_SERVICE_YEARS` · EOS · NUMBER · 1 · years
- `UNPAID_DAY_DIVISOR` · RATE_BASE · INTEGER · 30 · days
- `OVERTIME_RATE_BASE_DIVISOR` · RATE_BASE · INTEGER · 30 · days

> Add a `description` on each (short, e.g. "Annual leave for service under 5 years").
> Seed UAE/SAUDI packages as empty shells (name + country only) for now.

## 3. Backend (`com.hrms.rule` module)

Mirror the `reference`/`employee` module style exactly.

- `domain/RulePackage.java` (extends AuditableEntity) — fields per table.
- `domain/Rule.java` (extends AuditableEntity, implements `EffectiveDated`) — fields per table.
- `dto/RulePackageDto.java`, `dto/RuleDto.java`.
- `repository/RulePackageRepository.java`:
  - `List<RulePackage> findByCompanyIdIsNullOrderByName()` (global templates)
  - `Optional<RulePackage> findByCompanyIdIsNullAndCode(String code)`
- `repository/RuleRepository.java`:
  - `List<Rule> findByPackageIdOrderByCategoryAscNameAsc(UUID packageId)`
  - `List<Rule> findByPackageIdAndCodeAndStatus(UUID packageId, String code, String status)`
- `repository/CompanyRulePackageRepository.java` (entity `CompanyRulePackage`, PK companyId).
- `service/RuleService.java`:
  - `getActivePackageCode()` → company's selected package (default "QATAR" if none).
  - `setActivePackage(code)` → upsert `company_rule_package`.
  - `listPackages()` → global templates.
  - `listRules(packageCode)` → all rules of the package (history included), newest first per code.
  - `createOrUpdateRule(dto)` → **supersede on change**: if an ACTIVE rule with same
    (package, code) exists and the new `effective_from` is after it, set old
    `effective_to = newFrom - 1`, status INACTIVE, then insert the new ACTIVE row
    (same pattern as `ContractPayItemService.create`). Guard: new effective_from
    must be after the current one (`BusinessRuleException` otherwise).
  - **Resolver (used by later engines):**
    `BigDecimal number(String ruleCode, LocalDate asOf)` and
    `String text(String ruleCode, LocalDate asOf)` — resolve the company's active
    package, pick the rule row for `ruleCode` whose `[effective_from, effective_to]`
    covers `asOf` and status ACTIVE; throw `ResourceNotFoundException` if missing.
    (This is the seam every future engine calls instead of hardcoding.)
- `web/RulePackageController.java` → `/api/v1/rule-packages` (GET list; GET/PUT active).
  - `GET /api/v1/rule-packages` → templates
  - `GET /api/v1/rule-packages/active` → `{ packageCode }`
  - `PUT /api/v1/rule-packages/active` body `{ packageCode }` → set company's country law
- `web/RuleController.java` → `/api/v1/rules`
  - `GET /api/v1/rules?packageCode=QATAR` → rules (with history)
  - `POST /api/v1/rules` → create/supersede
  - `PUT /api/v1/rules/{id}` → in-place correction (no supersede)
  - `DELETE /api/v1/rules/{id}`

All endpoints require auth (no extra @PreAuthorize, like other reference controllers).

## 4. Frontend

- `api/types.ts`: add `RulePackage` and `Rule` interfaces (mirror DTOs).
- `api/resources.ts`: add `rulePackageApi` (list, getActive, setActive) and `ruleApi`
  (byPackage, create, update, remove).
- **New page** `pages/CountryLawPage.tsx`:
  - A country selector (Select of packages) bound to the active package; changing it
    calls `rulePackageApi.setActive`.
  - Rules grouped by `category` (WORKING_TIME, LEAVE, OVERTIME, EOS, RATE_BASE) as
    cards; each rule shows name, current value + unit, effective_from, and an "Edit"
    that opens an inline form (new value + effective_from) → `ruleApi.create`
    (supersede). A "History" toggle per rule lists prior versions (faded), reusing the
    timeline style from `PayItemsPanel`.
  - A note: "Values are the country default; edits apply forward and keep history."
- Routing + left nav: add a **Reference** group containing **Country Law**
  (route e.g. `/country-law`). Match the existing nav/router setup in `App`/layout.

## 5. Acceptance checks
1. `cd frontend && npx tsc --noEmit` clean; backend entities match V14 (validate passes).
2. Reference → Country Law shows Qatar defaults grouped by category.
3. Editing `OT_RESTDAY_MULTIPLIER` to 160 effective next month → old 150 moves to
   history, new 160 is current; resolver returns 150 for today and 160 for next month.
4. Switching active country updates which package the page shows.

## 6. Out of scope (next slices, Vol.2 Ch.23)
Formula/expression language, cross-rule dependency resolution, conditional rules
(IF/THEN decision tables), the full 8-level override hierarchy (Global→…→Employee),
rule simulation/what-if, and approval of rule changes. This slice ships the
**value store + country-law editing + a point resolver**; later slices add the
expression engine that consumes them.
