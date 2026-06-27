# HRMS Workforce Management & Payroll System
## Functional & Technical Design Document — Volume 2
### Detailed Design Specifications (Chapters 23–32)

**Document type:** Functional & Technical Design Document (FTDD), Volume 2
**Relationship to Volume 1:** This volume complements and continues the FTDD (SRS v1.0, Chapters 1–22). It does not replace Volume 1; it deepens it. Chapter numbering continues from Volume 1, beginning at Chapter 23.
**Author role:** Lead Enterprise Solution Architect
**Date:** 25 June 2026
**Status:** For detailed-design sign-off
**System target:** Multi-company, multi-country Workforce Management & Payroll platform for organisations with 70,000+ employees, intended to replace a commercial enterprise HRMS/Payroll platform.

---

## Preface — Why Volume 2 exists

Volume 1 established the vision: every calculation is configuration, nothing is hardcoded, history is immutable, and payroll is reproducible years later. The Architecture Review Report confirmed that this vision is sound but identified a set of **Critical** and **High** findings — load-bearing mechanical details on which payroll correctness and enterprise operability depend, and which Volume 1 left under-specified.

Volume 2 closes those findings. It specifies the parts of the system that cannot be invented at build time without risking incorrect pay, data loss, or a failed go-live: the Rule Engine semantics, the payroll arithmetic contract, the migration and cutover machinery, multi-currency, tax and statutory handling, accounting integration, disaster recovery, quality assurance, security deep design, and operational guidelines.

This volume continues the writing style of Volume 1: numbered sections, short declarative statements, philosophy notes, and concrete worked examples. Consistent with the project constraint, **Volume 2 contains no source code, no SQL, no database table (DDL) definitions, and no API definitions.** Every specification is expressed as business rules, functional requirements, technical requirements, and worked examples — complete enough that the PostgreSQL schema, the Spring Boot backend, and the React frontend can later be generated without inventing additional business logic.

### Coverage map — findings closed by this volume

| Volume 2 Chapter | Closes |
|---|---|
| 23 — Rule Engine Technical Specification | X-04 (Critical) |
| 24 — Payroll Calculation Specification | X-03 (Critical), X-05 (High), X-13 (High) |
| 25 — Data Migration & Cutover Strategy | X-01 (Critical) |
| 26 — Multi-Currency Framework | X-08 (High) |
| 27 — Tax & Statutory Framework | X-02 (Critical) |
| 28 — Accounting & General Ledger Integration | X-14 (High) |
| 29 — Disaster Recovery & High Availability | X-07 (High), X-06 (High) |
| 30 — QA, Parallel Payroll & Reproducibility Testing | X-10 (High) |
| 31 — Security Deep Design | X-11 (High), X-12 (High), F-22-01/03/04/05 |
| 32 — Enterprise Operational Guidelines | X-09 (Med→High), X-16, X-17, X-18 |

### How each chapter is structured

Every chapter in Volume 2 follows the same seven-part structure:

1. **Purpose** — what the chapter specifies and which finding it closes.
2. **Business objectives** — the business outcomes the design must achieve.
3. **Functional requirements** — what the system must do, in functional terms.
4. **Technical requirements** — how it must behave technically (precision, ordering, performance, storage semantics) without prescribing code.
5. **Business rules** — the canonical, numbered rules that govern behaviour. These are the authoritative statements from which logic is generated.
6. **Examples** — worked examples in the Volume 1 style.
7. **Future extensibility** — how the design accommodates change without re-coding.

---

# Chapter 23 — Rule Engine Technical Specification

## 23.1 Purpose

This chapter specifies the Rule Engine completely: the rule model, the expression and formula language, how expressions are parsed and evaluated, the order in which values are computed, how dependencies between values are resolved, how the configuration hierarchy is merged, how conflicts are resolved, how rules are versioned and activated, and how rules are tested and simulated before they affect real pay.

The Rule Engine is the single most important component in the system. Volume 1 promised that "nothing is hardcoded" and that every calculation is configuration. That promise is only real if the engine that interprets configuration is precisely defined. This chapter closes finding **X-04 (Critical)**.

A guiding principle governs the entire chapter:

> The Rule Engine is the only place in the system where business calculation logic lives. The backend executes rules; it does not contain rules.

## 23.2 Business objectives

The Rule Engine exists to achieve five business outcomes:

It must let the business change how pay is calculated — adding an allowance, changing an overtime multiplier, introducing a new country's gratuity formula — without a software release. It must guarantee that a calculation performed today produces an identical result when re-run years later, because the exact rule version used is recorded with the result. It must allow the same platform to serve many companies and countries simultaneously, each with different rules, from one codebase. It must make every calculation explainable: for any number on a payslip, the business can see which rule produced it and why. And it must prevent a bad rule from ever reaching live payroll, by requiring that rules be tested and simulated before activation.

## 23.3 Functional requirements

The engine shall support rules of distinct **rule types**, each with a defined trigger and output. The minimum set is: **Eligibility rules** (decide whether something applies to an employee — e.g. is this employee entitled to an air ticket), **Calculation rules** (produce a numeric value — e.g. the amount of a housing allowance), **Validation rules** (accept or reject a value or transaction — e.g. a deduction must not exceed the legal cap), **Decision rules** (select one outcome from several based on conditions — e.g. choose an overtime multiplier by day type), and **Scheduling rules** (decide when something happens — e.g. when an air-ticket provision accrues).

Every rule shall be a named, versioned, independently testable unit of configuration. A rule shall never be embedded inside another rule's code; rules reference each other only by stable identifier.

Each rule shall declare its **inputs** (the data it reads), its **output** (the single value or decision it produces), its **rule type**, its **scope** (the hierarchy level and entity it applies to), its **effective period** (valid-from / valid-to), and its **version**.

The engine shall evaluate any rule for any employee at any point in time and return both the result and a **calculation trace** showing every intermediate value and which rule version produced it.

The engine shall support **simulation** — evaluating a rule or a whole payroll run against real or hypothetical data without producing any financial transaction — and shall support **what-if comparison** between two rule versions.

## 23.4 The rule and formula language

### 23.4.1 Design stance

Volume 1 implies formula resolution, dependency ordering, and circular-dependency detection but never defines the language. Volume 2 defines a **single declarative expression language** used everywhere a formula appears (component calculations, eligibility conditions, validations, decision tables). The language is deliberately small, side-effect-free, and deterministic. It is an expression language, not a programming language: it has no loops, no mutable variables, no I/O, and no ability to call out to code. This is what makes results reproducible and safe to author by non-developers.

The language is described here functionally (its grammar, operators, and evaluation semantics). It is not source code; it is the specification of the configuration language the system will interpret.

### 23.4.2 Value types

The language supports exactly these value types: **Number** (arbitrary-precision decimal — see Chapter 24 for precision), **Money** (a Number paired with a currency code), **Integer**, **Boolean**, **Date**, **Period** (a payroll period such as 202601), **Text** (used only for codes and labels, never arithmetic), and **Null** (meaning "no value"). Every expression evaluates to exactly one of these types. The type of every expression is known before evaluation; type mismatches are rejected at authoring time, not at run time.

### 23.4.3 Operands — what an expression may reference

An expression may reference only the following, each by a stable name:

A **payroll component** value, by component code (e.g. `BASIC`, `HOUSING`). A **rule parameter** — a named, hierarchy-resolved configuration value (e.g. `OVERTIME_WEEKDAY_MULTIPLIER`). An **employee attribute** exposed to the engine (e.g. grade, nationality, hire date, contract type). A **time/leave aggregate** produced by the upstream engines (e.g. approved overtime hours, unpaid-leave days). A **system function** from the fixed function library (§23.4.6). A **literal** value. The output of **another rule**, by rule identifier. No expression may reference raw database fields, services, or anything outside this list. This restriction is what guarantees the engine remains the only home of business logic.

### 23.4.4 Operators

The language supports the following operators, listed from highest to lowest precedence. Parentheses override precedence.

| Precedence | Operators | Meaning |
|---|---|---|
| 1 (highest) | `( )` | grouping |
| 2 | unary `-`, `NOT` | negation, logical not |
| 3 | `*`, `/` | multiply, divide |
| 4 | `+`, `-` | add, subtract |
| 5 | `=`, `<>`, `<`, `<=`, `>`, `>=` | comparison |
| 6 | `AND` | logical and |
| 7 (lowest) | `OR` | logical or |

Division by zero evaluates to a defined error, never to infinity or null; the rule fails validation and the payroll line is flagged rather than silently producing a wrong number. Arithmetic on Money requires matching currencies; mixed-currency arithmetic must go through the conversion function of Chapter 26 explicitly.

### 23.4.5 Conditional and decision constructs

Two constructs express conditional logic without procedural code:

A **conditional expression**: `IF <boolean> THEN <value> ELSE <value>`. It is an expression (it returns a value), may be nested, and every branch must return the same value type.

A **decision table** (DMN-style): an ordered set of rows, each with a set of input conditions and one output. The table declares a **hit policy** that defines how a result is chosen when multiple rows match (see §23.7). Decision tables are the preferred way to express tiered or banded logic (tax brackets, EOS service tiers, overtime multipliers by day type) because they are readable by business analysts and auditable as data.

### 23.4.6 The system function library

Formulas may call only functions from a fixed, versioned library. The library is the controlled surface through which the language touches dates, rounding, and aggregation. The minimum library:

- **Rounding:** `ROUND(value, scale, method)`, where method is one of the policies defined in Chapter 24.
- **Date:** `DAYS_BETWEEN(d1, d2)`, `MONTHS_BETWEEN(d1, d2)`, `CALENDAR_DAYS(period)`, `WORKING_DAYS(employee, d1, d2)`, `AGE(date)`, `IS_HOLIDAY(employee, date)`.
- **Aggregation over a period:** `SUM_COMPONENT(code, fromPeriod, toPeriod)`, `YTD(code, period)` (year-to-date cumulation, used by tax — Chapter 27).
- **Selection:** `MIN(...)`, `MAX(...)`, `COALESCE(...)` (first non-null), `CAP(value, ceiling)`, `FLOOR_AT(value, floor)`.
- **Hierarchy:** `PARAM(name)` resolves a rule parameter through the override hierarchy (§23.6).
- **Currency:** `CONVERT(money, targetCurrency, date)` (Chapter 26).

The function library is itself versioned. A formula records which library version it was authored against, so behaviour never silently changes when the library evolves.

## 23.5 Evaluation order and dependency resolution

### 23.5.1 The problem

Components depend on each other. Gross pay depends on basic plus allowances; a percentage allowance depends on basic; tax depends on taxable gross; net depends on everything. Values must be computed in an order that respects these dependencies, and the engine must refuse to run if the dependencies form a cycle.

### 23.5.2 Dependency graph

Before evaluation, the engine constructs a **dependency graph** for the set of values to be computed. Each node is a value (a component, a rule output, or a parameter). A directed edge from A to B means "A is needed to compute B." The graph is built by static analysis of the operands each expression references (§23.4.3) — no value needs to be computed to know what it depends on.

### 23.5.3 Topological ordering

The engine computes a **topological order** of the graph and evaluates values in that order, so every value is computed only after all of its inputs. Where two values are mutually independent, their relative order is irrelevant and the result is identical regardless — this is what makes the run deterministic and parallelisable.

### 23.5.4 Cycle detection

If the dependency graph contains a cycle (A needs B which needs A, directly or transitively), the engine **refuses to evaluate** and reports the exact cycle path. Cycle detection runs at two times: at **authoring/activation time**, so a circular rule can never be activated; and as a defensive check at **run time**, so a combination of independently valid rules that together form a cycle is caught before any money is computed. A cycle is always an error — never resolved by guessing an order or breaking the loop arbitrarily.

### 23.5.5 The two-pass principle

Because some statutory values are cumulative (year-to-date tax, social-insurance ceilings), the engine supports a declared **evaluation phase** per rule: *Phase 1 — period values* (everything computable from this period alone) and *Phase 2 — cumulative adjustments* (values that depend on prior-period results, such as YTD reconciliation). Phase 2 always runs after Phase 1 across all components. This ordering is part of the contract, not an implementation detail.

## 23.6 Override hierarchy

### 23.6.1 The hierarchy

Volume 1 defines an 8-level configuration hierarchy. Volume 2 fixes its canonical order, from most general to most specific:

1. Global
2. Country
3. Company
4. Business Unit
5. Project
6. Employee Category
7. Contract
8. Employee

A rule or rule parameter may be defined at any level. When the engine needs a value for a specific employee, it **resolves** the value by walking the hierarchy.

### 23.6.2 Resolution semantics — override vs. accumulate

This is the semantic Volume 1 left undefined. Every rule parameter declares a **merge mode**:

- **OVERRIDE (default):** the most specific defined level wins. The engine walks from Employee (level 8) upward and returns the first defined value. Example: if `OVERTIME_WEEKDAY_MULTIPLIER` is defined at Global as 1.25 and at Company as 1.5, an employee in that company resolves to 1.5.
- **ACCUMULATE:** values from all defined levels are combined by a declared aggregation (sum, or product, or min, or max). Used for things that stack — e.g. a global risk-allowance plus a project-specific risk-allowance. The aggregation function is part of the parameter's definition.
- **MERGE-SET:** for list-valued configuration (e.g. the set of eligible leave types), the resolved set is the union (or, if declared, the most-specific-replaces-all) of the levels.

The merge mode is a property of each parameter, declared once, and is shown in the calculation trace so resolution is always explainable.

### 23.6.3 Resolution is effective-dated

Resolution always happens **as of a date** (the payroll period's reference date, or an explicit as-of date for retro/simulation). At each level, only the rule version effective on that date is considered. This is what allows a payroll for January to use January's rules even when run in March (see Chapter 24, reproducibility).

## 23.7 Rule conflict resolution

Conflicts arise when more than one rule could produce a value. The engine resolves them with explicit, ordered policies — never randomly.

**Within a decision table**, a declared **hit policy** governs multiple matching rows: *UNIQUE* (only one row may match; more than one is an authoring error), *FIRST* (rows are ordered; the first match wins), *PRIORITY* (each output has a priority; the highest-priority match wins), or *COLLECT* (all matches are aggregated — sum/min/max/count, as declared). Every decision table must declare its hit policy; there is no implicit default that hides ambiguity.

**Across rules at the same hierarchy level** producing the same output, the rule with the higher explicit **priority** value wins; if priorities are equal, it is an authoring conflict and activation is blocked. The system never silently picks one.

**Across hierarchy levels**, §23.6.2 governs (override/accumulate/merge-set). Hierarchy resolution and same-level priority are independent and composable: the hierarchy chooses *which level's rule set applies*; priority/hit-policy choose *within* that set.

## 23.8 Versioning

Every rule and every rule parameter is **immutable once activated**. A change never edits an active rule; it creates a **new version** with its own effective period. This is the foundation of reproducibility.

A rule version carries: a version number, an effective-from date, an optional effective-to date, the author, the activation approver, an activation timestamp, and a change note. Versions of the same rule may not have overlapping effective periods at the same scope.

A **rule package** is a named, versioned bundle of rule versions that were active together (e.g. "KSA Payroll Rules — 2026 H1"). Payroll runs record the rule-package version they used, so the entire rule context of a run is captured by a single reference. This is the mechanism that makes "reproduce payroll three years later" practical (Chapter 24, §24.10).

Volume 1's bi-temporal requirement (finding X-13) is satisfied here in the rule dimension: a rule has **valid-time** (the effective period during which it governs pay) and **transaction-time** (when the version was created/activated). A retro correction in May to a rule that should have applied in January creates a new transaction-time record without destroying the version that was used for the original January run — so both "what we computed then" and "what we now believe was correct" are recoverable.

## 23.9 Rule activation

Rules move through a controlled **lifecycle**: *Draft → In Review → Approved → Active → Superseded → Retired.*

A rule may only become **Active** when: it has passed validation (type-checks, no cycles, no same-level priority conflicts), it has at least one passing test case attached (§23.10), it has been approved by an authoriser who is not its author (segregation of duties — Chapter 31), and its effective-from date is set. Activation is itself an audited event.

Activation never deletes the prior version; the prior version becomes **Superseded** and remains available for reproduction of past results. **Retired** means a rule is no longer used for any future period but is retained for history.

Emergency changes follow the same path with an expedited "break-glass" approval that is more heavily audited, never a path that bypasses testing.

## 23.10 Rule testing

No rule reaches Active without tests. A **rule test case** declares a set of inputs and the expected output (or expected decision, or expected accept/reject). Test cases are stored with the rule and re-run automatically whenever the rule, the function library, or a depended-upon rule changes — a regression suite for configuration.

The system shall support: **unit tests** for a single rule; **scenario tests** that exercise a group of rules for a representative employee profile; and **golden tests** — frozen input/output pairs captured from a known-correct payroll run, used to prove that a change has not altered results that were supposed to stay the same. Golden tests are the bridge to Chapter 30 (reproducibility testing).

A rule change that breaks an existing golden test cannot be activated without an explicit, audited acknowledgement that the change of result is intended.

## 23.11 Rule simulation

Simulation runs the engine end-to-end **without producing any financial transaction**. It exists so the business can see the effect of a change before committing to it.

The system shall support: **single-employee simulation** (compute and trace one employee's pay under a candidate rule set); **population simulation** (run a candidate rule package across all or a filtered set of employees and report aggregate impact — total cost change, count of employees whose net pay changes, largest movers); and **A/B comparison** (run the same population under the current package and a candidate package and produce a line-by-line difference).

Simulation always uses a copy-on-read view of data and is explicitly marked as non-financial, so simulated runs can never be mistaken for, or promoted directly to, real payroll without going through activation and an actual payroll run.

## 23.12 Business rules (canonical)

- **BR-23-01** All business calculation logic resides in rules; the application code interprets rules and contains none.
- **BR-23-02** Every rule and rule parameter is versioned and immutable once active; changes create new versions.
- **BR-23-03** The expression language is declarative, deterministic, side-effect-free, and has no loops or mutable state.
- **BR-23-04** An expression may reference only components, parameters, exposed employee attributes, time/leave aggregates, library functions, literals, and other rules' outputs.
- **BR-23-05** Values are computed in topological dependency order; mutually independent values may be computed in any order with identical results.
- **BR-23-06** Circular dependencies are always errors; they block activation and abort run-time evaluation with the cycle path reported.
- **BR-23-07** Every rule parameter declares a merge mode (OVERRIDE | ACCUMULATE | MERGE-SET); resolution walks the 8-level hierarchy as of the reference date.
- **BR-23-08** Every decision table declares a hit policy; same-level rule conflicts are resolved by explicit priority and are an authoring error if tied.
- **BR-23-09** A rule may become Active only after validation, at least one passing test, and approval by a non-author.
- **BR-23-10** Every computed value is accompanied by a trace identifying the exact rule versions that produced it.
- **BR-23-11** A payroll run records the rule-package version used, enabling exact reproduction.
- **BR-23-12** Simulation never creates financial transactions and can never be promoted to live payroll without activation and a real run.

## 23.13 Examples

**Example 1 — A percentage allowance (override hierarchy + dependency order).**

```
Rule: HOUSING_ALLOWANCE  (Calculation, output = Money)
Formula:  ROUND( BASIC * PARAM(HOUSING_RATE), 2, HALF_UP )

PARAM(HOUSING_RATE):
   Global   = 0.20
   Company  = 0.25   (merge mode OVERRIDE)

Employee Ahmed, Basic = 4,500, in a company with the 0.25 override.

Dependency graph:  BASIC  →  HOUSING_ALLOWANCE
Resolution:        HOUSING_RATE resolves to 0.25 (Company overrides Global)
Evaluation:        HOUSING_ALLOWANCE = ROUND(4,500 × 0.25, 2) = 1,125.00

Trace:  HOUSING_ALLOWANCE = 1,125.00
        ← BASIC = 4,500.00
        ← HOUSING_RATE = 0.25  (resolved at Company level, v3, eff 2026-01-01)
```

**Example 2 — A tiered EOS accrual (decision table, replacing the flat 8.33%).**

Volume 1's example used a flat 8.33%. Finding F-12 noted EOS must be tiered. Expressed as a decision table:

```
Decision table: EOS_ACCRUAL_RATE   (hit policy = FIRST, input = completed years of service)

  Row | Condition                         | Output (rate on basic)
  ----+-----------------------------------+-----------------------
   1  | years_of_service < 1              | 0
   2  | years_of_service >= 1 AND < 5     | 0.0833   (≈ 21 days/yr)
   3  | years_of_service >= 5             | 0.1666   (≈ 30 days/yr)

Employee with 6 completed years, Basic = 5,000:
   EOS_ACCRUAL = ROUND(5,000 × 0.1666, 2) = 833.00 for the period’s provision
```

**Example 3 — A validation rule (deduction cap, linking to Chapter 24).**

```
Rule: DEDUCTION_CAP_CHECK  (Validation)
Formula:  TOTAL_DEDUCTIONS <= PARAM(MAX_DEDUCTION_RATE) * NET_BEFORE_DEDUCTIONS
PARAM(MAX_DEDUCTION_RATE): Country = 0.50

If the check fails, the deduction is reduced per the deduction-priority rules of
Chapter 24, and the shortfall is carried forward — the rule does not silently
allow negative net.
```

**Example 4 — Cycle detection (an authoring error that is blocked).**

```
ALLOWANCE_A = BASIC + ALLOWANCE_B * 0.1
ALLOWANCE_B = BASIC + ALLOWANCE_A * 0.1     ← A needs B, B needs A

Engine response at activation: REJECTED.
Reported cycle: ALLOWANCE_A → ALLOWANCE_B → ALLOWANCE_A
Neither rule can be activated until the cycle is removed.
```

## 23.14 Future extensibility

The fixed function library is versioned, so new functions (e.g. a new statutory rounding method) are added as additive library versions without disturbing existing formulas. New rule types can be introduced as new declared trigger/output contracts without changing the language. The hierarchy is data-driven; if a new organisational level is ever required, it is added as a level in the resolution order rather than as new code. Decision tables, being data, allow whole new countries' bracketed logic (tax, EOS, social insurance) to be onboarded as configuration. Because every rule records the library and package version it was authored against, the language and library can evolve indefinitely while every historical result remains exactly reproducible.

---

# Chapter 24 — Payroll Calculation Specification

## 24.1 Purpose

This chapter defines the payroll arithmetic contract: the exact lifecycle a payroll run follows, how proration is computed, how daily and hourly rates are derived, how values are rounded, the precision used for money, how retroactive and off-cycle payroll work, how negative net is prevented, the order in which deductions are taken, and how a payroll result is made reproducible.

These are the mechanics on which payroll *correctness* depends and which Volume 1 left ambiguous. This chapter closes findings **X-03 (Critical)** and **X-05 (High)**, and completes the bi-temporal/reproducibility requirement **X-13 (High)** begun in Chapter 23.

A guiding principle:

> Every figure on a payslip must be explainable, reproducible, and lawful. There are no implicit conventions; every convention is a named, country-configurable rule.

## 24.2 Business objectives

Payroll must be correct to the smallest currency unit and identical every time it is recomputed for the same inputs and rules. Proration of mid-period joiners, leavers, salary changes, and unpaid absence must follow a convention the business has explicitly chosen, because proration is the most common source of pay disputes. An employee's net pay must never become negative through deductions, and statutory caps on deductions must be honoured. A payroll already approved and paid must be correctable through controlled retro and off-cycle mechanisms, not by editing history. And any historical payroll must be reproducible years later, exactly, for audit and dispute resolution.

## 24.3 Functional requirements

The system shall execute payroll as a defined sequence of stages (§24.4), each idempotent and restartable. It shall compute each employee's components using the Rule Engine (Chapter 23), apply proration where applicable, round per the component's rounding policy, apply deductions in priority order subject to caps, and produce a payslip plus the financial transactions that feed banking (Chapter 26), accounting (Chapter 28), and statutory reporting (Chapter 27).

It shall support a **regular monthly run**, an **off-cycle run** (a payment outside the normal cycle — e.g. a final settlement or a correction), and a **retro run** (recomputation of a prior, already-closed period). It shall snapshot every input and rule version so a run can be reproduced (§24.10). It shall never allow a paid period to be silently altered; corrections always create new, traceable transactions.

## 24.4 Payroll calculation lifecycle

A payroll run progresses through the following ordered, audited stages. Each stage has a defined entry condition and output; a run may be paused and resumed between stages.

1. **Initiate** — select the payroll group, period, and run type (regular / off-cycle / retro). The system resolves the applicable rule package as of the period reference date and locks it to the run.
2. **Collect inputs** — gather approved transactions from the upstream engines (attendance/timesheet, overtime, leave, loans, one-time payments, etc.). Only *approved* transactions enter; nothing is computed from un-approved data.
3. **Snapshot** — freeze a complete, immutable copy of every input and every resolved rule/parameter version used for this run. This snapshot is the reproducibility anchor (§24.10).
4. **Calculate** — for each employee, build the dependency graph and evaluate components in topological order (Chapter 23). Apply proration (§24.5), rate derivation (§24.6), rounding (§24.7), then deduction priority and caps (§24.9).
5. **Validate** — run all validation rules (deduction caps, negative-net, WPS minimum-pay floor, currency consistency). Failures are flagged per employee; the run can proceed for clean employees while flagged ones are held.
6. **Review** — produce the run register, exception report, and variance-vs-prior-period report for human review.
7. **Approve** — an authoriser (not the person who ran it — Chapter 31) approves. Approval is required before any money-movement or posting artefact is generated.
8. **Lock** — the period is locked; results become immutable. Further change is only via retro/off-cycle.
9. **Generate outputs** — payslips, bank/WPS files (Chapter 26), GL journals (Chapter 28), statutory files (Chapter 27).
10. **Close** — the period is closed; the run, its snapshot, and its rule-package reference are archived for reproduction.

## 24.5 Proration rules

Proration converts a periodic amount into the portion actually earned when an employee is not present for the whole period. Volume 1's examples mixed conventions; Volume 2 makes the convention an explicit, country-configurable rule.

### 24.5.1 The proration basis

Each prorated component resolves a **proration basis** through the rule hierarchy. The supported bases are:

- **CALENDAR_30** — the period is always treated as 30 days regardless of actual length. Daily portion = amount / 30. Common in GCC practice.
- **CALENDAR_ACTUAL** — the period uses its actual number of calendar days (28–31). Daily portion = amount / actual days.
- **WORKING_DAYS** — only the employee's scheduled working days count. Daily portion = amount / scheduled working days in the period.
- **FIXED_WORKING** — a fixed assumed number of working days (e.g. 22) regardless of the actual count.

The basis is a parameter (merge mode OVERRIDE), so a country, company, or even a contract type can set its own convention, and the basis used is recorded in the trace.

### 24.5.2 Proration triggers

Proration applies on: **mid-period hire** (earn from hire date to period end), **mid-period termination** (earn from period start to last working day), **mid-period salary or component change** (split the period at the effective date and apply each rate to its portion), and **unpaid absence** (deduct the unpaid days at the daily portion). Each trigger uses the same basis so the arithmetic is consistent.

### 24.5.3 Proration must reconcile

A core rule: the sum of prorated segments within a period must never exceed the full-period amount, and an employee present the whole period with no changes must receive exactly the full amount (proration is a no-op). Rounding of prorated segments uses the residual method of §24.7.4 so segments always sum back to the rounded full amount.

## 24.6 Daily rate and hourly rate calculation

### 24.6.1 Daily rate

The **daily rate** is derived, never stored as an independent figure that could drift from salary. It is computed from a configurable **daily-rate basis** that mirrors the proration bases:

`daily_rate = monthly_basis_amount / daily_rate_divisor`

where the divisor is 30, the actual calendar days, the scheduled working days, or a fixed number, per the resolved basis. The component(s) included in `monthly_basis_amount` (e.g. basic only, or basic + housing) is itself a configurable set — the **rate base composition** — because different countries and different purposes (overtime vs. unpaid-day deduction vs. EOS) legally use different bases. The system therefore supports **named rate bases** (e.g. `OVERTIME_BASE`, `EOS_BASE`, `UNPAID_DAY_BASE`), each defining its component set and divisor.

### 24.6.2 Hourly rate

The **hourly rate** is derived from the daily rate and the employee's standard daily hours:

`hourly_rate = daily_rate / standard_daily_hours`

`standard_daily_hours` is a resolved parameter (e.g. 8). Overtime pay is then `hourly_rate × overtime_hours × overtime_multiplier`, where the multiplier is selected by a decision table on day type (weekday / weekend / public holiday) per Chapter 23.

### 24.6.3 No silent rate storage

Daily and hourly rates are computed at point of use from current salary and the named rate base. They may be cached within a run's snapshot for reproducibility, but they are never a standalone editable field, so they can never disagree with the salary they derive from.

## 24.7 Rounding policy

### 24.7.1 Rounding is explicit and per-component

Every monetary value is rounded by a named rounding method resolved per component (Volume 1 already lists "Rounding Method" as a component attribute; Volume 2 defines the methods). Supported methods:

- **HALF_UP** — round half away from zero (the common default).
- **HALF_EVEN** (banker's rounding) — round half to the nearest even digit; reduces cumulative bias in large populations.
- **CEILING** / **FLOOR** — always up / always down (used for some statutory amounts).
- **TRUNCATE** — drop digits beyond scale (used rarely, only where law requires).

### 24.7.2 Rounding scale

The **scale** (number of decimal places) is the minor-unit precision of the component's currency (§24.8) unless a rule overrides it (some statutory amounts round to whole units).

### 24.7.3 Round once, at defined points

Intermediate calculations are carried at full precision; rounding is applied only at defined boundaries — the final component value, and any value that legally must be a rounded figure before it feeds another calculation (e.g. a rounded taxable base if law requires). Rounding the same value twice is forbidden; the trace records where rounding occurred.

### 24.7.4 Residual rounding for splits

When a total is split (proration segments, cost-centre allocation, currency conversion of a multi-line total), the system uses **largest-remainder residual allocation**: each part is rounded, and the rounding residual is assigned to the largest part so the parts sum exactly to the rounded total. This guarantees that prorated segments and allocations never lose or gain a minor unit.

## 24.8 Currency precision

Money is stored and computed as **arbitrary-precision decimal**, never as floating-point binary, so 0.10 + 0.20 is exactly 0.30. Each currency declares its **minor-unit scale** from its ISO 4217 definition (2 for most, 3 for some — e.g. Kuwaiti/Bahraini/Omani dinars, 0 for some). All display and final-value rounding uses the currency's scale. Internal calculation precision is higher than the storage scale (a defined working precision, e.g. 6 decimal places) so that chained percentages and conversions do not lose accuracy before the single final rounding. Cross-currency arithmetic is never implicit; it always passes through the conversion function of Chapter 26 with an explicit rate and date.

## 24.9 Deduction priority and negative-net handling

This section closes finding **X-05 (High)**.

### 24.9.1 The problem

Loans, advances, fines, recoveries, garnishments, and statutory deductions can together exceed an employee's net pay. Labour law typically caps the total that may be deducted in a period and protects a minimum take-home. Volume 1 had no cap, no priority, and no negative-net handling.

### 24.9.2 Deduction priority

Every deduction component carries a **deduction priority** (a number; lower = taken first) and a **deduction class**. The canonical default ordering (configurable per country) is:

1. **Statutory mandatory** — social insurance / pension, income tax (Chapter 27). Taken first; never reduced by caps unless law says so.
2. **Court-ordered** — garnishments, legal attachments.
3. **Company recoverable** — loans, salary advances, asset recoveries.
4. **Disciplinary** — fines, penalties.
5. **Voluntary** — savings, voluntary contributions.

Deductions are applied in priority order until the cap or the minimum-net floor is reached.

### 24.9.3 Deduction cap and minimum-net floor

Two resolved parameters govern limits: **MAX_DEDUCTION_RATE** (maximum fraction of pay that may be deducted in a period) and **MIN_NET_FLOOR** (an absolute minimum take-home, e.g. a minimum-wage or WPS floor). The system reduces lower-priority deductions first so that the cap and floor are respected. Statutory deductions are exempt from the cap unless the country rule states otherwise.

### 24.9.4 Carry-forward of un-recovered amounts

When a deduction is reduced or skipped because of a cap or floor, the **shortfall is carried forward** to the next period as an outstanding balance against the same obligation (e.g. the remaining loan instalment). Carry-forward is tracked per obligation with a full ledger, so balances never silently disappear and never push net negative.

### 24.9.5 Negative net is impossible by construction

Net pay is computed as gross minus the *capped, floored, prioritised* deductions. Because lower-priority deductions are reduced before the floor is breached, and the remainder is carried forward, net can be zero but never negative. A run that would produce negative net before carry-forward is a flagged exception in stage 5, not a payment.

## 24.10 Payroll reproducibility

This section, with Chapter 23 §23.8, closes the reproducibility and bi-temporal requirement (**X-13**).

A payroll result is reproducible because the run's **snapshot** (stage 3) and **rule-package reference** together capture everything needed to recompute it: every input transaction as it was at run time, every resolved rule and parameter version, the function-library version, the proration/rate/rounding bases, and the FX rates used. Re-running the engine against the snapshot must produce a bit-for-bit identical result.

The system distinguishes **valid-time** (the period the pay relates to) from **transaction-time** (when a record was created or corrected). A correction made later never overwrites the original; it is a new transaction-time record. Therefore the system can always answer two distinct questions: *"What did we pay for January, as computed in January?"* (the original snapshot) and *"What do we now believe January should have been?"* (the original plus retro corrections). Both are first-class, queryable states.

A **reproducibility self-test** (Chapter 30) periodically re-runs archived snapshots and asserts the results are identical, proving the guarantee holds over time and across software upgrades.

## 24.11 Retroactive and off-cycle payroll

### 24.11.1 Retro payroll

A **retro run** recomputes one or more already-closed periods because something that should have applied then is now known (a backdated salary increase, a late-approved overtime, a corrected rule). The retro run:

1. Recomputes each affected closed period **using that period's original rule package and bases** (so the recomputation reflects what *should* have been computed then, not today's rules — unless the correction is specifically a rule fix, which is versioned with the correct historical effective date per Chapter 23).
2. Computes the **delta** between the original result and the corrected result, per component.
3. Pays or recovers the delta **in the current open period** as clearly-labelled retro lines, with full back-reference to the period and reason.
4. Generates corresponding accounting reversals/adjustments (Chapter 28) and, where required, amended statutory filings (Chapter 27).

Retro never edits the original closed period's payslip; the original remains the historical record, and the delta is transparent.

### 24.11.2 Off-cycle payroll

An **off-cycle run** is a payment made outside the regular schedule — most commonly a **final settlement** (end-of-service: gratuity, leave encashment, outstanding dues minus recoveries) or an urgent correction. Off-cycle runs use the full lifecycle (§24.4) but for a defined subset of employees and components, and they respect the same caps, floors, rounding, and reproducibility rules. Final settlement specifically resolves all carry-forward balances (§24.9.4): outstanding loans are recovered (subject to the cap/floor and legal rules), and accrued provisions (EOS, leave) are paid out.

## 24.12 Business rules (canonical)

- **BR-24-01** Payroll executes as a fixed, audited, restartable, idempotent stage sequence; outputs are produced only after approval.
- **BR-24-02** Every prorated, daily, or hourly figure derives from a named, hierarchy-resolved basis; there are no implicit conventions.
- **BR-24-03** Prorated segments always reconcile to the full-period amount; full-period presence with no change yields the exact full amount.
- **BR-24-04** Daily and hourly rates are derived at point of use from current salary and a named rate base; they are never independently editable.
- **BR-24-05** Money is arbitrary-precision decimal; each currency uses its ISO minor-unit scale; floating-point is never used for money.
- **BR-24-06** Rounding is explicit, per-component, applied once at defined boundaries, using a named method; splits use largest-remainder residual allocation.
- **BR-24-07** Deductions are taken in priority order subject to a maximum-deduction cap and a minimum-net floor; statutory deductions are exempt from the cap unless law states otherwise.
- **BR-24-08** Un-recovered deduction amounts are carried forward per obligation with a tracked ledger; net pay can be zero but never negative.
- **BR-24-09** A run snapshots all inputs and rule versions; re-running against the snapshot reproduces the result exactly.
- **BR-24-10** Valid-time and transaction-time are distinct; corrections create new records and never overwrite history.
- **BR-24-11** Retro runs recompute affected periods on their original bases, pay/recover only the delta in the current period, and never edit closed payslips.
- **BR-24-12** Off-cycle and final-settlement runs use the full lifecycle and resolve all carry-forward balances and accrued provisions.

## 24.13 Examples

**Example 1 — Mid-period hire with CALENDAR_30 basis.**

```
Ahmed hired 16 January. Monthly Basic = 6,000. Proration basis = CALENDAR_30.
Days earned = 15 (16th–30th inclusive, by the 30-day convention).
Daily portion = 6,000 / 30 = 200.00
Prorated Basic = 200.00 × 15 = 3,000.00
```

**Example 2 — Unpaid leave with WORKING_DAYS basis.**

```
Monthly Basic = 6,600. Scheduled working days in period = 22. Unpaid days = 2.
Basis = WORKING_DAYS  →  daily portion = 6,600 / 22 = 300.00
Unpaid deduction = 300.00 × 2 = 600.00
Paid Basic = 6,600.00 − 600.00 = 6,000.00
```

**Example 3 — Overtime via named rate base.**

```
OVERTIME_BASE = Basic only. Basic = 5,280. standard_daily_hours = 8.
daily_rate  = 5,280 / 22 (WORKING_DAYS)   = 240.00
hourly_rate = 240.00 / 8                    = 30.00
Weekday overtime multiplier (decision table) = 1.25
6 weekday OT hours → 30.00 × 6 × 1.25 = 225.00
```

**Example 4 — Deduction cap, floor, and carry-forward (negative net prevented).**

```
Gross = 4,000.  Statutory deduction = 400 (exempt from cap).
MAX_DEDUCTION_RATE = 0.50 of gross = 2,000.  MIN_NET_FLOOR = 1,500.
Outstanding loan instalment requested = 2,500 (priority: Company recoverable).

Net before recoverable deductions = 4,000 − 400 = 3,600.
Max recoverable without breaching floor = 3,600 − 1,500 = 2,100.
Cap also allows up to 2,000 of non-statutory deductions → binding limit = 2,000.
Loan taken this period = 2,000.   Carry-forward to next period = 500.
Net pay = 3,600 − 2,000 = 1,600  (≥ floor; never negative).
```

**Example 5 — Retro salary increase.**

```
March: a salary increase backdated to 1 January is approved.
Retro run recomputes Jan and Feb on their original bases:
   Jan delta = +300,  Feb delta = +300.
Current (March) payslip shows:
   Regular March pay …………………… as normal
   Retro adjustment (Jan–Feb) ……… +600.00  [ref: periods 202601, 202602]
Closed Jan/Feb payslips are unchanged; a GL adjustment is posted for the delta.
```

## 24.14 Future extensibility

New proration bases, rate bases, and rounding methods are added as named configuration entries, not code. New deduction classes slot into the priority framework by declaration. Because retro and off-cycle reuse the same lifecycle and snapshot mechanism, new run types (e.g. a bonus-only run, a 13th-month-salary run) are configuration of which components and which population participate. The valid-time/transaction-time foundation means any future audit, dispute, or regulatory look-back requirement is answerable without schema change. New currencies, including those with non-2 minor units, are onboarded by their ISO definition alone.

---

# Chapter 25 — Data Migration & Cutover Strategy

## 25.1 Purpose

This chapter specifies how 70,000+ employees and their accumulated history are moved from the legacy commercial HRMS/Payroll platform onto this system, how opening balances are established, how correctness is proven before go-live, and how the organisation falls back if go-live fails. This is the single largest go-live risk and Volume 1 was silent on it. This chapter closes finding **X-01 (Critical)**.

A guiding principle, consistent with Volume 1's §1.8 (do not copy the legacy database):

> We migrate *meaning*, not *tables*. Every migrated value enters through the same validated, rule-governed paths a live transaction would, so the migrated state is indistinguishable from state the system itself produced.

## 25.2 Business objectives

The business must be able to stop running the legacy system and run this one with no loss of employee entitlement, no broken balances, and no interruption to the pay cycle. Accrued rights that took years to build — end-of-service entitlement, leave balances, loan balances, year-to-date statutory figures — must arrive intact and correct. The first live payroll must be provably equal to what the legacy system would have produced, before anyone is paid from the new system. And if anything goes wrong, the organisation must be able to return to the legacy system within a defined window without having lost data.

## 25.3 Functional requirements

The system shall provide a **migration capability** built on the same Integration/Import engine used for ongoing integrations (Chapter 28 / Volume 1's export engine), so migration is configuration-driven and repeatable, not a one-off script. It shall import master data, opening balances, and (optionally) historical data through validated loaders, each producing a reconciliation report. It shall support repeated **trial migrations** into non-production environments, a mandatory **parallel run** comparing new vs. legacy payroll, defined **reconciliation sign-off gates**, a controlled **cutover**, and a **rollback** path.

Every migrated record shall be tagged with its source, its migration batch, and a "migrated" provenance flag, so migrated data is always distinguishable from natively-created data for audit.

## 25.4 Migration scope and sequencing

Migration proceeds in dependency order, because later objects reference earlier ones. The canonical sequence:

1. **Reference and configuration data** — countries, currencies, calendars, organisation hierarchy, grades, payroll components, and the initial rule packages. (This is configuration, validated and activated per Chapter 23, before any employee arrives.)
2. **Employee master data** — identities, contracts, assignments, organisational placement, bank details, salary structures (as component values).
3. **Opening balances** — the accumulated states (§25.5).
4. **Historical data** — past payslips and transactions, to the extent in scope (§25.6).
5. **In-flight items** — un-recovered loan balances, pending leave requests, open provisions, approvals in progress.

Each step is gated: a step may not begin until the prior step's reconciliation has been signed off.

## 25.5 Opening balances

Opening balances are the heart of migration. Each is loaded as a validated opening entry with an "as-of" date (the cutover date), so the system's ledgers begin from a known, reconciled state.

### 25.5.1 Employee migration

Each employee is migrated with: identity and personal data; one or more contracts and assignments with their historical effective dates (so service length is correct); current salary expressed as **payroll component values** (not a single salary number), so the new component-based engine can reproduce current pay; bank and payment details; and organisational placement under the reconciled hierarchy (Chapter 32 reconciles the hierarchy depth — X-09).

### 25.5.2 Leave balances

For each employee and each leave type, the current **accrued balance**, the **taken** figure for the current accrual period, and the accrual parameters (rate, carry-over cap, expiry) are migrated as an opening balance with the cutover as-of date. The system must reproduce the same balance the legacy system showed on cutover day; any difference is a reconciliation exception that must be resolved before sign-off.

### 25.5.3 EOS (end-of-service) balances

End-of-service is migrated as **accrued service** and **accrued entitlement amount** as of cutover, plus the parameters needed to continue accruing under the tiered rule (Chapter 24 / Chapter 23 decision table). Because EOS is a long-dated liability, the migrated figure must equal the legacy actuarial/accrued figure exactly; this is a primary reconciliation line.

### 25.5.4 Loan and advance balances

Each outstanding loan/advance is migrated with its **original principal, amount already recovered, outstanding balance, instalment schedule, and priority/class** (Chapter 24 §24.9). The system continues recovery from cutover under the deduction-priority and cap rules — so the first new payroll resumes instalments seamlessly.

### 25.5.5 Year-to-date statutory balances

For tax and social insurance (Chapter 27), the **year-to-date taxable earnings, tax already withheld, and contributions already made** in the current statutory year are migrated as opening YTD values. Without these, mid-year cutover would mis-apply progressive tax brackets and contribution ceilings. This is mandatory whenever go-live is not on the first day of a statutory year.

## 25.6 Historical data

Historical payroll requires an explicit business decision (Chapter 25 open decision; see also Volume 1 review open-decisions register). Two strategies, and a recommended hybrid:

- **Migrate-as-data:** past payslips and pay results are imported as read-only historical records for display and reporting, but are **not** recomputable by the engine (they predate the new rule packages).
- **Re-creatable-from-inputs:** historical periods are reproduced by importing historical inputs plus historical rule packages and re-running the engine. This is far harder, often infeasible for legacy periods where inputs or legacy rules are incomplete, and is rarely justified.

**Recommended:** a hybrid — reproducibility is guaranteed **from go-live forward** (Chapter 24 §24.10); legacy periods are migrated **as read-only archived data** for display, audit, and statutory look-back, clearly flagged as legacy and not engine-reproducible. The retention horizon for legacy data is a business/legal decision.

## 25.7 Parallel run

A parallel run is mandatory and is the primary correctness gate.

For a defined number of consecutive pay cycles (recommended minimum: **two to three full monthly cycles**), payroll is computed in **both** the legacy system (the system of record for actual payment during this window) and the new system (computing in shadow). For each cycle, the two results are compared **line by line, per employee, per component**.

A **variance tolerance** is declared in advance — the recommended target is **zero variance** on net pay and on every statutory figure; any non-zero variance is an exception requiring root-cause and resolution (a data fix, a rule fix, or a documented, approved explanation). The parallel run is considered passed only when the agreed number of consecutive cycles reconcile within tolerance and the exception log is cleared. Employees are paid from the legacy system throughout the parallel window; the new system does not pay anyone until cutover.

## 25.8 Reconciliation

Reconciliation runs at every stage, not only during parallel run. Each migration load and each parallel cycle produces a reconciliation report comparing **control totals** (headcount, total gross, total net, total deductions, total EOS liability, total leave-days liability, total outstanding loans) and **record-level detail** between legacy and new. Reconciliation has formal **sign-off gates**: a named accountable owner (Payroll, Finance, HR) signs off each control area before the programme advances. No gate may be skipped; sign-offs are recorded.

## 25.9 Go-live strategy

Go-live is the controlled switch from legacy to new as the system of record.

The recommended approach is a **big-bang cutover per legal entity / country** (all employees of an entity move together on a chosen date — typically the first day of a pay period and, ideally, the first day of a statutory year to simplify YTD), with **phased rollout across entities** (countries/companies go live in sequence, not all at once), so lessons from the first entity de-risk the rest. A **cutover runbook** defines the exact ordered steps, the responsible owner per step, the timing window, the freeze on legacy changes during cutover, the final delta-reconciliation immediately before switch, and the explicit **go/no-go decision point** with named approvers. The first live payroll after cutover is treated as a high-attention run with extra review (it is, in effect, the final parallel cycle now paid for real).

## 25.10 Rollback strategy

Rollback is the safety net if go-live fails its go/no-go or fails after switch.

Before cutover, a **rollback point** is established: the legacy system is kept fully operational and is not decommissioned until a defined **stabilisation period** (recommended: at least two successful live cycles on the new system) has passed. The rollback runbook defines the conditions that trigger rollback (e.g. unrecoverable reconciliation failure, inability to pay on time), the steps to revert the system of record to legacy, how any data entered into the new system during the failed window is reconciled back, and the communication plan. Legacy decommissioning is itself a gated decision taken only after stabilisation, never at cutover.

## 25.11 Business rules (canonical)

- **BR-25-01** Migration imports meaning, not legacy tables; every value enters through validated, rule-governed loaders.
- **BR-25-02** Migration is configuration-driven and repeatable, reusing the integration/import engine; trial migrations precede production.
- **BR-25-03** Migration proceeds in dependency order; each step is gated by signed-off reconciliation.
- **BR-25-04** Current salary is migrated as component values, not a single number, so the engine reproduces current pay.
- **BR-25-05** Leave, EOS, loan, and YTD statutory balances are migrated as opening balances with a cutover as-of date and must equal legacy figures within tolerance.
- **BR-25-06** Engine reproducibility is guaranteed from go-live forward; legacy history is retained as read-only archived data unless re-creation is explicitly funded.
- **BR-25-07** A parallel run of at least two to three consecutive cycles, reconciled to the declared (target zero) tolerance, is mandatory before cutover.
- **BR-25-08** Every migrated record is tagged with source, batch, and migrated-provenance flag.
- **BR-25-09** Go-live is big-bang per legal entity, phased across entities, governed by a runbook and a named go/no-go decision.
- **BR-25-10** Legacy remains operational through a stabilisation period; rollback is a defined, runnable path until decommissioning is separately approved.

## 25.12 Examples

**Example 1 — EOS opening balance reconciliation.**

```
Employee Sara, hire date 2014-03-01, cutover 2026-07-01.
Legacy accrued EOS entitlement at cutover = 48,750.00.
Migrated as: accrued_service = 12y 4m, accrued_entitlement = 48,750.00 (as-of 2026-07-01).
First new run continues accrual under the tiered EOS decision table.
Reconciliation line: new opening EOS (48,750.00) vs legacy (48,750.00) → variance 0.00 ✔
```

**Example 2 — Mid-year YTD migration so tax brackets stay correct.**

```
Cutover 2026-07-01 (mid statutory year). Employee in a country with progressive tax.
Migrated opening YTD: taxable earnings = 90,000, tax withheld = 7,200.
First new run computes July tax on cumulative YTD (90,000 + July taxable),
not on July alone → correct bracket application. Without YTD migration, July tax
would be under-withheld.
```

**Example 3 — Parallel-run exception.**

```
Cycle 1 comparison: Employee Omar — net pay  new 8,412.00  vs  legacy 8,400.00  (variance +12.00).
Root cause: housing allowance rounding method mismatch (legacy TRUNCATE vs new HALF_UP).
Resolution: set HOUSING component rounding = TRUNCATE for that country (rule fix, versioned).
Re-run cycle 1 → variance 0.00. Exception closed, logged with approver.
```

## 25.13 Future extensibility

Because migration reuses the configuration-driven integration engine, onboarding a new acquired company or a new country later is the same process as the initial migration — define the source mapping, run trial loads, parallel-run, reconcile, cut over — with no new code. The migrated-provenance flags and opening-balance model mean future audits can always separate inherited balances from system-generated ones. The hybrid history strategy means legacy archives from multiple past systems can coexist, each flagged by source.

---

# Chapter 26 — Multi-Currency Framework

## 26.1 Purpose

This chapter specifies how the system handles more than one currency: the exchange-rate master, how rates are effective-dated and sourced, where and how conversion happens, and how multi-currency results are consolidated for reporting. Volume 1 asserts "Multi Currency" and attaches a currency to components, banks, and settlements, but defines no rate management. This chapter closes finding **X-08 (High)**.

Guiding principle:

> Every amount carries its currency. Conversion is always explicit, always dated, and always recorded — never implicit and never lossy.

## 26.2 Business objectives

A multi-country employer pays people in different currencies, holds bank accounts in different currencies, and must report consolidated cost in a single reporting currency. The business needs each employee paid in the correct currency at the correct amount, needs conversions to use the right rate for the right date (so historical payroll reproduces exactly), and needs group-level reporting that rolls many currencies into one without distorting the underlying figures.

## 26.3 Functional requirements

The system shall maintain an **exchange-rate master**: for each currency pair, a series of rates each with an effective date, a rate type, and a source. It shall expose a single conversion function (`CONVERT`, Chapter 23 §23.4.6) used everywhere conversion occurs. It shall define, per use, **which** currency is the basis and **which** rate type and date apply. It shall consolidate multi-currency results into a configurable **reporting currency** for group reporting while preserving the original-currency figures.

## 26.4 Currency concepts

The framework distinguishes several currency roles, each explicit on the relevant entity:

- **Component currency** — the currency in which a payroll component is expressed.
- **Pay currency** — the currency in which an employee is actually paid (may differ from component currency).
- **Account currency** — the currency of the bank account a payment is drawn from.
- **Functional/entity currency** — the accounting currency of a legal entity (Chapter 28).
- **Reporting currency** — the single currency into which group reporting consolidates.

Each amount is always stored with its currency; the role determines when conversion is required.

## 26.5 Exchange-rate master

The rate master holds, per currency pair and effective date: the **rate**, the **rate type**, and the **source**. Rate **types** include at minimum: *spot* (market rate on a date), *period-average* (used for some reporting), *fixed/corporate* (a rate fixed by finance for a period, common for budgeting), and *statutory* (a rate mandated by an authority for tax/contribution conversion). Rate **source** records provenance (e.g. central-bank feed, corporate finance, manual entry) and whether the rate was system-fed or manually entered, with an audit trail.

Rates are **effective-dated**: a rate is valid from its effective date until superseded. The system never overwrites a past rate; a correction is a new dated entry, preserving reproducibility.

## 26.6 Conversion rules

Conversion is governed by explicit rules, never assumed:

Each **conversion point** declares the rate **type** and the **date** to use. The canonical points: payroll conversion uses the **payroll period reference date** and the rate type configured for payroll (often *fixed/corporate* for the period); statutory conversion (tax/contribution in another currency) uses the **statutory** rate type and the date the authority mandates; accounting conversion uses the entity's configured rate type and posting date (Chapter 28); reporting conversion uses the reporting rate type and the report's as-of date.

Conversion precision follows Chapter 24 §24.8: convert at full working precision, round once to the target currency's minor-unit scale, and when converting a multi-line total, use largest-remainder residual allocation so converted lines sum to the converted total. The **rate used** (value, type, date, source) is recorded on every converted transaction, so any converted figure is fully explainable and reproducible.

Triangulation (when no direct pair exists) goes through a declared **pivot currency** (e.g. USD), and both legs' rates are recorded.

## 26.7 Consolidation and reporting

Group reporting consolidates many currencies into the reporting currency using the reporting rate type as of the report date. Reports always preserve and can show **both** the original-currency amount and the converted amount, so consolidation never hides the source figures. Because rates are effective-dated, a historical report re-run for a past date uses that date's rates and reproduces the original consolidated figure.

## 26.8 Business rules (canonical)

- **BR-26-01** Every monetary amount is stored with its currency; arithmetic across currencies is forbidden without explicit conversion.
- **BR-26-02** The exchange-rate master holds dated rates per pair, with rate type and source; past rates are never overwritten.
- **BR-26-03** Every conversion point declares the rate type and date it uses.
- **BR-26-04** Conversion is performed at full working precision and rounded once to the target currency's minor-unit scale; multi-line conversions use residual allocation.
- **BR-26-05** The rate (value, type, date, source) is recorded on every converted transaction.
- **BR-26-06** Where no direct pair exists, conversion triangulates through a declared pivot currency, recording both legs.
- **BR-26-07** Consolidated reports preserve original-currency amounts alongside converted amounts and reproduce historical figures using historical rates.

## 26.9 Examples

**Example 1 — Paying an expatriate in a different currency.**

```
Component currency = USD. Pay currency = EGP. Payroll period 202607.
Salary component total = 5,000.00 USD.
Conversion point = payroll; rate type = fixed/corporate for 202607; rate = 49.50 EGP/USD (source: corporate finance, eff 2026-07-01).
Paid amount = ROUND(5,000.00 × 49.50, 2 [EGP scale], HALF_UP) = 247,500.00 EGP.
Transaction records: 5,000.00 USD → 247,500.00 EGP @ 49.50 (fixed, 2026-07-01, corporate).
```

**Example 2 — Consolidated cost report.**

```
Reporting currency = USD, report as-of 2026-07-31 (reporting rates).
   Entity A net cost = 247,500.00 EGP → @ 0.0202 → 5,000.00 USD
   Entity B net cost =  18,750.00 SAR → @ 0.2666 → 5,000.00 USD
   Consolidated group net cost = 10,000.00 USD
Report shows both original (EGP/SAR) and converted (USD) columns.
```

## 26.10 Future extensibility

New currencies are onboarded by ISO definition; new rate sources (e.g. an automated central-bank feed) attach to the rate master without code change; new rate types can be declared for new purposes. Because conversion is centralised in one function with declared points, any future requirement (hedged rates, dual reporting currencies, hyperinflationary adjustment) is a configuration of rate type and conversion point rather than new logic.

---

# Chapter 27 — Tax & Statutory Framework

## 27.1 Purpose

This chapter specifies income tax and statutory social insurance / pension as a **configuration-driven, country-independent** framework. Volume 1 has a `Taxable` flag and a `Tax` category but no engine, no brackets, no contribution model, and no country handling. This chapter closes finding **X-02 (Critical)**.

Guiding principle, consistent with the whole system:

> Tax and social insurance are not special code. They are rules, brackets, and parameters expressed in the Rule Engine, differing only by configuration per country.

## 27.2 Business objectives

The employer must withhold the correct income tax and the correct social-insurance/pension contributions (employee and employer share) for every country it operates in, remit them to the right authority in the right format, and prove correctness on audit. Because the platform is multi-country and will add countries over time, none of this can be hardcoded to one jurisdiction; each country is onboarded as configuration.

## 27.3 Functional requirements

The system shall model, per country and effective period: **income tax** (brackets/bands, rates, allowances, exemptions, reliefs, and the cumulation method), and **social insurance / pension** (contributory base, employee rate, employer rate, floors, ceilings, and category-based variations). It shall compute these through the Rule Engine using decision tables and formulas (Chapter 23), apply them in the correct payroll phase (Chapter 23 §23.5.5, cumulative Phase 2 for YTD), produce employee and employer contribution amounts, and generate the statutory **remittance files and reports** through the integration engine (Chapter 28 / Volume 1 export engine).

## 27.4 Tax and statutory concepts

The framework is built from country-neutral concepts, each instantiated per country:

- **Statutory scheme** — a named scheme (e.g. an income-tax scheme, a social-insurance scheme) with an authority, a country, an effective period, and a contributory/taxable base definition.
- **Contributory/taxable base** — the set of payroll components that count toward the scheme (driven by component flags such as `Taxable`, and a parallel `Insurable` flag), plus any base cap/floor.
- **Bracket/band table** — a decision table of thresholds and rates (progressive, flat, or regressive), with hit policy (Chapter 23 §23.7).
- **Allowances, exemptions, reliefs** — deductions from the base or from the computed amount, parameterised per country and sometimes per employee (e.g. dependants).
- **Cumulation method** — how the period amount relates to the year: *non-cumulative* (each period independent), *cumulative YTD* (progressive tax reconciled on year-to-date earnings each period), or *annualised* (estimate annual, divide by periods).
- **Contribution shares** — employee share and employer share, each its own rate and ceiling; the employer share is a cost, not a deduction from the employee.

## 27.5 Income tax

Income tax is computed per the country's scheme. For a progressive **cumulative-YTD** scheme (the common hard case), the engine: computes YTD taxable earnings (`YTD` function, Chapter 23) including the migrated opening YTD (Chapter 25 §25.5.5); applies allowances/exemptions; looks up the bracket table to compute YTD tax due; subtracts tax already withheld YTD; and withholds the difference this period. This self-corrects across the year and is why YTD migration at cutover is mandatory. Flat-rate schemes are the same mechanism with a single-row bracket table and a non-cumulative method.

## 27.6 Social insurance / pension

Social insurance is computed on the **insurable base** (which often differs from the taxable base and is frequently capped by a floor and ceiling). The engine applies the **employee contribution** as a prioritised statutory deduction (Chapter 24 §24.9, exempt from the deduction cap unless law states otherwise) and computes the **employer contribution** as an employer cost that posts to accounting (Chapter 28) and remits with the employee share. Category-based variation (e.g. nationals vs. expatriates, or scheme-specific categories) is handled by decision tables keyed on employee attributes, so for example a country where only nationals contribute is pure configuration.

## 27.7 Statutory reporting and remittance

Each scheme defines the **remittance artefacts** it must produce — contribution files, withholding returns, end-of-year certificates — generated through the integration engine as country-specific format packs (the same mechanism as country-specific bank/WPS files, Chapter 28). Amended filings are produced when retro runs change a closed period's statutory figures (Chapter 24 §24.11.1).

## 27.8 Business rules (canonical)

- **BR-27-01** Tax and social insurance are expressed entirely as rules, decision tables, and parameters per country and effective period; nothing is hardcoded to one jurisdiction.
- **BR-27-02** The taxable base and the insurable base are each defined as component sets via component flags, independently of each other.
- **BR-27-03** Progressive tax uses cumulative-YTD computation by default, including migrated opening YTD; flat tax is a single-row table with non-cumulative method.
- **BR-27-04** Employee statutory contributions are prioritised statutory deductions, exempt from the deduction cap unless the country rule states otherwise.
- **BR-27-05** Employer statutory contributions are an employer cost posted to accounting and remitted with the employee share.
- **BR-27-06** Category-based eligibility and rate variation are handled by decision tables on employee attributes.
- **BR-27-07** Statutory remittance files and certificates are generated as country-specific format packs; retro changes trigger amended filings.
- **BR-27-08** Every statutory figure is reproducible: the scheme version, rates, brackets, and YTD basis used are recorded with the result.

## 27.9 Examples

**Example 1 — Progressive cumulative-YTD income tax.**

```
Country X scheme (illustrative), cumulative YTD. Annual bands:
   0 – 40,000      : 0%
   40,001 – 100,000: 10%
   above 100,000   : 20%
Employee, period 202607 (7th period). YTD taxable incl. opening = 120,000. Tax withheld YTD = 6,000.
YTD tax due = 0 + (100,000−40,000)×10% + (120,000−100,000)×20% = 6,000 + 4,000 = 10,000.
Tax this period = 10,000 − 6,000 = 4,000  (withheld in 202607).
```

**Example 2 — Social insurance with ceiling, national-only.**

```
Country Y social insurance. Insurable base = Basic + Housing, capped at ceiling 25,000.
Employee rate 9%, employer rate 12%. Eligibility decision table: nationality = National → contributes; Expat → 0.
National employee, Basic+Housing = 28,000 → capped base = 25,000.
   Employee contribution = 25,000 × 9% = 2,250  (statutory deduction, cap-exempt)
   Employer contribution = 25,000 × 12% = 3,000 (employer cost → GL)
Expat colleague, same salary → 0 / 0 (per eligibility table).
```

## 27.10 Future extensibility

A new country's tax and social insurance is onboarded entirely as configuration: define the scheme, the base component sets, the bracket/contribution decision tables, the cumulation method, the eligibility tables, and the remittance format pack — no code. Mid-year statutory changes are new effective-dated rule versions; historical periods keep computing on their original versions (Chapter 23 §23.8). New scheme kinds (e.g. a training levy, a solidarity contribution) reuse the same scheme/base/table model.

---

# Chapter 28 — Accounting & General Ledger Integration

## 28.1 Purpose

This chapter specifies how payroll results become accounting entries: the posting model, cost dimensions, the structure of payroll journals, how reopened/retro periods reverse and re-post, and how journals reach the target ERP/GL. Volume 1 implies "financial transactions" but defines no posting model. This chapter closes finding **X-14 (High)**.

Guiding principle:

> Every figure that affects money must post to the ledger as a balanced, traceable, reversible entry. Payroll is not finished when it is paid; it is finished when it is correctly accounted for.

## 28.2 Business objectives

Finance must see payroll cost in the general ledger, broken down by the dimensions it manages cost by (entity, cost centre, department, project), with every journal balanced and traceable back to the payroll run and the employee detail behind it. When payroll is corrected (retro/reopen), the accounting must reverse and re-post cleanly so the ledger always reflects truth without manual fixes. And the integration to the ERP/GL must be reliable, idempotent, and reconcilable.

## 28.3 Functional requirements

The system shall translate each payroll run's results into **balanced journal entries** using a configurable **posting-rule mapping** from payroll components to GL accounts and cost dimensions. It shall support **accrual** entries (cost recognised when earned, e.g. provisions) and **payment** entries (cash/clearing when paid). It shall **reverse and re-post** on reopen/retro. It shall transmit journals to the target ERP/GL through the integration engine and reconcile what was sent against what was accepted.

## 28.4 Posting model

Payroll posts to the ledger through posting rules, each mapping a **component (or component group)** plus **conditions** to a **debit/credit treatment**, a **GL account**, and a set of **cost dimensions**. The model recognises the standard payroll accounting shape:

- **Earnings and employer costs** are expenses → **debit** expense accounts (salaries, allowances, employer social-insurance, provision expense).
- **Net pay** owed to employees → **credit** a net-pay liability/clearing account until paid, then cleared by the payment entry.
- **Withholdings** (employee tax, employee social insurance, loan recoveries) → **credit** the respective liability accounts until remitted/applied.
- **Provisions** (EOS, leave) → **debit** provision expense, **credit** provision liability, accrued each period (Chapter 24).

Every journal balances (total debits = total credits) per entity and per currency; cross-currency journals convert via Chapter 26 before posting in the entity's functional currency.

## 28.5 Cost dimensions and allocation

Each posting carries the **cost dimensions** finance reports by: legal entity, cost centre, department, project, and any additional configured dimension. Where a single employee's cost must be split across dimensions (e.g. an employee working across two projects), the system applies a configurable **cost-allocation rule** (Volume 1 already flags "Cost Allocation Required" on components), splitting the amount by the allocation percentages and using residual rounding (Chapter 24 §24.7.4) so allocated parts sum exactly to the posted total.

## 28.6 Journal structure and lifecycle

A payroll posting is produced as a **journal batch** per run, per entity, per period. The batch carries: the source run reference, the period, the entity, the currency, the posting date, and the balanced set of lines (each with account, dimensions, debit/credit, amount, and a back-reference to the payroll detail). Journals move through *Draft → Approved → Posted → (Reversed)*. A journal is only generated after the payroll run is approved (Chapter 24 §24.4, stage 9) and is itself subject to finance approval before transmission.

## 28.7 Reversal and re-post on reopen/retro

When a closed period is reopened or corrected by retro (Chapter 24 §24.11), accounting integrity is preserved by **reversal, not editing**: the original posted journal is **reversed** (an equal-and-opposite balanced entry, dated per finance policy), and the corrected result is **re-posted** as a new journal. Alternatively, where finance prefers, only the **delta** is posted as an adjustment journal — the choice is a configurable policy. Either way, the original journal is never altered, the audit trail is complete, and the ledger nets to the corrected truth. This directly resolves Volume 1 review finding F-22-04 (reopen interactions with posted journals).

## 28.8 ERP/GL transmission and reconciliation

Journals are transmitted to the target ERP/GL through the integration engine using a configurable **GL format pack** (mirroring the bank/WPS and statutory format packs). Transmission is **idempotent** — re-sending the same batch never double-posts, because each batch carries a unique, stable identity the target can deduplicate on. The system tracks each batch's transmission state (sent / accepted / rejected) and **reconciles** the total it sent against the total the ERP accepted, flagging any mismatch. Rejected batches are corrected and re-sent under the same idempotency guarantee.

## 28.9 Business rules (canonical)

- **BR-28-01** Every payroll result posts to the GL as balanced journals (debits = credits) per entity and currency.
- **BR-28-02** Posting is driven by a configurable component-to-account-and-dimension mapping; no account is hardcoded.
- **BR-28-03** Earnings/employer costs debit expense; net pay and withholdings credit liabilities until paid/remitted; provisions accrue each period.
- **BR-28-04** Every posting carries the configured cost dimensions; split costs use allocation rules with residual rounding.
- **BR-28-05** Journals are generated only after payroll approval and require finance approval before transmission.
- **BR-28-06** Corrections reverse-and-re-post (or post a delta adjustment) by policy; original journals are never edited.
- **BR-28-07** GL transmission is idempotent and reconciled (sent vs accepted); rejections are corrected and re-sent safely.
- **BR-28-08** Every journal line back-references the payroll run and employee detail for full traceability.

## 28.10 Examples

**Example 1 — Monthly payroll journal (single employee, simplified).**

```
Entity A, period 202607, functional currency SAR.
  Dr  Salaries expense (CC: Ops, Dept: Logistics)        10,000
  Dr  Employer social insurance expense                   1,200
      Cr  Net pay clearing liability                              8,250
      Cr  Employee tax withheld liability                          900
      Cr  Employee social insurance liability                      900
      Cr  Loan recovery (asset/clearing)                           950
      Cr  Employer SI payable liability                          1,200
  Totals:  Dr 11,200  =  Cr 11,200   ✔ balanced
Later, on payment:  Dr Net pay clearing 8,250 / Cr Bank 8,250.
```

**Example 2 — Reversal on retro.**

```
202607 originally posted salaries expense 10,000.
August: backdated increase makes 202607 salaries = 10,300.
Reverse-and-re-post policy:
   Reversal journal (dated per policy): Dr/Cr opposite of original 202607 batch.
   Re-post journal: corrected 202607 with salaries 10,300.
Net ledger effect = +300 expense; both original and reversal remain visible.
```

## 28.11 Future extensibility

New ERP/GL targets are onboarded as new format packs; new cost dimensions are added to the posting model as configuration; new component types automatically post correctly once mapped. Because posting is rule-driven and transmission is idempotent and reconciled, connecting an additional finance system (e.g. after an acquisition) requires mapping configuration only. The reversal model means any future regulatory restatement requirement is handled by the existing reverse-and-re-post mechanism.

---

# Chapter 29 — Disaster Recovery & High Availability

## 29.1 Purpose

This chapter specifies how the platform stays available under failure and how it recovers from disaster: high availability for every tier (not just the application and database), recovery objectives (RPO/RTO), backup and point-in-time recovery, and the reconciliation of the contradictory concurrency target. Volume 1 covers app-tier and PostgreSQL HA but leaves Redis, RabbitMQ, and MinIO as single points of failure and defines DR mechanisms without recovery objectives. This chapter closes findings **X-07 (High)** and **X-06 (High)**.

Guiding principle:

> Payroll is a deadline business. Availability and recovery objectives are stated as numbers the business has agreed, and every component that can stop payroll has a defined failure and recovery behaviour.

## 29.2 Business objectives

Employees must be paid on time even when infrastructure fails. The business must agree, in advance, how much data it can afford to lose (RPO) and how quickly service must be restored (RTO) — for payroll these are stringent because a missed or wrong payday has legal and reputational cost. Every component that carries state payroll depends on — the rule cache, the event/message backbone, and document/payslip storage — must survive the loss of a node without losing work.

## 29.3 Functional requirements

The system shall run every tier in a **redundant, no-single-point-of-failure** configuration, shall define and meet **RPO/RTO** targets per service tier, shall back up all stateful stores with tested restore and **point-in-time recovery**, shall fail over automatically where possible and through a documented runbook otherwise, and shall be sized against a single, reconciled concurrency figure (§29.7).

## 29.4 High availability per tier

Volume 1's gap was treating only two tiers. Volume 2 requires HA for **all** stateful and stateless tiers:

- **Application tier (stateless):** multiple instances behind a load balancer across availability zones; instances are disposable; loss of one is absorbed without user impact.
- **PostgreSQL (system of record):** a primary with synchronous standby for zero-data-loss failover, plus asynchronous replicas for read scaling and DR in a second site/region. Automatic failover with a defined promotion procedure.
- **Redis (rule cache, sessions, hot data):** run in a replicated, automatically-failing-over configuration (primary/replica with sentinel or a clustered mode). Critically, Redis is treated as a **cache, not a source of truth** — its loss degrades performance but never loses authoritative data, because every cached value (resolved rules, parameters) is rebuildable from PostgreSQL.
- **RabbitMQ (event/message backbone carrying payroll events):** run as a mirrored/quorum cluster so a node loss does not lose messages; messages that drive financial flows are **persistent** and consumed with acknowledgement and idempotency (Chapter 28), so no event is lost or double-processed across a failover.
- **MinIO (documents, payslips, generated files):** run in a distributed, erasure-coded configuration across nodes so object loss is survivable, with replication to the DR site. Payslips and statutory/bank files are durable artefacts and must survive node loss.

No tier may be deployed as a single instance in production.

## 29.5 Recovery objectives (RPO / RTO)

Recovery objectives are stated per service tier and agreed by the business (the actual numbers are an open decision — §29.8 / Volume 1 open-decisions register). The framework and recommended targets:

- **RPO (Recovery Point Objective)** — maximum acceptable data loss. For the payroll system of record (PostgreSQL), the recommended target is **near-zero RPO** via synchronous replication, because losing committed payroll data is unacceptable.
- **RTO (Recovery Time Objective)** — maximum acceptable time to restore service. Recommended tiering: the **core payroll path** has the tightest RTO; reporting and analytics may tolerate a longer RTO.
- Objectives are defined for two scenarios: **component failure** (a node/zone lost — handled by HA, effectively no downtime) and **site/regional disaster** (the whole primary site lost — handled by DR failover to the second site within the agreed RTO).

Objectives are meaningless unless tested; §29.6 makes testing mandatory.

## 29.6 Backup and point-in-time recovery

All stateful stores are backed up: PostgreSQL with continuous archiving enabling **point-in-time recovery** to any moment (essential for recovering from a logical error such as a bad bulk update); Redis state is not backed up as authoritative (it is rebuildable); RabbitMQ persistent queues and MinIO objects are backed up/replicated to the DR site. Backups are **encrypted** (Chapter 31), **retained** per a defined schedule aligned to legal retention, stored **off-site/cross-region**, and — the non-negotiable rule — **restore-tested on a schedule**. An untested backup is treated as no backup. A full **DR drill** (fail over to the second site, run a payroll there, fail back) is performed periodically and is itself a sign-off gate for production readiness.

## 29.7 Concurrency reconciliation

Volume 1 contradicts itself: §1.7 states 5,000 concurrent users; §16.16 states 500. Volume 2 resolves this as a **definition** problem and requires the business to confirm one authoritative figure (§29.8):

The two numbers most likely measure different things. Volume 2 defines the terms so sizing is unambiguous: **named users** (total people with accounts — for 70,000+ employees, potentially tens of thousands if self-service is in scope), **active concurrent sessions** (users logged in at once), and **concurrent requests** (simultaneous in-flight requests — the figure that actually drives connection pools and thread sizing). The recommendation: size the **self-service/read path** for the high figure (5,000+ concurrent sessions, mostly light reads, especially at payslip-release peaks) and the **transactional/admin path** for the lower figure (hundreds of concurrent operational users), and validate both against the batch workload (Volume 1's 3M timesheet records/month and the payroll batch window). The single agreed set of figures becomes the sizing basis for the HA topology above.

## 29.8 Business rules (canonical)

- **BR-29-01** No production tier runs as a single instance; application, PostgreSQL, Redis, RabbitMQ, and MinIO are all deployed redundantly.
- **BR-29-02** Redis is a cache, never a source of truth; all cached values are rebuildable from PostgreSQL.
- **BR-29-03** Financial-flow messages are persistent and consumed idempotently with acknowledgement, surviving broker failover without loss or duplication.
- **BR-29-04** Documents, payslips, and generated files are stored durably (erasure-coded) and replicated to a DR site.
- **BR-29-05** RPO and RTO are defined per service tier and agreed by the business; PostgreSQL targets near-zero RPO via synchronous replication.
- **BR-29-06** All stateful stores are backed up, encrypted, off-site, retained per policy, and restore-tested on a schedule; an untested backup is treated as no backup.
- **BR-29-07** PostgreSQL supports point-in-time recovery; a full DR drill is performed periodically and is a production-readiness gate.
- **BR-29-08** The platform is sized against a single, business-confirmed set of concurrency figures (named users / concurrent sessions / concurrent requests) plus the batch workload.

## 29.9 Examples

**Example 1 — Component failure during a payroll run.**

```
A PostgreSQL primary node fails mid-run.
  → synchronous standby is promoted automatically (near-zero RPO).
  → the payroll run, being stage-based and idempotent (Chapter 24 §24.4),
    resumes from its last completed stage; no committed work is lost.
A RabbitMQ node fails:
  → quorum cluster keeps persistent payroll events; consumers re-read
    unacknowledged messages idempotently; no event lost or double-applied.
User impact: none beyond a brief reconnect.
```

**Example 2 — Site disaster.**

```
Primary site lost.
  → DR site promoted; service restored within the agreed core-payroll RTO.
  → MinIO payslips/bank files available from DR replica.
  → most recent committed data present to the near-zero RPO bound.
A periodic DR drill has already proven a full payroll can run at the DR site.
```

## 29.10 Future extensibility

The redundant, objective-driven design accommodates growth (more app instances, more read replicas) and stricter future objectives (tighter RTO via active-active) without architectural change. New stateful components introduced later inherit the same rule: redundant, backed up, restore-tested, with stated objectives. As employee count or self-service adoption grows, the confirmed concurrency figures are revised and the topology scaled against them.

---

# Chapter 30 — QA, Parallel Payroll & Reproducibility Testing

## 30.1 Purpose

This chapter specifies how the system's correctness is proven and kept proven: the testing strategy across layers, the parallel-payroll method, regression testing of rules, and the reproducibility verification that proves a years-old payroll re-computes identically. Volume 1 names a QA Lead and promises reproducibility but contains no testing chapter. This chapter closes finding **X-10 (High)**.

Guiding principle:

> Correctness is not a one-time event at go-live; it is a property the system continuously proves. Rules are tested with the same rigour as code, and reproducibility is asserted automatically forever.

## 30.2 Business objectives

The business must trust that the system pays correctly before go-live and stays correct after every change. It must be able to demonstrate, on audit or in a dispute, that any historical payroll re-computes to exactly what was paid. And it must be able to change rules confidently, knowing a regression suite will catch any unintended change to results.

## 30.3 Functional requirements

The system shall support layered testing (rule-level, calculation-level, integration-level, end-to-end), a **parallel-payroll** comparison capability (reused from migration, Chapter 25 §25.7), an automatically-run **regression suite** over rules and golden results (Chapter 23 §23.10), and an automated **reproducibility self-test** that re-runs archived snapshots and asserts identical results (Chapter 24 §24.10).

## 30.4 Testing strategy by layer

Testing is layered so failures are localised:

- **Rule tests** (Chapter 23 §23.10) — each rule has unit tests; rule changes trigger them automatically. This is the first and cheapest line of defence.
- **Calculation tests** — representative employee profiles (national/expat, hourly/salaried, mid-period joiner/leaver, with loans/leave/overtime) are run through the full payroll calculation and asserted against hand-verified expected results. These cover the interactions Chapter 24 specifies (proration, caps, rounding, priority).
- **Integration tests** — verify the boundaries: bank/WPS file generation, GL posting (Chapter 28), statutory file generation (Chapter 27), and that idempotency/reconciliation hold.
- **End-to-end tests** — a full payroll lifecycle (Chapter 24 §24.4) from input collection to closed period and generated outputs, including approval and lock controls.
- **Non-functional tests** — load tests against the confirmed concurrency figures (Chapter 29 §29.7), batch-window tests against the 3M-timesheet workload, and security tests (Chapter 31).

## 30.5 Parallel payroll

Parallel payroll is the headline correctness gate and is used in two contexts: at **go-live** (legacy vs new, Chapter 25 §25.7) and at **major change** (current production rules vs candidate rules, via simulation A/B, Chapter 23 §23.11). In both, results are compared line-by-line, per employee, per component, against a declared variance tolerance (target zero), with every non-zero variance root-caused and resolved or formally explained. A parallel run passes only when the agreed number of cycles reconcile within tolerance and the exception log is clear.

## 30.6 Regression and golden results

Every known-correct payroll result is captured as a **golden result** (Chapter 23 §23.10). The regression suite re-runs golden inputs after any change to rules, the function library, or the engine, and asserts the outputs are unchanged. A change that alters a golden result is blocked until someone with authority explicitly acknowledges, with an audit note, that the change of result is intended (e.g. a deliberate rule change) — distinguishing intended changes from accidental regressions. This makes "configure confidently" real.

## 30.7 Reproducibility testing

Reproducibility is verified automatically, not assumed. The **reproducibility self-test** periodically selects archived payroll snapshots (Chapter 24 §24.10) — including old ones — re-runs the engine against each snapshot's frozen inputs and rule-package version, and asserts the recomputed result is **bit-for-bit identical** to the originally stored result. Because snapshots freeze inputs, rule versions, the function-library version, and the bases/rates used, a passing self-test proves the reproducibility guarantee holds even across software upgrades. Any divergence is a high-severity defect: it means an upgrade changed behaviour that should have been frozen, and it is investigated before the upgrade is accepted into production. This is the concrete, testable mechanism behind Volume 1's "reproduce payroll years later" promise.

## 30.8 Business rules (canonical)

- **BR-30-01** Every rule has tests that run automatically on change; no rule activates without a passing test (Chapter 23).
- **BR-30-02** Representative employee profiles are calculation-tested against hand-verified results covering proration, caps, rounding, and priority.
- **BR-30-03** Parallel payroll (line-by-line, per employee, per component, target-zero tolerance) gates go-live and major rule changes.
- **BR-30-04** Golden results are captured and regression-tested on every change; an intended change to a golden result requires explicit, audited acknowledgement.
- **BR-30-05** A reproducibility self-test periodically re-runs archived snapshots and asserts bit-for-bit identical results; divergence blocks the offending upgrade.
- **BR-30-06** Non-functional tests validate the confirmed concurrency and batch-window targets and security controls.

## 30.9 Examples

**Example 1 — A rule change caught by regression.**

```
A developer changes the function library's ROUND implementation.
Regression suite re-runs 5,000 golden results.
  → 1,287 goldens now differ by ±0.01.
Build is blocked. Investigation shows the change was unintended.
The library change is reverted; goldens unchanged. No production impact.
```

**Example 2 — Reproducibility self-test after an upgrade.**

```
After a platform upgrade, the self-test re-runs the 202401 snapshot.
  Stored net for employee #44213 = 7,412.50.
  Recomputed net                  = 7,412.50.   ✔ identical.
All sampled snapshots reproduce exactly → upgrade accepted.
(If any had differed, the upgrade would be rejected and investigated.)
```

## 30.10 Future extensibility

The golden/regression mechanism grows automatically — every new known-correct run becomes a future guardrail. New countries and rule packs bring their own rule tests and goldens, so quality scales with scope. The reproducibility self-test covers all snapshots regardless of age or country, so the guarantee extends to every jurisdiction onboarded later without new test infrastructure.

---

# Chapter 31 — Security Deep Design

## 31.1 Purpose

This chapter specifies the data-protection layer of the system: database-enforced row-level security for multi-tenant and scope isolation, encryption of sensitive data, key and secret management, segregation of duties across the payroll lifecycle, and the resolution of the conflict between "never delete" and data-erasure obligations. Volume 1 covers RBAC, MFA, and audit at the access layer but leaves the data-protection mechanics undefined. This chapter closes findings **X-11 (High)** and **X-12 (High)** and Volume 1 review findings **F-22-01, F-22-03, F-22-04, F-22-05**.

Guiding principle:

> Access control decides who may ask; data protection decides what they can possibly see and proves no one tampered. Both are enforced, the second one in the database itself — not only in application code.

## 31.2 Business objectives

Salary and personal data are among the most sensitive an organisation holds. A user must never see data outside their authorised company, entity, or scope — and that boundary must hold even if application code has a bug, so it is enforced at the database. Sensitive data must be unreadable if storage or backups are stolen. The keys that protect data must themselves be protected and rotatable. No single person should be able to both create and approve a payment. And the system must honour legal data-erasure rights without breaking the immutable audit and payroll history that law also requires.

## 31.3 Functional requirements

The system shall enforce **scope isolation at the database** via row-level security in addition to service-layer checks; **encrypt** sensitive data at rest and in transit; manage **keys and secrets** in a dedicated vault with rotation; enforce **segregation of duties** through an SoD matrix mapped to roles; and reconcile **erasure vs. retention** through a defined technique that satisfies both. All security-relevant events are audited immutably.

## 31.4 Row-level security (RLS)

Scope isolation (Volume 1 §22.7, review finding F-22-01) is enforced in the **database**, not only in the application. Every row of scoped data carries the dimensions that define its ownership (legal entity / company, and finer scope as configured). The database applies **row-level security policies** that restrict every query to the rows permitted by the **current security context**, which is established from the authenticated user's authorisation and passed into the database session for each request. The effect: even a flawed query, a mistaken join, or a compromised service path cannot return rows outside the caller's scope, because the database itself filters them. Application-layer checks remain (defence in depth), but the database is the last, unbypassable line. Highly-privileged break-glass access that can bypass RLS is itself tightly controlled, time-boxed, and heavily audited (§31.7).

## 31.5 Encryption

Encryption is applied in three places: **in transit** (all traffic — client-to-server and service-to-service and to data stores — uses current TLS); **at rest** (the database, document store/MinIO, message store, and backups are encrypted at the storage level); and **field-level** for the most sensitive attributes (bank account numbers, national IDs, and salary where policy requires), so that even a database administrator with raw table access cannot read them without the field keys. Field-level encryption is applied to a defined, configurable set of sensitive attributes. Encryption must never break reproducibility: encrypted values decrypt to identical plaintext, so calculations and snapshots are unaffected.

## 31.6 Key management and secrets

Keys and secrets are never stored in code, configuration files, or the database in plaintext. They live in a dedicated **secrets/key-management vault**. The design requires: a clear **key hierarchy** (a master key protecting data-encryption keys), **rotation** of keys on a schedule and on compromise without re-encrypting the world (envelope encryption — rotate the key-encrypting key, not every record), **separation** so that those who administer the database cannot also retrieve the keys (a control that makes at-rest encryption meaningful), and full **audit** of key access. Application secrets (database credentials, integration credentials, signing keys) are issued from the vault, short-lived where possible, and rotated. Token lifetimes, refresh, and revocation are defined so that a stolen token's blast radius is bounded (review finding F-22-03).

## 31.7 Segregation of duties (SoD)

No single person controls a payment end to end (review finding F-22-05). The system enforces an **SoD matrix** mapping incompatible duties to distinct roles. The canonical incompatibilities:

- The person who **runs** a payroll cannot **approve** it (Chapter 24 §24.4).
- The person who **authors** a rule cannot **approve/activate** it (Chapter 23 §23.9).
- The person who **enters** a bank account or payment instruction cannot **release** the payment file.
- **Reopening** a locked period requires dual approval (Volume 1 §22.10).
- Granting **privileged/break-glass** access requires approval by someone other than the requester, is time-boxed, and is fully audited.

The matrix is configuration; the system blocks any action that would violate it and records the attempt.

## 31.8 Erasure vs. never-delete (GDPR / data-protection reconciliation)

Volume 1's "never delete" principle conflicts with legal erasure rights (review finding X-11). Volume 2 reconciles them with **crypto-shredding and pseudonymisation** rather than physical deletion:

Personal data that may be subject to erasure is stored field-encrypted with a **per-subject key**. To honour an approved erasure request, the system **destroys that subject's key** (crypto-shredding): the encrypted personal data becomes permanently unreadable, satisfying erasure, while the **structural and financial records remain intact** — amounts, audit trail, and accounting integrity are preserved (now referencing a pseudonymous subject). This satisfies both obligations at once: the law's right to erasure of personal data, and the law's (and audit's) requirement to retain financial and statutory records. Which data is erasable, the legal basis, retention periods, and the approval workflow for an erasure request are all defined as policy; legal must approve crypto-shredding as a valid erasure method for the operating jurisdictions (an open decision, §31.10 / Volume 1 open-decisions register). Audit records of *who did what* are themselves retained (they are not the data subject's personal data in the erasable sense) so tamper-evidence is never lost.

## 31.9 Business rules (canonical)

- **BR-31-01** Scope/tenant isolation is enforced by database row-level security driven by the request's security context, in addition to application checks.
- **BR-31-02** No query can return rows outside the caller's authorised scope, even if application code is flawed.
- **BR-31-03** Data is encrypted in transit, at rest, and at field level for a defined set of sensitive attributes; encryption never alters decrypted values or reproducibility.
- **BR-31-04** Keys and secrets live in a vault, follow a key hierarchy with envelope encryption, are rotatable without mass re-encryption, and are auditable; DB administrators cannot retrieve data keys.
- **BR-31-05** Token lifetime, refresh, and revocation are defined to bound the impact of credential theft.
- **BR-31-06** An SoD matrix enforces that run≠approve, author≠activate, enter≠release, reopen needs dual approval, and break-glass needs independent approval; violations are blocked and logged.
- **BR-31-07** Erasure is honoured by crypto-shredding the subject's key and pseudonymising, preserving financial/audit integrity; legal approves the method per jurisdiction.
- **BR-31-08** All security-relevant events, key access, privileged access, and SoD-violation attempts are recorded in an immutable audit trail.

## 31.10 Examples

**Example 1 — RLS prevents cross-entity leakage despite a code bug.**

```
A reporting query accidentally omits the company filter.
Caller's security context: company = A only.
Database RLS policy still restricts results to company A rows.
Result: company B data is never returned, despite the application bug.
```

**Example 2 — Erasure via crypto-shredding.**

```
Approved erasure request for a former employee.
  → destroy that subject's field-encryption key.
  → name, national ID, bank details become permanently unreadable.
  → payroll history amounts, GL postings, audit trail remain intact,
    now referencing pseudonymous subject #ANON-90431.
Erasure satisfied; financial and statutory retention satisfied.
```

**Example 3 — SoD block.**

```
User Maha ran the 202607 payroll. Maha then attempts to approve it.
System response: BLOCKED — "run and approve are segregated duties."
Attempt is logged. A different authorised approver must approve.
```

## 31.11 Future extensibility

The RLS context model extends to new scope dimensions by configuration. The key hierarchy and per-subject-key model support new sensitive attributes and new jurisdictions' erasure rules without re-architecting. The SoD matrix is data, so new incompatible-duty pairs are added as policy. As new regulations emerge, the encryption/erasure/audit foundation accommodates them through configuration and policy rather than redesign.

---

# Chapter 32 — Enterprise Operational Guidelines

## 32.1 Purpose

This chapter specifies how the system is observed and operated, the depth of the configurable workflow/approval engine, the scope and boundaries of employee/manager self-service and mobile, and the reconciliation of the organisation-hierarchy depth discrepancy. It gathers the remaining medium-and-high operability findings: **X-18 (observability), X-16 (workflow engine depth), X-17 (self-service/mobile scope)**, and **X-09 (org-hierarchy mismatch)**.

Guiding principle:

> A system that runs payroll for 70,000+ people across countries must be observable, operable, and governable by configuration — including its own organisational shape, approval flows, and self-service surface.

## 32.2 Business objectives

Operations must be able to see the health of the platform and the progress of every payroll run, and be alerted before problems affect a payday. Approvals must match each organisation's real governance without code changes. Employees and managers must self-serve routine actions to remove load from HR, within clearly bounded scope. And the system's model of the organisation must be internally consistent so reporting, scoping, and approvals align.

## 32.3 Functional requirements

The system shall expose **observability** (metrics, logs, traces, health, and business-level payroll-run monitoring with alerting and SLOs); a **configurable workflow engine** for approvals; a defined, bounded **self-service and mobile** capability; and a single, reconciled **organisation hierarchy** used consistently across domain, scoping, reporting, and physical model.

## 32.4 Observability and SLOs

Observability has two layers. **Technical observability**: metrics (throughput, latency, error rates, queue depths, connection-pool saturation), centralised structured logs, distributed traces across services, and health checks per tier — aligned with Volume 1's Prometheus/Grafana/ELK stack. **Business observability**: the state and progress of every payroll run (which stage, how many employees processed, exception counts), integration delivery status (bank/WPS/GL/statutory files sent/accepted/rejected, Chapters 26–28), and rule-change activity. **Service Level Objectives** are defined for the paths that matter — payroll batch completion within its window, self-service availability at payslip-release peaks, integration delivery deadlines — and **alerting** fires on objective breach or leading indicators (e.g. a payroll run trending to miss its window), so operators act before a payday is at risk. Every alert maps to a runbook.

## 32.5 Workflow and approval engine

Approvals across the system (leave, overtime, salary changes, payroll approval, rule activation, reopen, off-cycle, erasure requests) are driven by one **configurable workflow engine**, not hardcoded per feature (review finding X-16). A workflow is defined as configuration: the **stages**, the **approver(s)** per stage (by role, by organisational position, or by rule-resolved selection), **conditions** (e.g. amount thresholds that add an extra approval level), **escalation/timeout** behaviour, **delegation** (acting approvers during absence), and **parallel vs. sequential** approval. The engine enforces the SoD matrix (Chapter 31) so a configured workflow can never assign incompatible duties to one person. Because workflows are configuration, each company/country can model its own governance without a release.

## 32.6 Self-service and mobile scope

Self-service is explicitly bounded (review finding X-17) so scope and load are controllable. **Employee self-service** (in scope): view payslips and statutory certificates, view leave balances and request leave, view personal/team calendar, submit timesheets/attendance where applicable, update permitted personal data (subject to approval workflow), and view total-reward information. **Manager self-service** (in scope): approve/reject requests routed to them, view their team's data within their scope (enforced by RLS, Chapter 31), and initiate permitted transactions. **Out of scope for self-service** (admin only): rule authoring, payroll execution/approval, configuration, and any action with financial-release authority. **Mobile** delivers a defined subset of self-service (payslips, leave request/approval, calendar, notifications) — the read-and-approve actions that benefit most from mobility — while administrative and payroll-execution functions remain on the full application. The high-concurrency sizing of Chapter 29 §29.7 is driven primarily by this self-service/mobile read surface, especially at payslip-release peaks.

## 32.7 Organisation hierarchy reconciliation

Volume 1 is internally inconsistent: §2.4 defines a 7-level hierarchy (Company → Business Unit → Division → Department → Section → Team → Employee) while the physical/blueprint chapters model only Business Unit, Department, Section (review finding X-09). Volume 2 reconciles this with a **single, configurable hierarchy definition** used everywhere:

The organisation is modelled as a **configurable tree of organisational-unit types**, not a fixed set of named levels. The canonical full depth is the 7 levels of §2.4, but each level beyond the mandatory ones (Company, and the employee leaf) is **optional and configurable per company** — so an organisation that does not use Division and Team simply does not instantiate those levels, and one that needs additional levels can extend the tree. The **same** hierarchy is the basis for the override hierarchy (Chapter 23 §23.6, where Business Unit/Project/etc. appear), for security scoping (Chapter 31), for cost dimensions (Chapter 28), and for reporting (Chapter 26). The earlier physical/domain mismatch is resolved by making the hierarchy data-driven rather than hardcoding three or seven levels. This must be settled before the data model is finalised (it is a Phase-0/Phase-3 gate in the review's roadmap, and an open decision — which levels are mandatory for the launch entities).

## 32.8 Business rules (canonical)

- **BR-32-01** The platform exposes technical and business observability, with SLOs on payroll-batch, self-service, and integration paths, and alerting that fires before a payday is at risk; every alert maps to a runbook.
- **BR-32-02** All approvals run on one configurable workflow engine; stages, approvers, conditions, escalation, delegation, and parallelism are configuration.
- **BR-32-03** The workflow engine enforces the SoD matrix; no configured workflow may assign incompatible duties to one person.
- **BR-32-04** Self-service scope is explicitly bounded; rule authoring, payroll execution/approval, configuration, and financial release are admin-only.
- **BR-32-05** Mobile delivers a defined read-and-approve subset; administrative/payroll-execution functions remain on the full application.
- **BR-32-06** The organisation is a single configurable hierarchy tree used identically for override resolution, security scoping, cost dimensions, and reporting; optional levels are instantiated per company.

## 32.9 Examples

**Example 1 — Conditional approval workflow.**

```
Workflow: SALARY_CHANGE
  Stage 1: line manager approves.
  Condition: if increase % > PARAM(SALARY_APPROVAL_THRESHOLD) → add Stage 2: HR Director.
  Escalation: if a stage is not actioned in 3 business days → escalate to next level.
  SoD: the requester can never be an approver of their own request.
```

**Example 2 — Configurable hierarchy per company.**

```
Company A uses: Company → Business Unit → Department → Section → Employee (Division, Team not used).
Company B uses: Company → Business Unit → Division → Department → Section → Team → Employee (full).
Both are the same tree definition; each instantiates the levels it needs.
Override resolution, scoping, costing, and reporting use whichever levels exist.
```

## 32.10 Future extensibility

New organisational levels, new approval workflows, new self-service actions, and new SLOs are all configuration. As the platform adds countries and companies (including acquisitions), each brings its own hierarchy shape, governance workflows, and self-service policy without code change. The observability foundation extends to new services and new business processes by instrumentation and SLO definition, not redesign.

---

# Closing — Volume 2 completeness statement

Volume 2 closes every Critical and High finding from the Architecture Review Report, plus the related medium findings on hierarchy, workflow, self-service, and observability:

| Finding | Severity | Closed in |
|---|---|---|
| X-01 Data migration & cutover | Critical | Ch. 25 |
| X-02 Tax & social insurance | Critical | Ch. 27 |
| X-03 Proration / daily-rate / rounding | Critical | Ch. 24 |
| X-04 Rule Engine semantics | Critical | Ch. 23 |
| X-05 Deduction caps / negative net | High | Ch. 24 |
| X-06 Concurrent-user contradiction | High | Ch. 29 |
| X-07 Infra SPOFs / RPO-RTO | High | Ch. 29 |
| X-08 Multi-currency FX | High | Ch. 26 |
| X-09 Org-hierarchy mismatch | Med→High | Ch. 32 |
| X-10 QA / reconciliation / reproducibility | High | Ch. 30 |
| X-11 GDPR vs never-delete | High | Ch. 31 |
| X-12 Multi-tenant isolation | High | Ch. 31 |
| X-13 Bi-temporal model | High | Ch. 23 & 24 |
| X-14 GL / accounting integration | High | Ch. 28 |
| X-16 Workflow engine depth | Medium | Ch. 32 |
| X-17 Self-service / mobile scope | Medium | Ch. 32 |
| X-18 Observability | Low-Med | Ch. 32 |
| F-22-01/03/04/05 Security mechanics | High/Med | Ch. 31 (and 28 for F-22-04) |

**Decisions still owned by the business.** Volume 2 specifies the mechanisms; a small set of choices remain with the client because only they can make them: the in-scope countries and their launch order; the canonical proration convention(s) per country; the statutory deduction-cap and EOS rules per country; the historical-payroll strategy (recommended: reproducible from go-live, legacy archived read-only); the RPO/RTO numbers; the authoritative concurrency figures; the mandatory organisation-hierarchy levels for launch entities; legal approval of crypto-shredding as an erasure method; the GL/ERP target and posting policy; and the parallel-run acceptance tolerance and cycle count. These are catalogued in the Architecture Review Report's open-decisions register and should be resolved in the sequence given there.

**Constraint compliance.** Consistent with the project constraint, this volume contains no source code, no SQL, no database table (DDL) definitions, and no API definitions. Every specification is expressed as business rules, functional and technical requirements, and worked examples — the level of detail required so that the PostgreSQL schema, the Spring Boot backend, and the React frontend can be generated from Volumes 1 and 2 together without inventing additional business logic.

*End of Volume 2.*

