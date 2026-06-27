# Enterprise Architecture Review Report
## HRMS Workforce Management & Payroll System — Functional & Technical Design Document (FTDD v1.0)

**Reviewer role:** Senior Enterprise Architect
**Document under review:** *HRMS Workforce Management & Payroll System — Software Requirement Specification (SRS), Version 1.0* (264 pages, Chapters 1–22)
**Review date:** 25 June 2026
**Intended audience:** Enterprise Software Architects · Senior Backend Developers (Java / Spring Boot) · PostgreSQL Database Architects · HR & Payroll Business Analysts · Technical Leads · QA Leads
**System target:** Multi-company, multi-country workforce & payroll platform for organisations with 70,000+ employees, intended to *replace a commercial enterprise HRMS/Payroll platform*.
**Constraint honoured:** This is a review only. It contains **no code, no database table definitions (DDL), and no API definitions**. All recommendations are expressed as requirements, principles, and design directions to be resolved *before* implementation begins.

---

## How to read this report

The report is organised in three parts:

- **Part A — Executive summary and cross-cutting findings.** The issues that span multiple chapters and that, in this reviewer's judgement, must be resolved before any implementation work starts. Read this first.
- **Part B — Chapter-by-chapter review (Chapters 1–22).** A sequential walk through the document. For each chapter: its purpose, what it does well, and the specific findings raised against it.
- **Part C — Consolidated findings by review perspective, prioritised remediation roadmap, and open decisions.** The same findings re-cut along the eight requested review lenses, plus a sequenced plan and a register of decisions the business must make.

### Finding identifiers and severity

Every finding has a stable ID (e.g. **F-07-03** = Chapter 7, finding 3; **X-04** = cross-cutting finding 4) so it can be tracked in a backlog. Severity is rated:

| Severity | Meaning |
|---|---|
| **Critical** | Will cause incorrect payroll, data loss, a security/compliance breach, or a blocked go-live if not resolved. Must be fixed before build. |
| **High** | Significant correctness, scalability, or maintainability risk. Resolve before the affected module is built. |
| **Medium** | Real gap or ambiguity that will cause rework or defects if left unspecified. Resolve during detailed design of the module. |
| **Low** | Improvement, clarification, or hardening opportunity. Address opportunistically. |

Each finding is also tagged with the relevant review **perspective(s)**: *Business*, *Functional*, *Architecture*, *Database*, *Security*, *Payroll*, *Reporting/Integration*, *Maintainability*.

### Overall assessment in one paragraph

This is an unusually well-conceived requirements document. Its central architectural bet — that *every* business calculation is configuration in a versioned Rule Engine, that *nothing* is hardcoded, that history is immutable and payroll is reproducible years later, and that the database follows the domain model rather than driving it — is exactly the right foundation for a system meant to outlive a commercial product across multiple countries' labour laws. The domain decomposition is clean and the separation between "engines that produce approved transactions" and "the single Payroll Engine that turns them into money" is disciplined and correct. **However**, the document is strong on *vision and breadth* and comparatively thin on the *load-bearing mechanical details* on which payroll correctness and enterprise operability actually depend: the proration/daily-rate conventions, the rule-expression language and formula evaluation semantics, taxation and statutory social-insurance for the non-GCC target countries, the data migration / opening-balance / cutover strategy (mandatory when replacing a live payroll system), multi-currency FX handling, deduction caps and negative-net protection, and the operational hardening of the supporting infrastructure (Redis/RabbitMQ/MinIO HA, RPO/RTO, key management). There are also several concrete internal inconsistencies (most notably the concurrent-user target and the organisation-hierarchy depth). None of these are fatal; all are resolvable; but they should be closed in a "Volume 2" detailed-design pass before code generation, because the document explicitly designates itself the *single source of truth* from which the schema and services will be generated, and generation will faithfully reproduce whatever ambiguity it is given.

---

# PART A — Executive Summary & Cross-Cutting Findings

## A.1 Top issues to resolve before implementation (ranked)

1. **No data migration, opening-balance, and cutover strategy (X-01, Critical).** The system explicitly replaces a live legacy payroll platform, yet there is no chapter describing how 70,000 employees, their accrued service for End-of-Service (EOS), leave balances, loan balances, in-flight provisions, and the historical payroll needed to honour "reproduce payroll years later" are brought in. This is the single largest go-live risk.
2. **Taxation and statutory social insurance are absent (X-02, Critical).** A `Taxable` flag and a `Tax` category exist, and the Payroll Domain lists "Taxes", but no engine, rule type, or data structure defines income tax or social-insurance/pension (e.g. GOSI in KSA, Egyptian income tax and social insurance, Jordan social security). For the stated multi-country roadmap this is a correctness and compliance gap, not an enhancement.
3. **Payroll proration / daily-rate convention is unspecified and internally inconsistent (X-03, Critical).** Examples mix conventions (a monthly salary divided by 30 for unpaid leave; daily workers paid by actual days) without ever defining the canonical rule for mid-period joiners/leavers, salary changes, and unpaid absence. Proration is the most common source of payroll disputes; it must be a first-class, country-configurable rule.
4. **The Rule Engine's expression language and evaluation semantics are undefined (X-04, Critical).** Chapters 6 and 15 promise formula resolution, dependency ordering, circular-dependency detection, and a rule hierarchy with override precedence, but never define the expression grammar, the operator/precedence model, how formulas reference other components, how the 8-level hierarchy is *merged* (override vs. accumulate), or how rules are stored and evaluated. The entire "nothing is hardcoded" promise rests on this and it is the least-specified critical component.
5. **Deduction caps, negative-net protection, and minimum-wage/WPS floor handling are missing (X-05, High).** Loans, advances, fines and recoveries can exceed net pay. Labour laws cap monthly deductions; the document has no cap rule, no negative-net handling, and no WPS minimum-pay validation.
6. **Concurrent-user target contradiction (X-06, High).** §1.7 states **5,000 concurrent users**; §16.16 states **500 concurrent users** — a 10× difference that changes the entire sizing, HA, and connection-pool design. Must be reconciled.
7. **Supporting infrastructure has unaddressed single points of failure and no RPO/RTO (X-07, High).** HA is described for the app tier and PostgreSQL, but Redis, RabbitMQ, and MinIO (which carry rule-cache, payroll events, and all documents/payslips) have no HA story, and disaster recovery defines mechanisms (daily backup, PITR) but no recovery objectives.
8. **Multi-currency is asserted but FX management is undefined (X-08, High).** Components, banks, and settlements carry a currency, and "Multi Currency" is a stated capability, but there is no exchange-rate master, no rate effective-dating/source, and no rule for cross-currency consolidation or reporting.
9. **Domain model vs. physical schema mismatch on the organisation hierarchy (X-09, Medium→High).** §2.4 defines a 7-level hierarchy (Company → Business Unit → Division → Department → Section → Team → Employee); the physical/blueprint chapters (20–21) model only Business Unit, Department, Section. Division and Team are dropped silently.
10. **No test/QA, reconciliation, and "payroll reproducibility" verification strategy (X-10, High).** A QA Lead is named in the audience and reproducibility is a core promise, yet there is no testing chapter, no parallel-run/regression approach, and no defined way to *prove* a years-old payroll re-computes identically.

## A.2 Cross-cutting findings register

These findings recur across many chapters and are best tracked once, centrally, rather than repeated per chapter. The chapter-by-chapter sections reference them where they bite hardest.

**X-01 — Data migration & cutover strategy absent. (Critical · Business, Database, Payroll)**
§1.8 forbids copying the legacy DB and transfers "only business knowledge" — correct in principle, but it leaves a vacuum: there is no specification of opening balances (accrued EOS service date and amount, leave balances per type, outstanding loans/advances, YTD earnings for statutory caps), no historical-payroll backfill needed to satisfy "reproduce payroll three years later" (Principle 9–10), no mapping/cleansing/validation approach, no dual-run/parallel period, no cutover sequencing, and no rollback plan. *Recommendation:* add a dedicated Migration & Cutover chapter covering source-to-target mapping, opening-balance loaders (as configuration-driven imports, reusing the Integration Engine), a mandatory parallel-run period with variance tolerances, reconciliation sign-off gates, and a rollback/contingency plan. Decide explicitly whether historical payroll is *migrated as data* or *re-creatable from migrated inputs + historical rule packages* (the latter is far harder and probably infeasible for legacy periods).

**X-02 — Taxation & statutory social insurance engine missing. (Critical · Business, Payroll, Functional)**
The GCC core is largely income-tax-free, but the explicit roadmap includes Egypt and Jordan (income tax + social insurance) and KSA/others have GOSI-type employer/employee social contributions even where personal income tax is absent. The document models a `Taxable` flag and a tax *category* but no tax *computation*: no bracket/threshold tables, no employer-vs-employee contribution split, no statutory ceilings, no YTD cumulation, no employer-cost vs. net-pay distinction. *Recommendation:* define a Statutory Contributions & Tax sub-engine (ideally as Rule Engine rule types operating over YTD cumulative bases), with country rule packages, contribution ceilings, and separate employer-cost provisioning. Treat it as core, not "future."

**X-03 — Proration, daily-rate derivation, and rounding policy unspecified. (Critical · Payroll, Functional)**
Needed for: mid-period joiners/leavers, salary changes (§7.9), unpaid leave (§8.18 uses salary/30), daily workers (§7.7), partial-month allowances. The document never fixes whether daily rate = salary/30, salary/calendar-days, salary/working-days, or salary/scheduled-shift-days; whether allowances prorate identically to basic; or the global rounding/accumulation policy and currency minor-unit handling. These choices materially change every payslip. *Recommendation:* make proration and daily-rate derivation explicit, country-configurable Rule Engine rules; define a single rounding policy (per-component vs. per-total, half-up vs. banker's, minor-unit precision) and an accumulation order.

**X-04 — Rule Engine language & evaluation semantics undefined. (Critical · Architecture, Maintainability, Payroll)**
Undefined: the expression/condition grammar; supported data types and operators; how a formula component references other components and resolves order (§6.7/§6.9 mention priority and a formula engine but no algorithm); how the 8-level hierarchy (§15.4) is combined — does the lowest level fully override, partially override, or accumulate?; conflict resolution when two same-level rules match; how rules are persisted and evaluated (interpreted metadata vs. compiled vs. embedded engine such as Drools/MVEL/DMN); sandboxing and performance of evaluating thousands of rules over 70k employees inside a <10-minute window. *Recommendation:* select and specify a rule representation (recommend a declarative, versionable form — DMN decision tables and/or a constrained, sandboxed expression language — over free-form code), define merge/override and conflict semantics precisely, and define the formula dependency-resolution algorithm (topological sort with cycle detection) that §15.24 already implies.

**X-05 — Deduction caps, negative net, and pay floors. (High · Payroll, Functional, Business)**
No rule caps total deductions (labour laws commonly cap at a percentage of wage), no behaviour when deductions exceed net (carry-forward of unrecovered loan? partial recovery? rejection?), no minimum-wage/WPS floor validation. *Recommendation:* add configurable deduction-priority and cap rules, an unrecovered-balance carry-forward mechanism, and a pre-WPS net-pay floor validation.

**X-06 — Concurrent-user target contradiction (5,000 vs 500). (High · Architecture, Functional)** Reconcile §1.7 and §16.16; the chosen number drives app-node count, DB connection pooling, Redis sizing, and load-test acceptance criteria.

**X-07 — Infrastructure HA gaps & missing RPO/RTO. (High · Architecture)** Define HA/clustering for Redis (e.g. Sentinel/Cluster), RabbitMQ (quorum queues/mirroring), and MinIO (distributed/erasure-coded), or document the accepted degradation. Set explicit RPO/RTO and back them with the replication/PITR design; payroll and documents likely warrant RPO≈0 / low RTO.

**X-08 — Multi-currency FX handling undefined. (High · Payroll, Database, Reporting)** Add an exchange-rate master with source, rate type (spot/closing/budget), and effective dating; define the currency of record per company vs. transaction currency vs. reporting/consolidation currency, and where conversion occurs (and is frozen) in the payroll run.

**X-09 — Org-hierarchy depth inconsistency (Ch.2 vs Ch.20–21). (Medium→High · Database, Functional)** Either model all seven levels or formally reduce the hierarchy; ensure security scoping (§22.7), reporting roll-ups, and approval routing all reference the *same* agreed hierarchy.

**X-10 — No QA / reconciliation / reproducibility-verification strategy. (High · Maintainability, Payroll)** Add a test strategy: unit/rule-level tests, golden-master regression for reproducibility (re-run an archived period and assert byte-identical results), parallel-run variance reporting, performance/load tests tied to the (reconciled) concurrency and volume targets, and payroll-to-bank-to-GL reconciliation controls.

**X-11 — GDPR "right to erasure" conflicts with "never delete / keep forever." (High · Security, Database, Business)** §18.6/§17.21 mandate permanent immutable retention; §22.19 commits to "GDPR principles where applicable." These are in direct tension. *Recommendation:* define lawful-basis and retention policy explicitly (statutory payroll retention usually overrides erasure for financial records), and design crypto-shredding / PII-pseudonymisation for the narrow cases where erasure applies, rather than physical deletion.

**X-12 — Multi-tenant data isolation mechanism unspecified. (High · Security, Architecture, Database)** "Companies are completely isolated" but they share one database. The isolation mechanism (PostgreSQL Row-Level Security, mandatory company predicate enforced in a data-access guard, or schema-per-company) is not chosen. Relying on application-layer `WHERE company_id = ?` alone is a known cross-tenant-leak risk. *Recommendation:* enforce isolation at the database boundary (RLS) in addition to the service layer.

**X-13 — Bi-temporal model implied but not specified. (High · Database, Payroll)** Retroactive payroll (§7.10) plus immutable history plus effective dating effectively require *bi-temporal* data (valid-time and transaction-time). The document provides `effectiveFrom/effectiveTo` + `version` + `isDeleted` (valid-time and a coarse version), but no transaction-time axis to answer "what did we *believe* on the day we ran payroll." *Recommendation:* commit to an explicit bi-temporal pattern for payroll-affecting master data (contract, salary package, assignment, rule package, calendar) and define overlap-prevention constraints generally, not just for shift assignment.

**X-14 — Accounting/GL integration underspecified (no Chart of Accounts master, no double-entry/period-close controls). (High · Reporting/Integration, Payroll)** Provisions and payroll reference debit/credit accounts and cost centres, but there is no GL-account master data, no account-mapping configuration entity, no double-entry balancing validation, and no reconciliation/period-close handshake with the ERP. *Recommendation:* add a GL mapping master and a posting-control layer that validates balanced entries and reconciles totals to the ERP before marking a period posted.

**X-15 — Time-zone, cross-midnight, and Hijri-calendar handling. (Medium · Functional, Payroll)** Multi-timezone is a system parameter, but shift times are stored without timezone and night shifts crossing midnight (22:00→06:00) have no defined date-attribution rule for cost and OT. Ramadan auto-revert (§4.18) depends on the Hijri lunar calendar, whose dates shift yearly and are not always known in advance — yet no Hijri-calendar support is specified. *Recommendation:* store shift times with an explicit company/site timezone, define the worked-date attribution rule for cross-midnight shifts, and add Hijri-calendar handling (or an annual administrative Ramadan-window configuration) for seasonal shifts and any Hijri-based statutory dates.

**X-16 — Approval/Workflow engine depth. (Medium · Functional, Maintainability)** Workflow is "configurable" everywhere but the engine itself (parallel vs. sequential steps, conditional routing on amount/type, delegation, escalation SLAs/timers, out-of-office, re-approval on edit-after-approval, bulk approval authority) is only sketched. Because *every* module depends on it, under-specifying it propagates risk system-wide.

**X-17 — Self-service & mobile scope undefined. (Medium · Business, Functional)** "Employees (Self Service)" is a target user and mobile is "future," but no ESS capability set, permission model, or notification/consent design exists. Given 70k employees, ESS adoption materially affects load and data-entry volume assumptions.

**X-18 — Observability beyond tooling. (Low→Medium · Maintainability, Architecture)** Prometheus/Grafana/ELK are named and a correlation ID is captured, but there are no business-level SLIs/SLOs, no distributed tracing standard (e.g. OpenTelemetry), and no alerting policy for the payroll-run critical path.

---

# PART B — Chapter-by-Chapter Review

## Chapter 1 — System Vision

**Purpose.** Establishes the product vision: a configuration-driven *business platform* (not a payroll program) for 70,000+ employees across heavy industry, replacing legacy systems, with ten immutable core principles, scope, target users, supported countries, performance targets, technology, and design goals.

**Strengths.** The ten Core Business Principles are the document's backbone and are excellent: nothing hardcoded, unlimited everything, reproducibility (Principle 9), and immutable history (Principle 10). The "answer one business question" framing (§1.10) is a clarifying north star. The layered model (§1.12) correctly forbids business logic in SQL.

**Findings.**

- **F-01-01 (High · Architecture/Functional) — Concurrent-user contradiction.** §1.7 specifies **5,000 concurrent users**; §16.16 specifies **500**. See **X-06**. This is not cosmetic: it changes node count, pool sizing, and load-test pass criteria by an order of magnitude. *Recommendation:* pick one, justify it from realistic peak-day behaviour (payroll close + month-end timesheet entry), and define separately *named users* vs *concurrent active sessions* vs *concurrent heavy operations*.
- **F-01-02 (Critical · Business/Payroll) — Reproducibility promise has no migration counterpart.** Principles 9–10 promise that a payroll run from three years ago reproduces identically using the historical rule package. For periods that predate this system (i.e. all legacy history at go-live), that is only possible if either the historical *inputs and rule packages* are migrated, or historical results are migrated as immutable records. Neither is specified. See **X-01**. *Recommendation:* explicitly scope reproducibility as "from first live period onward," and define migrated legacy history as read-only archived records not subject to the recomputation guarantee.
- **F-01-03 (High · Business) — "Each engine is completely independent" needs an architectural definition.** §1.4 says engines are completely independent and communicate only through business services. Taken literally this suggests microservices; the rest of the document (§16) describes a modular monolith. The independence model (module boundaries inside one deployable, with in-process service interfaces and a shared database) should be stated plainly to avoid the team over-engineering toward distributed services prematurely. *Recommendation:* declare the target as a **modular monolith with strict bounded-context boundaries and an internal event bus**, with a documented path to extract services later if needed.
- **F-01-04 (Medium · Business) — Supported-countries list implies statutory scope that later chapters do not cover.** §1.6 commits to Qatar now and UAE/KSA/Oman/Kuwait/Bahrain/Egypt/Jordan via rule packages "with no software modification." Egypt and Jordan in particular bring income tax and social insurance that the engines do not model (see **X-02**). *Recommendation:* qualify the "no software modification" claim — new *labour-law rules* need no code, but new *statutory mechanisms* (tax brackets, social-insurance ceilings, Hijri dates) require the corresponding engine capabilities to exist first.
- **F-01-05 (Low · Architecture) — Technology list is sound but commits early.** Java 21 / Spring Boot / PostgreSQL / Redis / RabbitMQ / React are appropriate. Note the document lists both "future GraphQL" and "REST" and both Docker and "Kubernetes (future)"; ensure the MVP target runtime is pinned so non-functional testing has a fixed baseline.

## Chapter 2 — Business Domain Model

**Purpose.** Defines the business-first domain decomposition (Company, Organisation, Employee, Contract, Project, Assignment, Calendar, Payroll Period, Shift, Attendance, Timesheet, Cost Allocation, Time Types) before any technical design.

**Strengths.** The modelling instinct is correct and mature: assignments (not permanent links) between employees and projects; weekly-off belonging to Shift not Calendar; cost allocations as unlimited independent transaction records explicitly rejecting the legacy CC1..CCn anti-pattern (§2.16); employees never physically deleted (§2.5). This chapter alone demonstrates strong domain understanding.

**Findings.**

- **F-02-01 (Medium→High · Database/Functional) — Seven-level org hierarchy not carried into the schema.** §2.4 defines Company → Business Unit → Division → Department → Section → Team → Employee. Chapters 20–21 model only BusinessUnit, Department, Section (Division and Team disappear). See **X-09**. *Recommendation:* decide the canonical depth now, because security scoping, approval routing, and cost roll-ups all bind to it; consider a *recursive/adjacency* organisation-unit model with a `unitType` rather than fixed levels, which makes the depth configurable and future-proof.
- **F-02-02 (High · Functional/Payroll) — "Payroll is independent from the organisation hierarchy" (§2.4) needs nuance.** Cost allocation, approvals, and statutory reporting (e.g. establishment-level social insurance) often *do* depend on organisational/legal-entity placement. *Recommendation:* clarify that *gross-to-net calculation* is org-independent but *cost attribution, approval routing, and statutory grouping* are org-dependent.
- **F-02-03 (Medium · Functional) — Contract "only one active at a time" vs. concurrent/dual employment.** §2.6 allows multiple contracts but only one active. Heavy-industry groups sometimes second or dual-employ staff across legal entities. *Recommendation:* confirm whether concurrent active contracts across companies are required; if so the "one active" rule and the payroll aggregation model change materially.
- **F-02-04 (Medium · Functional) — Attendance sources include biometric/API but no de-duplication or conflict rule.** §2.12 lists multiple attendance sources feeding one timesheet. When biometric, crew, Excel, and manual entries collide for the same employee-day, precedence is undefined. *Recommendation:* define a source-precedence and conflict-resolution rule (and surface conflicts as timesheet exceptions).
- **F-02-05 (Low · Database) — Time Types and master entities should carry country/company scoping from the outset.** §2.17 says future time types need no software change; ensure the entity carries labour-law/company-policy scoping (it does later in §5.4) consistently here.

## Chapter 3 — Timesheet Engine

**Purpose.** Converts attendance into approved, cost-allocated payroll transactions via Generate → Copy Days → modify exceptions → validate → approve → lock. Defines daily-record structure, work segments, cost-allocation validation, shift/project changes mid-period, declared vs undeclared OT, approval workflow, audit, and the <1-minute generation target.

**Strengths.** Operationally realistic for construction at scale: Copy Days and Crew Attendance reflect how site timekeeping actually works; unlimited Work Segments correctly replace fixed cost-code columns; "use the shift/project effective on each date" is the right temporal stance; cost-allocation hours must equal approved hours or payroll stops (§3.11) is a strong control.

**Findings.**

- **F-03-01 (High · Payroll/Functional) — Cost-allocation equality with fractional time and rounding.** §3.11 requires Σ segment hours = approved hours exactly. With crew distribution, breaks, and partial hours, exact equality needs a defined tolerance and rounding rule (e.g. distribute remainder deterministically). *Recommendation:* define minute-level precision, a tolerance, and a deterministic remainder-allocation rule; tie to the global rounding policy (**X-03**).
- **F-03-02 (High · Functional) — Undeclared-OT dispositions imply structures not in the data model.** §3.15 allows undeclared OT to be "Converted into Time Off" or "Converted into Future Payment." A *time-off-in-lieu (TOIL) bank* and a *deferred-payment* construct are implied but never modelled (no TOIL entity appears in Chapters 17–21). *Recommendation:* add a TOIL balance and a deferred-payment carry-forward to the Leave/Overtime domains, or remove the disposition options.
- **F-03-03 (Medium · Functional) — Re-approval after edit / lifecycle reversibility.** §3.3 lifecycle and §3.17 audit are good, but the rule for *editing an already-approved (not yet locked) timesheet* — does it reset approvals? partial re-approval? — is undefined. See **X-16**. *Recommendation:* specify that any change after an approval step invalidates downstream approvals and re-routes.
- **F-03-04 (Medium · Performance/Architecture) — <1-minute generation for 70k employees is aggressive and unsized.** Generating ~70,000 × ~30 daily records ≈ 2.1M rows/month within 60 seconds requires bulk-insert/batch strategy, partition-aware writes, and likely parallelism. *Recommendation:* state the batching approach (Spring Batch partitioned steps, COPY/bulk insert, deferred index maintenance) and validate with a load test; treat 60s as a target to *prove*, not assume.
- **F-03-05 (Medium · Functional) — Concurrent editing conflict handling.** §3.18 requires concurrent editing but no optimistic-locking/merge behaviour is described beyond the generic `version` column. *Recommendation:* define optimistic concurrency with clear conflict messaging at the daily-record grain.
- **F-03-06 (Low · Functional) — Late joiner / mid-period termination interaction with Copy Days.** Exceptions list late joiner/termination (§3.7) but the generation step should *not* create days outside the active assignment/contract window. *Recommendation:* state that generation is bounded by contract/assignment effective dates.

## Chapter 4 — Calendar & Shift Engine

**Purpose.** Defines the working schedule: calendar hierarchy and yearly generation, payroll periods, calendar days, public holidays, and a rich shift model (structure, weekly-off, assignment, versioning, types, attendance windows, breaks, overtime limits, Ramadan, rotation, holiday/Friday behaviour, validation).

**Strengths.** The Calendar-belongs-to-Company / Shift-belongs-to-Employee-via-assignment split (§4.2) is clean and powerful. Shift *versioning* with historical payroll continuing on the old version (§4.12) is exactly right for reproducibility. Date-driven shift assignment and "never modify historical shift" are correct.

**Findings.**

- **F-04-01 (Medium · Functional/Payroll) — Cross-midnight night shifts lack date-attribution rules.** §4.18/§2.11 define night shifts (e.g. 22:00–06:00) but not which calendar/payroll day the worked hours, OT, and cost belong to, nor timezone handling. See **X-15**. *Recommendation:* define worked-date attribution (shift-start-date vs split-at-midnight) and store shift times with an explicit timezone.
- **F-04-02 (Medium · Functional) — Ramadan auto-revert depends on the Hijri calendar.** §4.18 says assignments "automatically return to normal after Ramadan ends," but Ramadan dates are lunar and not fixed in the Gregorian calendar. See **X-15**. *Recommendation:* support Hijri dates or an annually-configured Ramadan window per country, and define how the auto-revert is triggered and audited.
- **F-04-03 (Medium · Functional) — Rotational shifts (4/2, 14/7) need a pattern + anchor model.** §4.19 promises custom rotation but the representation (pattern definition, cycle anchor date, handling of leave/holiday interrupting a rotation) is unspecified. *Recommendation:* define a rotation-pattern entity with an anchor date and interruption rules.
- **F-04-04 (Low · Functional) — Holiday/Friday "behaviour" overlaps Time Types and Rule Engine.** §4.20–4.21 say the Shift Engine *classifies* the day and the Payroll/Rule Engine *calculates*. This is correct but the boundary between Shift "behaviour" fields and Rule Engine holiday/Friday rules (§15.12–15.13) should be stated to avoid duplicated/competing configuration. *Recommendation:* make the Shift classify; make the Rule Engine the sole place rates live.
- **F-04-05 (Low · Database) — Shift "weeklyOff" is a single field in the physical model but conceptually multi-day/rotational.** §20.11 shows `weeklyOff` as one column while §4.10 allows Friday+Saturday and rotational. *Recommendation:* model weekly-off as a set/pattern, not a scalar.

## Chapter 5 — Time Type Engine

**Purpose.** Treats every Time Type as a complete, versioned business rule controlling payroll behaviour, the per-component payroll matrix, leave-balance behaviour, overtime behaviour, approvals, provisions, reporting, cost-allocation requirement, validation, assignment restrictions, and future expansion.

**Strengths.** This is one of the strongest design ideas in the document. Making each Time Type drive a *configurable per-component matrix* (§5.6/§6.8) and its own leave/provision/approval behaviour is the right abstraction and directly enables multi-country flexibility without code. Versioning with transaction-date resolution (§5.14) is correct.

**Findings.**

- **F-05-01 (High · Maintainability/Payroll) — The Time Type ↔ Payroll Component matrix is a large configuration surface with no described management/validation tooling.** With unlimited time types × unlimited components, the matrix can reach thousands of cells per rule package per version. There is no described default/inheritance, bulk-edit, or completeness-validation mechanism (what happens to an unset cell?). *Recommendation:* define matrix defaulting (e.g. inherit from category or from a global default), an "unset = explicit error vs. safe default" rule, and simulation/validation tooling (ties to **X-04** and §15.18).
- **F-05-02 (Medium · Functional) — "Depends on Labor Law / Company Policy" cells push undefined logic into the Rule Engine.** Many matrix cells (e.g. Sick Leave basic pay, EOS on unpaid leave) are "depends on Labor Law." That is the correct delegation, but it means the Rule Engine must support component-level conditional outcomes — reinforcing that **X-04** must be resolved before this engine is buildable.
- **F-05-03 (Medium · Functional) — Sick-leave tiering not modelled.** Most GCC labour laws pay sick leave in *tiers* (e.g. first N days full, next M days partial, then unpaid). The matrix as described is a single per-time-type outcome. *Recommendation:* confirm whether duration-based tiering is needed; if so, time-type behaviour must be a *function of cumulative days*, not a flat matrix.
- **F-05-04 (Low · Functional) — Category is "for reporting only" but priority exists (§5.4).** Clarify how Time Type `priority` is used at calculation time (e.g. when a day could match multiple types) versus the Rule Engine priority.

## Chapter 6 — Payroll Component Engine

**Purpose.** Models every salary element as an independent, versioned component with category, payment frequency, calculation method (fixed/percentage/formula/rates/rule/manual), calculation priority, the time-type matrix, formula resolution, recurring/scheduled/one-time behaviour, deductions, provisions, cost allocation, approval, payslip visibility, and historical versioning.

**Strengths.** "Salary is just a collection of components; no component is special" (§6.2) is the correct, future-proof philosophy. The component definition (§6.4) is rich and already includes the right flags (`taxable`, `wpsIncluded`, `eosIncluded`, `provisionIncluded`, `costAllocationRequired`). Scheduled components (schooling every 6 months, air ticket every 24) are well handled.

**Findings.**

- **F-06-01 (Critical · Payroll/Architecture) — Formula resolution algorithm undefined.** §6.7 (priority) and §6.9 (formula components referencing Basic etc.) promise automatic dependency resolution, but neither the expression syntax nor the resolution algorithm is given. A percentage-of-basic that itself feeds EOS that feeds a provision is a dependency graph requiring topological ordering and cycle detection. See **X-04**. *Recommendation:* specify the formula grammar and a deterministic dependency-resolution algorithm; define behaviour on cycles and on references to manual/not-yet-computed components.
- **F-06-02 (High · Payroll) — `Taxable` flag exists but nothing consumes it.** The component carries `taxable` and a `Tax` category, but no tax computation exists. See **X-02**. *Recommendation:* define the taxable-base accumulation and the tax engine that consumes it.
- **F-06-03 (High · Payroll) — Rounding method is per-component but no global accumulation/rounding policy.** §6.4 has `roundingMethod` per component, but rounding *order* (round each then sum, vs sum then round), net-pay rounding, and currency minor units are undefined. See **X-03**.
- **F-06-04 (Medium · Functional) — Negative/deduction components and caps.** §6.14 says "no special logic for deductions," which is elegant, but deduction *caps* and *priority when net would go negative* are genuinely special and missing. See **X-05**.
- **F-06-05 (Medium · Functional) — Proration interaction with component frequency.** A monthly housing allowance for a mid-month joiner, or a scheduled component due in a period where the employee is partly on unpaid leave, needs a defined proration interaction. See **X-03**. *Recommendation:* state, per calculation method, how proration applies.
- **F-06-06 (Low · Database) — Currency at component level invites cross-currency contracts.** §6.4 allows a per-component currency; combined with contract currency this implies possible mixed-currency payslips. Confirm and, if intended, bind to FX handling (**X-08**).

---

## Chapter 7 — Payroll Engine

**Purpose.** The financial core: transforms approved timesheets into payroll transactions by applying labour law, company policy, contracts, components, and time-type rules; supports monthly/daily/weekly/off-cycle/final-settlement and retroactive payroll; produces payslips, journals, and bank/WPS files.

**Strengths.** The processing flow (§7.4) is logically ordered and correctly delegates *what to pay* to rules while owning *the calculation*. "Invalid employees are skipped and logged" (§7.5) is the right batch posture. Retroactive payroll via adjustment transactions without mutating history (§7.10) is correct and consistent with the immutability principle.

**Findings.**

- **F-07-01 (Critical · Payroll) — Proration is shown by example but never defined as a rule.** §7.9 prorates a mid-month salary change (1–14 old, 15–31 new) but does not state the daily-rate basis. §7.7 daily worker uses actual days; §8.18 unpaid leave uses salary/30. Three different implicit conventions, no canonical rule. See **X-03**. This is the highest-frequency payroll-correctness risk in the document.
- **F-07-02 (Critical · Payroll) — No tax/social-insurance step in the processing flow.** §7.4 goes earnings → deductions → provisions → gross → net with no statutory step. See **X-02**. *Recommendation:* insert a statutory-contributions stage (employee deductions and employer costs) operating on YTD cumulative bases.
- **F-07-03 (High · Payroll/Functional) — Off-cycle and weekly payroll mechanics undefined.** §7.2 lists weekly and off-cycle payroll, but the rest of the document is month-centric (one period per month, §2.9). How weekly cycles, off-cycle bonus runs, and their interaction with monthly provisions, scheduled components, and the period-lock model work is unspecified. *Recommendation:* define the run-type taxonomy (regular/off-cycle/correction/final), each one's inputs, and how multiple runs against the same period accumulate and reconcile.
- **F-07-04 (High · Payroll/Architecture) — Batch restart/idempotency for a 70k run is not specified.** A <10-minute parallel run (§7.11) that fails midway must restart safely without double-paying. Spring Batch is in the stack but checkpoint/restart, idempotency keys, and partial-failure semantics are not described. *Recommendation:* define partitioned, restartable, idempotent run semantics with a per-employee run-state and exactly-once posting.
- **F-07-05 (High · Payroll) — Retroactive payroll's interaction with closed/locked periods and statutory YTD.** §7.10 creates adjustments for previous periods. How those adjustments affect already-filed WPS/tax/social-insurance, YTD caps, and provision recomputation is undefined (compounded by **X-02/X-13**). *Recommendation:* define whether retro adjustments are taxed/contributed in the *original* or *current* period, and how YTD is restated.
- **F-07-06 (Medium · Payroll) — Daily-rate derivation for the daily worker vs. monthly mismatch.** §7.7 (daily) vs §8.18 (salary/30) must be unified under **X-03**.
- **F-07-07 (Medium · Functional) — "Multiple Payroll Runs" + lock model need a concurrency rule.** §1.7 allows multiple payroll runs; §22.8 has one period status machine. Whether two runs can target one period concurrently, and how the lock arbitrates, is undefined.

## Chapter 8 — Leave Management Engine

**Purpose.** Manages the full leave lifecycle (types, categories, configuration, balances, accrual, request, approval, validation, paid/unpaid rules, leave calendar, payroll integration, encashment, settlement, audit).

**Strengths.** Treats leave as a "legal and financial event" with the right questions (§8.2). Independent balances per type, multiple accrual methods, and payroll integration via the time-type matrix are all correct. Holiday/weekly-off auto-exclusion using the assigned shift (§8.13) is well thought through.

**Findings.**

- **F-08-01 (High · Functional/Payroll) — Accrual edge cases unspecified.** §8.7 shows simple monthly accrual (2.5/month) but not: pro-rata accrual for mid-month joiners, accrual suspension during unpaid leave/suspension, maximum-balance caps, carry-forward *limits and expiry*, and accrual on probation. The `carryForwardRule` and `negativeBalanceAllowed` fields exist (§8.5) but the rules behind them do not. *Recommendation:* specify each as configurable Rule Engine rules with worked examples.
- **F-08-02 (High · Payroll) — Encashment and settlement rate basis undefined.** §8.15/§8.16 compute leave value (e.g. 18 × daily rate) but the *daily-rate basis for leave* (basic only? basic+housing? full gross?) and *which* labour law/company policy decides is not stated. This is a frequent dispute area and ties to **X-03**. *Recommendation:* make the leave-valuation base an explicit, country-configurable rule.
- **F-08-03 (Medium · Functional) — Sick-leave tiering (see F-05-03) lands here too.** §8.19 says "calculated per Qatar Labor Law Rule Package" but the engine must support duration-tiered pay; confirm and specify.
- **F-08-04 (Medium · Functional) — Half-day / hourly leave and overlapping requests.** Only whole-day leave is illustrated; many policies allow half-day/hourly leave. Overlap validation exists (§8.10) but partial-day handling does not. *Recommendation:* confirm granularity requirements.
- **F-08-05 (Medium · Functional) — "Approval Automatic" for sick leave (§8.19) contradicts configurable workflow.** Clarify whether some leave types bypass workflow and under what rule.
- **F-08-06 (Low · Functional) — Leave during a period that later reopens.** Interaction of approved leave with payroll reopen/data-freeze (§22.9–22.11) should be cross-referenced.

## Chapter 9 — Overtime Management Engine

**Purpose.** Independent OT engine (worked ≠ approved ≠ paid) covering OT types, declared/undeclared, authorization, shift rules, holiday/Friday/night OT, approval, payroll integration, validation, and audit.

**Strengths.** The three-stage model (Worked → Validated → Approved → Paid) and the explicit inequality of worked/approved/paid hours (§9.2) is precisely the right control for construction. Partial approval (§9.16: 5 undeclared, 2 rejected, 3 approved) is well modelled.

**Findings.**

- **F-09-01 (Medium · Payroll/Functional) — OT rate determination split across Shift, Time Type, and Rule Engine.** Holiday 200% (§15.12), Friday 150% (§15.13/§9.15), night premium "configured by rule package" (§9.10) — the authoritative location and the *stacking* rule (e.g. night OT on a public holiday on a weekly-off day) is undefined. *Recommendation:* define rate precedence and whether premiums stack or take the maximum.
- **F-09-02 (Medium · Functional) — Undeclared-OT dispositions (TOIL / future payment) need data structures.** Same gap as F-03-02; see **X-?** TOIL bank. *Recommendation:* model TOIL and deferred OT payment.
- **F-09-03 (Medium · Payroll) — OT hourly-rate basis undefined.** Whether OT is computed on basic, basic+allowances, or a statutory formula (and divided by 30×8, 26×8, or contracted hours) is not specified. Ties to **X-03**. *Recommendation:* make the OT-rate base and divisor a country rule.
- **F-09-04 (Low · Functional) — Standby/call-out OT (§9.3) implies non-worked paid time.** These types pay for availability, not hours worked; confirm the model supports paid-without-attendance OT.

## Chapter 10 — Provision Management Engine

**Purpose.** Calculates company liabilities (EOS, leave, air ticket, etc.) as accruals, with monthly calculation, accounting integration, adjustment, reversal, and final-settlement transfer.

**Strengths.** Correctly separates *liabilities the company recognises* from *payments to employees*. Monthly EOS accrual (§10.5), provision adjustment on salary change (§10.12), and reversal on payment (§10.13) form a coherent accrual lifecycle. Final-settlement transfer (§10.14) closes the loop.

**Findings.**

- **F-10-01 (High · Payroll/Functional) — EOS accrual rate (8.33%) shown as a constant.** §10.5/§10.15 use 8.33% (≈ 1 month/year) as if universal. EOS in GCC law is *tiered by years of service* (e.g. lower rate for first 5 years, higher after) and depends on resignation vs termination and on which components are eligible. A flat monthly rate under-/over-provisions. *Recommendation:* drive EOS accrual from the same tiered rule as settlement (§12.5) so accrual and settlement are consistent, and recompute the accrued liability as service crosses tier thresholds.
- **F-10-02 (High · Reporting/Integration) — Accounting posting lacks a GL master and balancing control.** §10.11 lists debit/credit/cost-centre but there is no Chart-of-Accounts master, no mapping configuration, and no double-entry balancing validation. See **X-14**. *Recommendation:* add GL mapping master and posting validation.
- **F-10-03 (Medium · Payroll) — Air-ticket provision frequency inconsistency in examples.** §10.8 uses every-24-months (÷24) and §10.17 uses every-36-months (÷36); the examples are fine individually but underline that ticket entitlement frequency must be a contract/policy attribute, not assumed. *Recommendation:* confirm entitlement frequency is contract-driven.
- **F-10-04 (Medium · Payroll) — Provision currency and FX.** Provisions accrue over years; if salary currency differs from reporting currency, the FX policy for accrued liabilities (historical rate vs revaluation) is undefined. Ties to **X-08**.
- **F-10-05 (Low · Functional) — Mid-life rule changes to EOS and historical accrual.** When EOS rules change (§15.16 versioning), whether previously accrued provision is restated or grandfathered must be stated.

## Chapter 11 — Additions & Deductions Engine (Financial Transactions)

**Purpose.** Manages financial transactions outside attendance: manual/scheduled/recurring/rule-based/imported additions and deductions, loans, advances, schooling, bonus, commission, with approval, payroll integration, accounting, and audit.

**Strengths.** Treats additions/deductions uniformly as payroll components (§11.16) — consistent with Chapter 6's philosophy. Loan vs salary-advance distinction (§11.10–11.11) and scheduled transactions are well modelled.

**Findings.**

- **F-11-01 (High · Payroll/Functional) — Deduction caps and negative-net are unaddressed (again).** Loan installment + fine + recovery can exceed net. See **X-05**. *Recommendation:* deduction priority, statutory cap, partial recovery, and carry-forward of unrecovered balances.
- **F-11-02 (Medium · Payroll) — Loan interest "(Optional)" but no amortisation model.** §11.10 mentions optional interest with no schedule/method (flat vs reducing balance), early-settlement handling, or restructuring. *Recommendation:* define the loan schedule model or explicitly restrict to interest-free installment loans.
- **F-11-03 (Medium · Functional) — Recurring/scheduled transaction calendar interactions.** Schooling "every 6 months" and "every December" need an anchor and behaviour when the due period is off-cycle or the employee is on unpaid leave/terminated mid-schedule. Ties to **X-03**.
- **F-11-04 (Low · Database) — Chapter has no "Chapter 11" heading.** The document jumps from "End of Chapter 10" straight to "11.1 Introduction" (no `Chapter 11 — Additions & Deductions Engine` title line). Cosmetic, but in a *single-source-of-truth* document used for generation, fix the structural inconsistency so automated tooling and readers index it correctly.

## Chapter 12 — Final Settlement Engine

**Purpose.** Computes all end-of-employment obligations (resignation/termination/completion/retirement/death/transfer/fixed-term end): last salary, OT, leave, EOS, air ticket, loans, recoveries, asset recovery; with approval, provision integration/reversal, settlement payslip, accounting, and audit.

**Strengths.** Comprehensive settlement scope and the right philosophy ("complete financial relationship," reproducible and auditable, §12.2). Asset-recovery gating (§12.9) and provision reversal after payment (§12.11) are correct. The future-enhancements list (gratuity simulation, clearance, e-signature) is sensible and explicitly configuration-driven.

**Findings.**

- **F-12-01 (High · Payroll) — EOS formula details (the legal core) are delegated entirely to the Rule Engine but the engine can't yet express them.** §12.5 correctly forbids hardcoded EOS and lists the inputs (country, contract type, category, years of service, eligible components, policy). But EOS rules require tiered rates, *resignation-penalty scales* (e.g. fractional gratuity for resignation under N years), capping at a number of years, and a defined eligible-earnings base — all of which depend on **X-04** being resolved with sufficient expressive power. *Recommendation:* validate the Rule Engine design against three real EOS regimes (Qatar, UAE, KSA) as acceptance criteria before building.
- **F-12-02 (High · Functional) — Death and transfer cases need distinct handling.** §12.1 lists death and inter-entity transfer but the document does not address beneficiary payment, transfer of accrued service/EOS to the receiving entity (vs settlement), or visa/immigration implications. *Recommendation:* specify death (beneficiary, no asset recovery against estate norms) and transfer (carry service vs settle) as distinct flows.
- **F-12-03 (Medium · Payroll) — Settlement vs final regular-payroll boundary.** §12.10 transfers "current payroll balance" into settlement; the rule preventing double-counting between the last regular run and the settlement run must be explicit (ties to F-07-03).
- **F-12-04 (Medium · Reporting/Integration) — Settlement accounting and provision reversal must reconcile.** §12.19 generates journals and reverses provisions; reconciliation that reversed provision + new expense = settlement payout should be a control. Ties to **X-14**.
- **F-12-05 (Low · Functional) — Negative settlement (employee owes company).** When recoveries/loans exceed entitlements, the net is negative; the document shows only positive nets. *Recommendation:* define the negative-settlement (debt recovery) process and its accounting.

---

## Chapter 13 — Reporting & Analytics Engine

**Purpose.** Turns operational data into operational reports, payroll/cost reports, executive dashboards, and exports; with report categories, filters, export formats, scheduled reports, report security, and a "uses the same business rules as payroll" guarantee.

**Strengths.** Broad, well-categorised report catalogue. The principle that reports use the *same* business rules as payroll (§13.3) avoids the classic "the report disagrees with the payslip" problem. The read-only reporting domain with materialized views (§17.18/§19.20) is a sound CQRS-leaning read model.

**Findings.**

- **F-13-01 (High · Reporting/Architecture) — "Real-time reporting" (§1.7) contradicts materialized-view-based reporting.** Materialized views are periodically refreshed and therefore *not* real-time. Over 70k employees and millions of rows, ad-hoc real-time aggregation will not meet the <30s target without pre-aggregation. See **X-?** reporting. *Recommendation:* define which reports are real-time (transactional reads) vs near-real-time (scheduled MV refresh), state refresh cadence and staleness SLAs, and consider a dedicated read replica for reporting to protect the transactional database.
- **F-13-02 (Medium · Reporting) — Report-as-of-historical-rules.** "Same rules as payroll" must mean *the rule version effective for the period being reported*, not the current version, or historical reports will drift. *Recommendation:* state that reporting resolves rule/version by period, consistent with reproducibility.
- **F-13-03 (Medium · Security) — Report security must inherit row-level data scoping.** §13.22 mentions report security; it must enforce the same company/project/payroll-group scoping as §22.7, including in exports. *Recommendation:* bind report and export authorization to the central data-scoping model (and **X-12**).
- **F-13-04 (Medium · Reporting) — Payroll-cost reports vs employer-cost completeness.** True labour cost includes employer social-insurance/EOS/provision, which depend on **X-02** and F-10-01. Cost reports will understate cost until those exist.
- **F-13-05 (Low · Reporting) — Custom/ad-hoc report builder governance.** "Unlimited custom reports" needs a governance and performance-guardrail model (query limits, scheduling) to protect the database.

## Chapter 14 — Integration & Export Engine

**Purpose.** Bidirectional integration: imports (attendance, Excel/CSV, ERP, government), exports (ERP/SAP/Oracle/Dynamics, banks, WPS, BI), a configurable Mapping Engine, scheduled interfaces, error handling/retry, security, and versioned APIs.

**Strengths.** The configurable Mapping Engine (§14.15) is excellent — changing a customer's export layout becomes configuration, not code (§14.21), directly serving maintainability. Validation-before-processing, an exception queue, payload retention, and automatic retry (§14.17) are solid. Versioned APIs and a clear "integration never calculates payroll" boundary are correct.

**Findings.**

- **F-14-01 (High · Reporting/Integration) — WPS is country-specific and under-specified.** §14.11 lists Qatar/UAE/Oman/KSA WPS "added through configuration," but WPS file formats (e.g. UAE SIF, Qatar WPS/SIF) have rigid, versioned layouts and validation rules, and rejection-handling/return-file processing. Treating them as pure mapping understates the work. *Recommendation:* model WPS per-country as validated format adapters with return-file reconciliation, and define behaviour on bank/WPS rejection.
- **F-14-02 (High · Reporting/Integration) — Bank-file and payroll-total reconciliation control missing.** No control asserts Σ bank transfer = Σ net pay for the run before release. *Recommendation:* add a mandatory pre-release reconciliation gate.
- **F-14-03 (Medium · Architecture) — Error handling lacks dead-letter/poison-message and ordering semantics.** Exception queue + retry exist, but DLQ, max-retry/backoff, idempotent consumers, and ordering guarantees for event-driven interfaces are not specified. Ties to **X-07** (RabbitMQ HA). *Recommendation:* define DLQ, retry/backoff, and idempotency at the consumer.
- **F-14-04 (Medium · Reporting/Integration) — Bulk API and pagination strategy for 70k records.** §14.14 versions APIs but does not define bulk endpoints, pagination, rate-limiting, or async-job patterns for large pulls/pushes. *Recommendation:* specify async bulk APIs with job status, and pagination/rate-limit standards.
- **F-14-05 (Medium · Security) — Inbound integration trust boundary.** Attendance and master-data imports can alter payroll inputs; beyond auth/TLS (§14.22), define authorization scope per interface, schema validation, and tamper-evidence (payload hash exists — good; extend to signed inbound where feasible).
- **F-14-06 (Low · Integration) — Government portal integrations are named but unscoped.** GOSI/labour-ministry/immigration portals differ per country and change often; flag as a per-country backlog rather than a generic capability.

## Chapter 15 — Business Rule Engine

**Purpose.** The decision-making core: configurable rules (no hardcoded logic), a rule hierarchy with override precedence, rule packages, rule types, conditions, versioning, testing/simulation, approval, activation without restart, validation, and audit.

**Strengths.** This is the philosophical and architectural keystone, and the *intent* is exactly right: versioned, simulatable, approvable, hot-activated rules with full audit and historical resolution by transaction date. Simulation (§15.18), rule approval workflow (§15.19), activation without restart (§15.20), and validation including circular-dependency and priority-conflict checks (§15.24) are all the right capabilities.

**Findings.**

- **F-15-01 (Critical · Architecture/Maintainability/Payroll) — The rule representation and evaluation semantics are not defined.** This chapter shows pseudo-`IF/THEN` examples but never specifies: the expression grammar and type system; how conditions compose (AND/OR/precedence); how a rule's THEN sets component-level outcomes; how the 8-level hierarchy *merges* (full override vs field-level override vs accumulation); tie-breaking among same-level matches; the storage/evaluation model (interpreted metadata vs. an embedded engine like DMN/Drools/MVEL); and how thousands of rules evaluate over 70k employees within the run budget. This is the document's most important under-specification. See **X-04**. *Recommendation:* commit to a concrete, declarative, versionable representation (DMN decision tables and/or a sandboxed expression language), and write the merge/precedence/conflict and dependency-resolution semantics in full, with worked multi-level examples (Global→Country→Company→…→Employee).
- **F-15-02 (High · Maintainability) — Rule testing needs a regression/golden-master harness, not just ad-hoc simulation.** §15.18 simulates one employee. Reproducibility (Principle 9) demands automated regression that re-runs archived periods and asserts identical output after any engine change. See **X-10**.
- **F-15-03 (High · Payroll) — Rule expressiveness must be validated against real statutes before build.** EOS tiers, sick-leave tiering, tax brackets with YTD cumulation, deduction caps, and proration are all non-trivial. *Recommendation:* treat "can the engine express Qatar + UAE + KSA + Egypt statutory payroll without code" as a binding proof-of-concept acceptance gate.
- **F-15-04 (Medium · Architecture) — Performance of rule evaluation + caching invalidation.** §16.11 caches rule packages in Redis; rule activation (§15.20) must invalidate caches cluster-wide atomically to avoid mixed-version runs. *Recommendation:* define cache-invalidation-on-activation and a guarantee that a single payroll run pins one rule-package version throughout.
- **F-15-05 (Medium · Security) — Rule changes are high-privilege and need segregation of duties.** §15.19 has an approval chain — good. Ensure SoD (author ≠ approver), full diff audit (it exists, §15.26), and that simulation cannot mutate production.

## Chapter 16 — System Architecture & Technical Design

**Purpose.** The technical blueprint: principles (DDD, clean architecture, SOLID, config-driven, API-first, event-driven, HA, horizontal scaling), high-level architecture, technology stack, modular layering, security architecture, background processing, file storage, caching, messaging, logging, monitoring, HA, performance targets, deployment, configuration, DR, and future scalability.

**Strengths.** Modern and coherent: clean layered backend with "no business logic in controllers" (§16.6), async background processing for heavy jobs (§16.9), object storage for documents (§16.10), Redis caching with auto-invalidation (§16.11), RabbitMQ for events, and a proper observability stack. The principles align well with the configuration-driven philosophy.

**Findings.**

- **F-16-01 (High · Architecture) — Concurrent-user figure contradicts Chapter 1.** §16.16 says 500; §1.7 says 5,000. See **X-06**.
- **F-16-02 (High · Architecture) — HA covers app + PostgreSQL but not Redis/RabbitMQ/MinIO.** §16.15 lists app servers, LB, DB replication, failover — but the cache (holds rule packages), the broker (carries payroll/integration events), and object storage (holds *all* payslips and documents) are single points of failure as written. See **X-07**. *Recommendation:* specify clustered Redis, mirrored/quorum RabbitMQ, and distributed MinIO (or managed equivalents), or document accepted degradation.
- **F-16-03 (High · Architecture) — DR has mechanisms but no RPO/RTO objectives.** §16.19 lists daily backup and PITR but no recovery targets, no failover region, no DR-test cadence with success criteria. See **X-07**. *Recommendation:* set RPO/RTO per data class (payroll/financial likely RPO≈0) and design to them.
- **F-16-04 (Medium · Architecture) — "Event-driven integration" lacks an event contract and consistency pattern.** RabbitMQ is named but there is no event schema/registry, no outbox/inbox pattern for reliable publication from the transactional DB, and no saga/compensation design for cross-domain consistency (e.g. settlement → provision reversal → accounting). *Recommendation:* adopt the transactional-outbox pattern and define event schemas and idempotent handlers.
- **F-16-05 (Medium · Architecture) — Performance targets need a workload model and sizing.** §16.16 targets (<10 min payroll, <30s reports, <500ms API, >1M-row export) are reasonable but unbacked by a sizing model (nodes, vCPU, DB IOPS, partition counts, pool sizes) and a load-test plan tied to the reconciled concurrency. See **X-10**.
- **F-16-06 (Medium · Security) — Security architecture is a list, not a design.** §16.8 names JWT, refresh tokens, RBAC, password encryption, HTTPS, audit, session timeout, optional MFA — but omits secrets/key management (KMS/Vault), encryption-at-rest design (TDE vs column-level for PII/salary), token lifetimes/revocation, and CSRF/CORS posture. See **X-11/X-12** and Chapter 22 findings.
- **F-16-07 (Low · Architecture) — "Database procedures" appear as an integration type (§14.4) despite the no-logic-in-SQL rule.** Reconcile: exposing read-only views is fine; stored *procedures* as an integration surface risks business logic leaking into SQL.

## Chapter 17 — System Data Architecture & Domain Model

**Purpose.** Domain-first data architecture: business domains defined before tables, with ~19 core domains, their contents, domain relationships, scalability targets, and the rule that the database never drives business logic.

**Strengths.** Excellent discipline: "business rules → domain model → database → code" (§17.2). Clear domain ownership, an explicit read-only Reporting domain, an immutable Audit domain, and a Master Data domain. The domain catalogue maps cleanly onto bounded contexts.

**Findings.**

- **F-17-01 (High · Architecture/Database) — Domains are named but aggregates, boundaries, and ownership rules are not.** True DDD needs aggregate roots, invariants, and cross-context contracts (which domain owns which entity; who may write vs read). E.g. Master Data is "shared across all modules" (§17.22) — shared *read* is fine, but write-ownership and change-propagation (cache invalidation, effective-dating of master changes mid-period) must be defined. *Recommendation:* define aggregate roots and an anti-corruption/contract approach between contexts; specify master-data change semantics relative to open/locked periods.
- **F-17-02 (High · Database) — Bi-temporal requirement implied here.** Effective dating + immutable history + retroactive payroll = bi-temporal. See **X-13**. *Recommendation:* state the temporal model explicitly per payroll-affecting entity.
- **F-17-03 (Medium · Database) — Financial Transaction domain (§17.14) vs Chapter 11 naming.** The engine is "Additions & Deductions" (Ch.11) but the domain is "Financial Transaction" (Ch.17/19). Align terminology to avoid generation ambiguity.
- **F-17-04 (Medium · Database) — Multi-currency/Multi-language asserted (§17.24) without supporting domain entities.** No FX-rate or localized-text entities appear. See **X-08**; add an i18n/translation strategy for payslips and rule descriptions.
- **F-17-05 (Low · Architecture) — "Direct database dependencies shall be minimized" (§17.23) vs a shared database.** With one PostgreSQL instance, contexts will share tables; clarify that *logical* isolation (no cross-context table writes) is the intent, enforced by ownership conventions and possibly schema separation (Ch.19 already proposes schemas).

## Chapter 18 — Database Design Principles

**Purpose.** PostgreSQL design principles: naming, UUID PKs, mandatory audit columns, soft delete, master/reference/transaction/history classification, history tables, effective dating, versioning, relationships, partitioning, indexing, constraints, performance, backup, retention, security, scalability.

**Strengths.** A strong, consistent set of principles: UUID PKs (with a clear rationale), mandatory audit columns, soft delete, effective dating, versioning, partitioning of large tables by period/year, GIN/materialized-view usage, and a normalized-with-intentional-denormalization stance. These are enterprise-grade defaults.

**Findings.**

- **F-18-01 (High · Database/Architecture) — UUID PKs on very large, partitioned, heavily-joined tables: specify the variant and index strategy.** Random UUID (v4) PKs hurt index locality and bloat on multi-million-row partitioned tables (Timesheet, PayrollTransaction). *Recommendation:* specify time-ordered UUIDs (v7/ULID) for high-volume tables, and define clustering/fill-factor and the partition-local index strategy. This is a concrete, consequential choice for the 3M-rows/month tables.
- **F-18-02 (High · Database/Security) — Soft-delete + "keep forever" vs GDPR erasure.** §18.6/§18.20 (permanent retention) conflict with §22.19 GDPR. See **X-11**. Also: soft delete requires *every* query and unique constraint to be `isDeleted`-aware (partial unique indexes), or deleted rows cause integrity bugs. *Recommendation:* state the soft-delete query/constraint discipline and the erasure/crypto-shred policy.
- **F-18-03 (Medium · Database) — Effective dating needs overlap-exclusion constraints generally.** Only shift assignment mentions overlap checks (§4.22). Contracts, salary packages, assignments, and rates all need non-overlap guarantees (PostgreSQL exclusion constraints are well suited). *Recommendation:* mandate exclusion constraints on all effective-dated entities.
- **F-18-04 (Medium · Database) — Partition lifecycle management.** Partitioning by period/year is right, but partition *creation automation*, retention/archival of old partitions, and detach/archive procedures are unspecified. *Recommendation:* define partition automation (e.g. pg_partman-style) and archival tiering.
- **F-18-05 (Medium · Database) — Encryption-at-rest specifics.** §18.21 lists "Encryption" for salary/IBAN/ID; choose column-level (pgcrypto/app-side) for the narrow sensitive set vs filesystem/TDE, with key management. Ties to **X-11**, F-16-06.
- **F-18-06 (Low · Database) — `status` as VARCHAR.** §20 shows `status VARCHAR(20)`; prefer constrained enumerations/reference tables with check constraints to prevent invalid states (the reference-table approach in §18.9 supports this).

## Chapter 19 — Database Schema Design

**Purpose.** Logical schema organised into layered schemas (Master, Core HR, Operations, Payroll, Accounting, Workflow, Security, Audit, Reporting) with per-domain entity lists, naming standards, general columns, relationships, indexing, and scalability.

**Strengths.** The schema-per-layer organisation is clean and aids security and maintenance. The entity inventories are thorough and map to the domains. Mandatory general columns (id, audit, version, status, soft-delete) enforce consistency.

**Findings.**

- **F-19-01 (Medium · Database) — Entity lists are not yet a logical model.** Cardinalities, keys, mandatory relationships, and especially *junction tables* for the many-to-many relationships (§19.23) are named in principle but not enumerated (e.g. RolePermission exists, but Time-Type↔Component matrix, Holiday↔Company/Project applicability, Employee↔Bank effective sets need explicit bridges). *Recommendation:* produce a logical ERD with cardinalities before generation; this is the artefact most needed to de-risk Chapter 21's auto-generation.
- **F-19-02 (Medium · Database) — Accounting schema lacks GL master.** §19 lists an Accounting layer conceptually but no GL-account/mapping entities. See **X-14**.
- **F-19-03 (Medium · Database) — Reporting schema staleness contract.** Materialized views/summary tables (§19.20) need a defined refresh/ownership model. Ties to F-13-01.
- **F-19-04 (Low · Database) — Naming: camelCase columns in PostgreSQL require quoting.** PostgreSQL folds unquoted identifiers to lowercase; camelCase columns (§18.3/§19.21) force double-quoting everywhere or silent lowercasing. *Recommendation:* either adopt snake_case at the database and map to camelCase in JPA, or accept and standardise mandatory quoting — decide deliberately to avoid subtle bugs.

## Chapter 20 — Physical Database Design

**Purpose.** Begins the physical table blueprint (Employee, EmployeeBank, EmployeeDocument, EmployeeDependent, Contract, SalaryPackage, Assignment, Calendar, PayrollPeriod, Shift, TimeType, PayrollComponent, CostCenter, CostCode) with columns, keys, indexes, and business notes; defers the remaining transactional tables to later work.

**Strengths.** Concrete, sensible column choices; reaffirms unlimited cost codes (no CC1..CC15), one-active-bank-per-period, effective-dated assignments, and consistent audit/soft-delete/version/effective-dating standards (§20.16).

**Findings.**

- **F-20-01 (High · Database) — Org-hierarchy entities mismatch the domain model.** Assignment references `businessUnitId`, `departmentId`, `sectionId` (no Division/Team), contradicting §2.4. See **X-09**. *Recommendation:* resolve to a single canonical org model (recursive OrganizationUnit recommended).
- **F-20-02 (Medium · Database/Payroll) — Critical tables are explicitly deferred.** The hardest, highest-volume, payroll-correctness-bearing tables (Timesheet, PayrollTransaction, Leave, Overtime, Provision, Settlement, Workflow) are listed as "subsequent chapters" but those chapters are not in this document. *Recommendation:* these must be fully modelled (with partitioning keys, the bi-temporal columns, and exclusion constraints) before generation; they carry most of **X-03/X-04/X-13**.
- **F-20-03 (Medium · Database) — `weeklyOff` scalar, `status` VARCHAR, missing FX/GL entities.** See F-04-05, F-18-06, **X-08/X-14**.
- **F-20-04 (Low · Database) — `fullName` stored alongside name parts.** Denormalized convenience; ensure it is derived/maintained consistently (and consider RTL/Arabic name handling — see localization).

## Chapter 21 — Database Implementation Blueprint

**Purpose.** Declares the document the single source of truth from which the full PostgreSQL schema (plus ERD, FKs, indexes, constraints, materialized views, partitions, Flyway migrations, JPA entities, repositories, seed data) is to be *auto-generated*, lists the table inventory per domain, and sets generation standards and rules (UUID, audit, soft delete, versioning, effective dating, 3NF with intentional denormalization).

**Strengths.** An admirably explicit generation contract and a comprehensive domain→table inventory. The standards are consistent with Chapters 17–20 and the "no additional business logic" instruction protects the architecture's integrity.

**Findings.**

- **F-21-01 (Critical · Database/Maintainability) — Auto-generation will faithfully reproduce every ambiguity above.** Because the schema and entities are generated *from this document*, the unresolved items (proration, rule semantics, bi-temporality, FX, GL, org depth, deferred transaction tables, tax) will either be omitted or guessed. See **X-01..X-14**. *Recommendation:* treat the cross-cutting register as *generation pre-conditions*: do not generate until they are closed in a Volume-2 detailed-design addendum.
- **F-21-02 (High · Database) — Generation needs a logical ERD and constraint catalogue as input, not just table-name lists.** Without explicit cardinalities, junction tables, exclusion constraints, partition keys, and check constraints, generated DDL will be structurally incomplete. See F-19-01. *Recommendation:* author the ERD + constraint catalogue as the binding generation input.
- **F-21-03 (Medium · Maintainability) — Seed data and reference data are mentioned but not specified.** Reference tables, default rule packages (Qatar), default roles/permissions, and master defaults need a defined, versioned seed set (and migration ordering). *Recommendation:* define seed-data scope and ownership.
- **F-21-04 (Medium · Maintainability) — Migration governance for a generated schema.** Flyway/Liquibase are both mentioned (§16.4/§21.5 list each); pick one. Define how *regeneration* coexists with hand-authored migrations once the system is live (you cannot re-generate over production). *Recommendation:* generation is a one-time bootstrap; thereafter changes are forward-only migrations under review.

## Chapter 22 — System Administration, Security & Platform Management

**Purpose.** System configuration, user management, authentication (incl. AD/LDAP/OAuth2/OIDC/SSO/MFA), RBAC authorization with permission levels, roles, data-security scoping, payroll-period lifecycle, payroll lock/reopen, data freeze, audit trail, change history, activity log, session management, monitoring, backup/recovery, security policies, and compliance.

**Strengths.** This is a strong close. The payroll-period state machine (Draft→Open→Processing→Calculated→Validated→Approved→Locked→Bank Transfer→Closed, §22.8), the payroll lock semantics (§22.9), the formal reopen workflow with audit (§22.10), and the data-freeze on close (§22.11) are exactly the controls an enterprise payroll needs. The immutable, richly-attributed audit trail (§22.12, incl. rule version and approval reference) directly supports reproducibility and compliance. Data-scoping dimensions (§22.7) and RBAC permission levels are well chosen.

**Findings.**

- **F-22-01 (High · Security/Architecture) — Data-security scoping (§22.7) needs a database-enforced isolation mechanism.** The scoping dimensions are right, but enforcement is implicitly application-layer. See **X-12**. *Recommendation:* enforce company/scope isolation with PostgreSQL Row-Level Security in addition to service-layer checks; define how scope is carried (security context → DB session variable).
- **F-22-02 (High · Security/Business) — Compliance commitments need concrete designs.** §22.19 commits to Qatar Labour Law, GDPR principles, audit, and retention. GDPR vs permanent retention is unresolved (**X-11**); "GDPR principles where applicable" needs a data-protection design (lawful basis, data map, DSAR handling, cross-border transfer rules given multi-country hosting). *Recommendation:* produce a data-protection and retention specification reconciling statutory payroll retention with privacy rights via pseudonymisation/crypto-shred.
- **F-22-03 (Medium · Security) — Key management and encryption design absent.** MFA, password policy, and TLS are covered, but secrets/key management (KMS/Vault), encryption-at-rest design for PII/salary, token lifetime/rotation/revocation, and break-glass admin access are not. See F-16-06, F-18-05.
- **F-22-04 (Medium · Functional) — Reopen/data-freeze interactions with downstream artefacts.** Reopen (§22.10) must define what happens to already-generated bank/WPS files, posted journals, and issued payslips when a locked period is reopened and recalculated (regenerate? supersede with versioned artefacts? reverse postings?). *Recommendation:* specify artefact-supersession and accounting-reversal on reopen.
- **F-22-05 (Medium · Security) — Segregation of duties across the payroll lifecycle.** The state machine implies multiple actors; make SoD explicit (e.g. the person who runs payroll cannot approve it; rule author ≠ approver; reopen requires dual approval — already partly present). *Recommendation:* define an SoD matrix mapped to roles and permission levels.
- **F-22-06 (Low · Security) — Concurrent-session control vs the concurrency target.** §22.15 concurrent-session control interacts with the (to-be-reconciled) concurrent-user target; ensure session policy does not contradict §1.7/§16.16 once **X-06** is resolved.

---

# PART C — Consolidated Findings, Remediation Roadmap & Open Decisions

Part C re-organises the same findings raised in Parts A and B so they can be consumed three different ways: (C.1) by each of the eight requested review perspectives, so a specialist can read only their lens; (C.2) as a sequenced remediation roadmap, so the programme can plan the order of work; and (C.3) as a register of business decisions that only the client can make and that block detailed design until answered. No new findings are introduced here — every item traces back to a finding ID in Parts A/B.

## C.1 Findings re-cut by review perspective

### Perspective 1 — Business completeness

*Question this lens answers: are all the business capabilities a 70,000-employee multi-country payroll replacement needs actually in scope?*

The document is broad but has material business gaps. The largest is the **complete absence of a data migration, opening-balance, and cutover capability (X-01)** — non-negotiable when replacing a live payroll. Close behind is **taxation and statutory social insurance (X-02)**: income tax and pension/social-insurance schemes for the non-GCC target countries are named in passing but never specified as a capability. Other business-scope gaps: **General Ledger / accounting integration (X-14)** is implied by "financial transactions" but no posting model, cost-centre dimension, or reversal handling is defined; **self-service and mobile scope (X-17)** is referenced but never bounded; and several lifecycle capabilities are thin — **final settlement and EOS tiering (F-08-xx, F-12-xx)**, **off-cycle / retro payroll triggers (F-12-xx)**, and **loan/advance lifecycle including caps (X-05)**. There is also no **QA/parallel-run capability (X-10)** treated as a deliverable. *Net:* the vision is complete; the operational "replace a live system" capabilities are the weak axis and should be elevated to first-class scope.

Primary findings: X-01, X-02, X-05, X-10, X-14, X-17; F-01-xx (scope/assumptions), F-08-xx, F-12-xx.

### Perspective 2 — Functional consistency

*Question: do the chapters agree with each other and with themselves?*

There are concrete, demonstrable inconsistencies. The **concurrent-user target differs 10×** between §1.7 (5,000) and §16.16 (500) — **X-06**. The **organisation hierarchy is 7 levels in §2.4 but only 3 levels in the physical/blueprint chapters** (Division and Team silently dropped) — **X-09**. The **proration/daily-rate convention switches between examples** without a canonical rule — **X-03**. **Chapter 11 has no chapter heading** (sub-section 11.1 appears with no parent), a structural defect that will confuse the "single source of truth" mapping. Smaller contradictions: **air-ticket eligibility frequency examples differ** (24 vs 36 months) across chapters; the **rule-hierarchy depth (8 levels) in Ch.6/15 does not line up with the org hierarchy depth** elsewhere. *Net:* fix the numeric/structural contradictions before generation, because a generator will encode whichever value it reads last.

Primary findings: X-03, X-06, X-09; F-11-xx (missing heading), plus the air-ticket and hierarchy-depth notes in F-02-xx / F-06-xx / F-15-xx.

### Perspective 3 — Architecture (scalability, HA, performance, DDD, CQRS/event-driven)

*Question: will the chosen architecture carry 70k employees and outlive a commercial product?*

The macro-architecture is sound: clean domain decomposition, "engines produce approved transactions → one Payroll Engine turns them into money," and an explicit no-hardcoding/configuration-first stance. The risks are operational. **Supporting infrastructure has unaddressed SPOFs and no recovery objectives (X-07)** — Redis, RabbitMQ and MinIO carry the rule cache, payroll events and all documents, yet only the app tier and PostgreSQL have an HA story, and DR lists mechanisms (daily backup, PITR) without RPO/RTO. The **concurrency contradiction (X-06)** prevents credible sizing. **Multi-tenant isolation (X-12)** is asserted but not enforced at the data layer. The document leans monolith-with-modules; for this scale that is defensible, but the **module boundaries should be made explicit as bounded contexts with an event contract**, and the **transactional-outbox / event-ordering / idempotency semantics (F-07-xx, F-19-xx)** for the "approved transaction" flow are under-specified. Performance targets exist but lack the workload model (batch window concurrency, the 3M-timesheet/month path) to validate them — **F-16-xx**. *Net:* the architecture is right in spirit; harden the supporting tier, settle the numbers, and pin down the event/consistency contracts.

Primary findings: X-06, X-07, X-12, X-13; F-07-xx, F-16-xx, F-19-xx.

### Perspective 4 — Database readiness (entities, effective dating, versioning, auditability, partitioning)

*Question: is the data foundation ready to be designed (note: no DDL is produced in this review)?*

Conceptually strong — immutability, reproducibility, and configuration-over-schema are stated principles. The gaps are in *temporal precision*. The document promises effective-dating and "reproduce payroll years later" but never commits to a **bi-temporal model distinguishing valid-time from transaction-time (X-13)**; without it, retro-correction plus audit-faithful reproduction cannot both be satisfied. **Partitioning is mentioned but no partition keys/strategy** for the high-volume tables (timesheets at 3M/month, payroll results) are defined — F-16-xx, F-20-xx. The **organisation-hierarchy modelling mismatch (X-09)** is a data-model defect. **Opening-balance entities (X-01)** and an **exchange-rate master with effective dating (X-08)** are missing entities. Auditability principles are good but need the **immutable-audit + soft-delete + RLS interaction (X-11, X-12)** worked out so "never delete" and GDPR erasure can coexist. *Net:* the principles are correct; the temporal model, partitioning strategy, and the missing master entities must be decided before schema design.

Primary findings: X-01, X-08, X-09, X-11, X-12, X-13; F-20-xx, F-21-xx, F-16-xx.

### Perspective 5 — Security (auth, RBAC, audit, payroll locking, data freeze, compliance, sensitive data)

*Question: is sensitive payroll data protected and is the lifecycle tamper-evident?*

Good bones: RBAC with permission levels, payroll lock/reopen, data-security scoping, MFA, audit trails. The gaps: **data-security scoping is application-layer only (F-22-01, X-12)** — it needs DB-enforced Row-Level Security; **key/secret management and encryption-at-rest for PII and salary are undefined (F-22-03, F-16-06, F-18-05)**; **GDPR/erasure conflicts with the never-delete principle (X-11)** and needs a crypto-shredding or pseudonymisation resolution; **segregation of duties (F-22-05)** is partial and should be a defined SoD matrix; and **reopen/data-freeze interactions with already-issued artefacts** (bank files, journals, payslips) need a supersession/reversal policy (F-22-04). *Net:* the access-control surface is well thought through; the data-protection and SoD/erasure mechanics are the unfinished part.

Primary findings: X-11, X-12; F-22-01, F-22-03, F-22-04, F-22-05, F-16-06, F-18-05.

### Perspective 6 — Payroll Engine (lifecycle, time/leave/overtime, provisions, final settlement, components, financial transactions)

*Question: will it compute money correctly, repeatably, and lawfully?*

This is the heart of the system and where the most correctness-critical gaps sit. **Proration/daily-rate convention undefined (X-03)** and **the Rule Engine's expression language and formula-evaluation semantics undefined (X-04)** are the two that most directly threaten correct pay: every component formula, hierarchy merge (override vs accumulate across the 8 levels), dependency ordering and cycle detection rests on X-04. **Taxation & social insurance (X-02)**, **deduction caps / negative-net / WPS floor (X-05)**, and **multi-currency FX (X-08)** are correctness-and-compliance gaps. **EOS must be tiered, not a flat 8.33% (F-12-xx)**; **provisions, retro/off-cycle runs, and final settlement (F-08-xx, F-12-xx)** need fuller lifecycle definition; **rounding policy and YTD cumulation (X-03, X-02)** must be canonical and country-configurable. *Net:* the engine's *architecture* is the document's best idea; its *arithmetic contract* is its least-specified critical area and must be fully pinned down before any payroll code is generated.

Primary findings: X-02, X-03, X-04, X-05, X-08; F-06-xx, F-08-xx, F-12-xx, F-15-xx.

### Perspective 7 — Reporting & Integration (export engine, API strategy, ERP/bank/WPS)

*Question: can data get in and out, correctly and at scale?*

The export/integration engine concept is good and reusable (and should be reused for the missing migration loaders, X-01). Gaps: **GL/ERP posting model (X-14)** — no journal structure, cost-centre dimension, or reversal-on-reopen; **WPS is country-specific** (UAE SIF, Qatar, etc.) and needs per-country file-format rule packs plus the **minimum-pay/WPS floor validation (X-05)**; **bank-file generation, acknowledgement/return handling, and reconciliation (F-18-xx, F-19-xx)** need a round-trip design, not just outbound files; **API strategy** (versioning, idempotency, auth for partner/bank integrations) is thin — F-19-xx. **FX for cross-currency consolidation in reports (X-08)** is unresolved. *Net:* outbound generation is conceived; the *round-trip*, *accounting*, and *country-format* depth is missing.

Primary findings: X-08, X-14; F-17-xx, F-18-xx, F-19-xx.

### Perspective 8 — Maintainability (extensibility, configuration-driven design, rule engine)

*Question: can this be operated and extended for years without re-coding?*

This is the document's stated North Star and its strongest theme — nothing hardcoded, everything versioned, rules over code. The risk is that the promise outruns the specification: **the Rule Engine is the linchpin of maintainability and is the least-specified critical component (X-04)**. Without a defined expression grammar, hierarchy-merge semantics, rule-versioning/activation model, and a test harness for rules, the "configure don't code" promise cannot be realised and teams will quietly fall back to code. Also under-specified for long-term operability: **observability (X-18)**, **the QA/regression/parallel-run capability that proves reproducibility (X-10)**, and the **workflow-engine depth (X-16)** that drives configurable approvals. *Net:* maintainability is the right strategy; protect it by fully specifying the Rule Engine and giving rules the same testing rigor as code.

Primary findings: X-04, X-10, X-16, X-18; F-06-xx, F-15-xx.

## C.2 Prioritised remediation roadmap

The roadmap is sequenced so that decisions which unblock the most downstream design are made first. Each phase ends in a gate; the programme should not generate schema or code for an area until its gate is cleared. Durations are relative effort, not commitments.

### Phase 0 — Reconcile contradictions (days, not weeks)

Resolve the cheap-but-blocking inconsistencies so the document is internally consistent before anyone designs against it: the **concurrent-user target (X-06)**, the **organisation-hierarchy depth (X-09)**, the **missing Chapter 11 heading**, the **air-ticket frequency** and **rule-vs-org hierarchy depth** mismatches (Perspective 2). *Gate G0:* a single, self-consistent SRS with no contradictory numbers or structures.

### Phase 1 — Pin the payroll arithmetic contract (highest correctness leverage)

Specify the load-bearing mechanics on which every later calculation depends: **Rule Engine expression language + hierarchy-merge + dependency/cycle semantics (X-04)**; **proration/daily-rate/rounding conventions (X-03)**; **deduction caps, negative-net, WPS floor (X-05)**. These are pure specification work and block all payroll design. *Gate G1:* a written, worked-example-backed "Payroll Arithmetic & Rule Semantics" specification.

### Phase 2 — Close the compliance and money-movement gaps

Add the country-and-finance capabilities: **taxation & statutory social insurance per target country (X-02)**; **multi-currency FX master + consolidation (X-08)**; **GL/accounting posting + reversal model (X-14)**; **country-specific WPS/bank file packs and bank-file round-trip/reconciliation (X-05, F-18/19-xx)**. *Gate G2:* per-country compliance matrix and a posting/payment round-trip design.

### Phase 3 — Lock the data foundation

With arithmetic and compliance known, settle the model: **bi-temporal valid-time/transaction-time decision (X-13)**; **partitioning strategy for timesheets and payroll results (F-16/20-xx)**; **missing master entities (opening balances X-01, FX rates X-08)**; **RLS-enforced multi-tenant isolation (X-12)**; **never-delete vs GDPR erasure resolution (X-11)**. *Gate G3:* an agreed conceptual/logical data model ready for (later) DDL.

### Phase 4 — Migration, cutover & QA capability

Build the "replace a live system" machinery: the **Migration & Cutover chapter (X-01)** with opening-balance loaders, mapping/cleansing, mandatory parallel-run with variance tolerances, reconciliation sign-off gates and rollback; and the **QA / regression / payroll-reproducibility verification strategy (X-10)**. *Gate G4:* a cutover runbook and a reproducibility test plan that can prove a years-old payroll re-computes identically.

### Phase 5 — Operational hardening

Make it production-grade: **HA for Redis/RabbitMQ/MinIO and defined RPO/RTO (X-07)**; **key/secret management and encryption-at-rest (F-22-03, F-16-06)**; **segregation-of-duties matrix (F-22-05)**; **reopen/data-freeze artefact-supersession (F-22-04)**; **observability/SLOs (X-18)**; **workflow-engine depth (X-16)**; **self-service/mobile scope bounding (X-17)**. *Gate G5:* an operability and security design signed off by Tech and QA leads.

Only after G0–G3 (and ideally G4–G5) should schema, APIs, and code be generated — consistent with the document's own "single source of truth" intent.

## C.3 Open decisions register (business / client must answer)

These are not engineering choices; they are decisions only the business and its legal/finance/HR stakeholders can make. Each blocks the phase noted.

1. **Historical payroll: migrate as data, or re-create from migrated inputs + historical rule packs?** (X-01) The latter is far harder and likely infeasible for legacy periods — decide the reproducibility boundary (e.g. "reproducible from go-live forward; legacy periods retained as read-only archived data"). *Blocks Phase 4.*
2. **Which countries are in scope at go-live, and in what order?** (X-02, X-08) Drives the tax/social-insurance and WPS/bank-format work; an open-ended "multi-country" scope cannot be costed. *Blocks Phase 2.*
3. **Canonical proration convention(s).** (X-03) Calendar-days vs fixed-30 vs working-days, per country and per pay element — a policy choice with legal/CBA implications, not a technical default. *Blocks Phase 1.*
4. **Statutory deduction-cap policy and negative-net handling per country.** (X-05) E.g. maximum monthly deduction %, carry-forward of un-recovered balances. *Blocks Phase 1.*
5. **EOS / gratuity rules per country and contract type.** (F-12-xx) Tiered accrual rates, qualifying service, treatment of unpaid leave — legal inputs. *Blocks Phase 1/2.*
6. **GDPR/data-erasure vs never-delete reconciliation.** (X-11) Legal must approve crypto-shredding/pseudonymisation as satisfying erasure obligations while preserving audit/payroll history. *Blocks Phase 3.*
7. **Recovery objectives (RPO/RTO) and acceptable data-loss window for payroll.** (X-07) A business risk decision that sizes the HA/DR investment. *Blocks Phase 5.*
8. **Authoritative concurrent-user and workload figures.** (X-06) The business must confirm real peak concurrency and batch windows so the platform can be sized once. *Blocks Phase 0/1.*
9. **Organisation-hierarchy canonical depth.** (X-09) Are Division and Team real organisational levels the system must model, or were they illustrative? *Blocks Phase 0/3.*
10. **GL/accounting target and posting policy.** (X-14) Which ERP/GL, what cost-centre dimensions, and the reversal policy on payroll reopen. *Blocks Phase 2.*
11. **Self-service and mobile scope.** (X-17) Which transactions are employee/manager self-service at go-live vs later. *Blocks Phase 5 (and overall scope/cost).*
12. **Parallel-run acceptance criteria.** (X-10) The variance tolerance (ideally zero) and number of cycles required before legacy decommission. *Blocks Phase 4 / go-live.*

---

*End of report. All findings (X-01–X-18 cross-cutting; F-01-xx–F-22-xx chapter-specific) are traceable from Part A → Part B → Part C. This review produced no code, no DDL, and no API definitions, per the stated constraint.*
