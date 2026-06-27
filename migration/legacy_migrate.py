#!/usr/bin/env python3
"""
Legacy FoxPro/DBF -> HRMS (PostgreSQL) data migration.

Reads the DBF snapshot produced by the legacy HR stored procedure
(Sp_ImportHRMSData output, exported as payresulth/payresultd/nation/currency/
dependants .dbf) and loads it into the new HRMS schema:

    payresulth.dbf  ->  employee + contract (+ employee_document for PERSONALNO)
    payresultd.dbf  ->  payroll_component (master) + contract_pay_item
    dependants.dbf  ->  employee_dependent

Design decisions (current-state cutover):
  * Only the CURRENT active state of each employee is migrated. The legacy
    action-sheet history/cancellation/closing logic is NOT reproduced; the
    action-sheet number (ACTSHT_NO) is preserved on each pay item's `remarks`
    for traceability.
  * IDEMPOTENT: re-running with a fresh snapshot updates existing rows instead
    of duplicating. Natural keys:
        employee            (company_id, employee_number=BADGE_CD)
        contract            contract_number = 'MIG-<BADGE_CD>'
        payroll_component   (company_id, code='LEG<CODE>')
        contract_pay_item   (contract_id, pay_component_id, effective_from)
        employee_document   (employee_id, document_type, document_number)
        employee_dependent  (employee_id, full_name)
  * All rows are stamped created_by/updated_by = 'legacy-migration'.

Usage:
    python legacy_migrate.py --src ../20170418                 # migrate
    python legacy_migrate.py --src ../20170418 --dry-run       # preview only

DB connection (used unless --dry-run) is read from env, matching the app:
    SPRING_DATASOURCE_URL  (jdbc:postgresql://host:port/db)  [or PGHOST/PGPORT/PGDATABASE]
    SPRING_DATASOURCE_USERNAME / PGUSER
    SPRING_DATASOURCE_PASSWORD / PGPASSWORD
    HRMS_COMPANY_ID   (default 00000000-0000-0000-0000-0000000000c1)
"""

import argparse
import os
import re
import sys
import uuid as _uuid
from collections import Counter, defaultdict
from datetime import date

from dbfread import DBF

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from nationality_map import to_iso2  # noqa: E402

DEFAULT_COMPANY_ID = "00000000-0000-0000-0000-0000000000c1"
MIG_USER = "legacy-migration"
EPOCH = date(2000, 1, 1)  # fallback effective_from for master data


def new_id():
    """String UUID (psycopg2 adapts str cleanly without register_uuid)."""
    return str(_uuid.uuid4())


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def s(v):
    """Trimmed string or None."""
    if v is None:
        return None
    t = str(v).strip()
    return t or None


def split_name(full):
    """Legacy splits on the first space: first token -> first_name, rest -> last_name."""
    full = re.sub(r"\s+", " ", (full or "").strip())
    if not full:
        return ("Unknown", "Unknown")
    parts = full.split(" ", 1)
    first = parts[0]
    last = parts[1] if len(parts) > 1 else first
    return (first[:100], last[:100])


def map_gender(v):
    v = s(v)
    if not v:
        return None
    return {"M": "MALE", "F": "FEMALE"}.get(v.upper(), None)


def map_marital(v):
    v = s(v)
    if not v:
        return None
    return {"S": "SINGLE", "M": "MARRIED", "D": "DIVORCED", "W": "WIDOWED"}.get(v.upper(), None)


def map_frequency(v):
    v = s(v)
    if not v:
        return "MONTHLY"
    u = v.upper()
    if u.startswith("DAIL"):
        return "DAILY"
    if u.startswith("WEEK"):
        return "WEEKLY"
    if u.startswith("MONTH"):
        return "MONTHLY"
    if u.startswith("ANNUAL") or u.startswith("YEAR"):
        return "ANNUAL"
    return "MONTHLY"


def component_meta(code, descr):
    """Derive payroll_component master attributes from a legacy pay code."""
    code = (code or "").strip()
    descr = (descr or "").strip() or f"Component {code}"
    comp_code = f"LEG{code}" if code else "LEG_UNK"
    # legacy '00' is basic salary; everything else is treated as an allowance/earning
    category = "SALARY" if code == "00" else "ALLOWANCE"
    return comp_code[:50], descr[:150], category


# ---------------------------------------------------------------------------
# Extract
# ---------------------------------------------------------------------------
def load_dbf(path):
    return list(DBF(path, load=True, ignore_missing_memofile=True))


def read_source(src):
    def p(name):
        return os.path.join(src, name)

    header = load_dbf(p("payresulth.dbf"))
    detail = load_dbf(p("payresultd.dbf"))
    deps = []
    dep_path = p("dependants.dbf")
    if os.path.exists(dep_path):
        try:
            deps = load_dbf(dep_path)
        except Exception as e:  # noqa: BLE001
            print(f"  (warning: could not read dependants.dbf: {e})")
    return header, detail, deps


# ---------------------------------------------------------------------------
# Transform -> a plan of plain dicts the loader can apply.
# ---------------------------------------------------------------------------
def transform(header, detail, deps, company_id):
    detail_by_badge = defaultdict(list)
    for d in detail:
        detail_by_badge[str(d.get("BADGE_CD")).strip()].append(d)

    deps_by_badge = defaultdict(list)
    for d in deps:
        deps_by_badge[str(d.get("BADGE_CD")).strip()].append(d)

    # 1) distinct payroll components from the detail lines
    components = {}  # comp_code -> dict
    for d in detail:
        comp_code, name, category = component_meta(d.get("CODE"), d.get("DESCR"))
        calc = "FIXED" if s(d.get("TYPE")) == "AM" else "PERCENTAGE"
        if comp_code not in components:
            components[comp_code] = {
                "code": comp_code,
                "name": name,
                "category": category,
                "component_type": "EARNING",
                "payment_frequency": map_frequency(d.get("FREQUENCY")),
                "calculation_method": calc,
                "currency_code": s(d.get("CURRENCY")),
                "effective_from": d.get("EFF_FROM") or EPOCH,
            }

    employees = []
    warnings = []
    for h in header:
        badge = s(h.get("BADGE_CD")) or s(h.get("BADGE_CHR"))
        if not badge:
            warnings.append("Skipped a header row with no BADGE_CD")
            continue
        first, last = split_name(h.get("NAME"))
        nat_legacy = s(h.get("NATION"))
        iso2 = to_iso2(nat_legacy) if nat_legacy else None
        if nat_legacy and not iso2:
            warnings.append(f"Badge {badge}: unmapped nationality '{nat_legacy}' -> stored NULL")

        hire = h.get("DTCONTRACT") or h.get("EFF_FROM")
        if not hire:
            warnings.append(f"Badge {badge}: no DTCONTRACT/EFF_FROM -> hire_date defaulted to {EPOCH}")
            hire = EPOCH

        lines = detail_by_badge.get(badge, [])
        # base currency: header currency, else first detail currency
        base_ccy = s(h.get("CURRENCY")) or (s(lines[0].get("CURRENCY")) if lines else None)

        emp = {
            "employee_number": badge,
            "first_name": first,
            "last_name": last,
            "nationality_country_code": iso2,
            "date_of_birth": h.get("DTBIRTH"),
            "gender": map_gender(h.get("SEX")),
            "hire_date": hire,
            "termination_date": h.get("DTERMINATE"),
            "status": "ACTIVE" if h.get("ACTIVE") else "TERMINATED",
            "contract": {
                "contract_number": f"MIG-{badge}",
                "contract_type": os.environ.get("DEFAULT_CONTRACT_TYPE", "PERMANENT"),
                "effective_from": hire,
                "effective_to": h.get("CONTR_END"),
                "base_currency_code": base_ccy,
            },
            "documents": [],
            "pay_items": [],
            "dependents": [],
        }

        # PERSONALNO -> document
        personal_no = s(h.get("PERSONALNO"))
        if personal_no:
            emp["documents"].append({
                "document_type": "PERSONAL_NO",
                "document_number": personal_no[:100],
                "issuing_country_code": iso2,
            })

        # pay items
        for d in lines:
            comp_code, _, _ = component_meta(d.get("CODE"), d.get("DESCR"))
            amount = d.get("AMOUNT")
            if amount is None:
                warnings.append(f"Badge {badge}: pay line {comp_code} has NULL amount -> skipped")
                continue
            emp["pay_items"].append({
                "component_code": comp_code,
                "amount": amount,
                "currency_code": s(d.get("CURRENCY")),
                "effective_from": d.get("EFF_FROM") or hire,
                "effective_to": d.get("EFF_TO"),
                "remarks": ("Action Sheet " + s(d.get("ACTSHT_NO"))) if s(d.get("ACTSHT_NO")) else None,
            })

        # dependents
        for dp in deps_by_badge.get(badge, []):
            nm = s(dp.get("NAME"))
            if not nm:
                continue
            emp["dependents"].append({
                "full_name": nm[:150],
                "relationship": s(dp.get("RELATION")) or None,
                "date_of_birth": dp.get("DOB") or dp.get("DTBIRTH"),
            })

        employees.append(emp)

    return {"company_id": company_id, "components": components,
            "employees": employees, "warnings": warnings}


# ---------------------------------------------------------------------------
# Load (PostgreSQL)
# ---------------------------------------------------------------------------
def load(plan, conn):
    cur = conn.cursor()
    company_id = plan["company_id"]
    stats = Counter()

    # --- components ---
    comp_ids = {}
    for c in plan["components"].values():
        cur.execute(
            "SELECT id FROM payroll_component WHERE company_id=%s AND code=%s",
            (company_id, c["code"]),
        )
        row = cur.fetchone()
        if row:
            comp_ids[c["code"]] = row[0]
            cur.execute(
                """UPDATE payroll_component
                      SET name=%s, category=%s, payment_frequency=%s,
                          calculation_method=%s, currency_code=%s,
                          updated_at=now(), updated_by=%s
                    WHERE id=%s""",
                (c["name"], c["category"], c["payment_frequency"],
                 c["calculation_method"], c["currency_code"], MIG_USER, row[0]),
            )
            stats["component_updated"] += 1
        else:
            cid = new_id()
            cur.execute(
                """INSERT INTO payroll_component
                       (id, company_id, code, name, category, component_type,
                        payment_frequency, calculation_method, currency_code,
                        effective_from, status, created_by)
                   VALUES (%s,%s,%s,%s,%s,'EARNING',%s,%s,%s,%s,'ACTIVE',%s)""",
                (cid, company_id, c["code"], c["name"], c["category"],
                 c["payment_frequency"], c["calculation_method"],
                 c["currency_code"], c["effective_from"], MIG_USER),
            )
            comp_ids[c["code"]] = cid
            stats["component_inserted"] += 1

    # --- employees ---
    for emp in plan["employees"]:
        cur.execute(
            "SELECT id FROM employee WHERE company_id=%s AND employee_number=%s",
            (company_id, emp["employee_number"]),
        )
        row = cur.fetchone()
        if row:
            emp_id = row[0]
            cur.execute(
                """UPDATE employee SET first_name=%s,last_name=%s,
                          nationality_country_code=%s,date_of_birth=%s,gender=%s,
                          hire_date=%s,termination_date=%s,status=%s,
                          updated_at=now(),updated_by=%s
                    WHERE id=%s""",
                (emp["first_name"], emp["last_name"], emp["nationality_country_code"],
                 emp["date_of_birth"], emp["gender"], emp["hire_date"],
                 emp["termination_date"], emp["status"], MIG_USER, emp_id),
            )
            stats["employee_updated"] += 1
        else:
            emp_id = new_id()
            cur.execute(
                """INSERT INTO employee
                       (id,company_id,employee_number,first_name,last_name,
                        nationality_country_code,date_of_birth,gender,hire_date,
                        termination_date,status,created_by)
                   VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)""",
                (emp_id, company_id, emp["employee_number"], emp["first_name"],
                 emp["last_name"], emp["nationality_country_code"], emp["date_of_birth"],
                 emp["gender"], emp["hire_date"], emp["termination_date"],
                 emp["status"], MIG_USER),
            )
            stats["employee_inserted"] += 1

        # --- contract (one per employee, keyed by contract_number) ---
        ct = emp["contract"]
        cur.execute(
            "SELECT id FROM contract WHERE employee_id=%s AND contract_number=%s",
            (emp_id, ct["contract_number"]),
        )
        row = cur.fetchone()
        if row:
            contract_id = row[0]
            cur.execute(
                """UPDATE contract SET contract_type=%s,effective_from=%s,
                          effective_to=%s,base_currency_code=%s,
                          updated_at=now(),updated_by=%s WHERE id=%s""",
                (ct["contract_type"], ct["effective_from"], ct["effective_to"],
                 ct["base_currency_code"], MIG_USER, contract_id),
            )
            stats["contract_updated"] += 1
        else:
            contract_id = new_id()
            cur.execute(
                """INSERT INTO contract
                       (id,employee_id,contract_number,contract_type,effective_from,
                        effective_to,base_currency_code,status,created_by)
                   VALUES (%s,%s,%s,%s,%s,%s,%s,'ACTIVE',%s)""",
                (contract_id, emp_id, ct["contract_number"], ct["contract_type"],
                 ct["effective_from"], ct["effective_to"], ct["base_currency_code"], MIG_USER),
            )
            stats["contract_inserted"] += 1

        # --- documents ---
        for doc in emp["documents"]:
            cur.execute(
                """SELECT id FROM employee_document
                    WHERE employee_id=%s AND document_type=%s AND document_number=%s""",
                (emp_id, doc["document_type"], doc["document_number"]),
            )
            if cur.fetchone():
                stats["document_skipped"] += 1
            else:
                cur.execute(
                    """INSERT INTO employee_document
                           (id,employee_id,document_type,document_number,
                            issuing_country_code,status,created_by)
                       VALUES (%s,%s,%s,%s,%s,'ACTIVE',%s)""",
                    (new_id(), emp_id, doc["document_type"], doc["document_number"],
                     doc["issuing_country_code"], MIG_USER),
                )
                stats["document_inserted"] += 1

        # --- pay items ---
        for pi in emp["pay_items"]:
            comp_id = comp_ids.get(pi["component_code"])
            cur.execute(
                """SELECT id FROM contract_pay_item
                    WHERE contract_id=%s AND pay_component_id=%s AND effective_from=%s""",
                (contract_id, comp_id, pi["effective_from"]),
            )
            row = cur.fetchone()
            if row:
                cur.execute(
                    """UPDATE contract_pay_item SET amount=%s,currency_code=%s,
                              effective_to=%s,remarks=%s,updated_at=now(),updated_by=%s
                        WHERE id=%s""",
                    (pi["amount"], pi["currency_code"], pi["effective_to"],
                     pi["remarks"], MIG_USER, row[0]),
                )
                stats["pay_item_updated"] += 1
            else:
                cur.execute(
                    """INSERT INTO contract_pay_item
                           (id,contract_id,employee_id,pay_component_id,amount,
                            currency_code,effective_from,effective_to,status,remarks,created_by)
                       VALUES (%s,%s,%s,%s,%s,%s,%s,%s,'ACTIVE',%s,%s)""",
                    (new_id(), contract_id, emp_id, comp_id, pi["amount"],
                     pi["currency_code"], pi["effective_from"], pi["effective_to"],
                     pi["remarks"], MIG_USER),
                )
                stats["pay_item_inserted"] += 1

        # --- dependents ---
        for dep in emp["dependents"]:
            cur.execute(
                "SELECT id FROM employee_dependent WHERE employee_id=%s AND full_name=%s",
                (emp_id, dep["full_name"]),
            )
            if cur.fetchone():
                stats["dependent_skipped"] += 1
            else:
                cur.execute(
                    """INSERT INTO employee_dependent
                           (id,employee_id,full_name,relationship,date_of_birth,status,created_by)
                       VALUES (%s,%s,%s,%s,%s,'ACTIVE',%s)""",
                    (new_id(), emp_id, dep["full_name"], dep["relationship"],
                     dep["date_of_birth"], MIG_USER),
                )
                stats["dependent_inserted"] += 1

    conn.commit()
    cur.close()
    return stats


# ---------------------------------------------------------------------------
# DB connection
# ---------------------------------------------------------------------------
def connect():
    import psycopg2  # lazy import so --dry-run needs no driver

    url = os.environ.get("SPRING_DATASOURCE_URL")
    host, port, dbname = "localhost", "5432", "hrms"
    if url:
        m = re.search(r"//([^:/]+)(?::(\d+))?/([^?]+)", url)
        if m:
            host = m.group(1)
            port = m.group(2) or "5432"
            dbname = m.group(3)
    host = os.environ.get("PGHOST", host)
    port = os.environ.get("PGPORT", port)
    dbname = os.environ.get("PGDATABASE", dbname)
    user = os.environ.get("SPRING_DATASOURCE_USERNAME") or os.environ.get("PGUSER", "hrms")
    pwd = os.environ.get("SPRING_DATASOURCE_PASSWORD") or os.environ.get("PGPASSWORD", "hrms")
    print(f"  Connecting to postgresql://{user}@{host}:{port}/{dbname}")
    return psycopg2.connect(host=host, port=port, dbname=dbname, user=user, password=pwd)


# ---------------------------------------------------------------------------
# Reporting
# ---------------------------------------------------------------------------
def print_preview(plan):
    print("\n=== MIGRATION PLAN (dry-run) ===")
    print(f"Company tenant : {plan['company_id']}")
    print(f"Components      : {len(plan['components'])}")
    for c in plan["components"].values():
        print(f"   - {c['code']:10} {c['name']:25} {c['category']:10} "
              f"{c['calculation_method']:10} {c['payment_frequency']:8} {c['currency_code']}")
    print(f"Employees      : {len(plan['employees'])}")
    nat = Counter(e["nationality_country_code"] for e in plan["employees"])
    print(f"   nationalities (ISO2): {dict(nat)}")
    pi = sum(len(e["pay_items"]) for e in plan["employees"])
    doc = sum(len(e["documents"]) for e in plan["employees"])
    dep = sum(len(e["dependents"]) for e in plan["employees"])
    print(f"   pay items={pi}  documents={doc}  dependents={dep}")
    print("\n   First 5 employees:")
    for e in plan["employees"][:5]:
        amt = ", ".join(f"{p['amount']} {p['currency_code']}" for p in e["pay_items"])
        print(f"   - {e['employee_number']:8} {e['first_name']:12} {e['last_name']:18} "
              f"nat={e['nationality_country_code']} hire={e['hire_date']} pay=[{amt}] "
              f"docs={len(e['documents'])}")
    if plan["warnings"]:
        print(f"\n   WARNINGS ({len(plan['warnings'])}):")
        for w in plan["warnings"][:30]:
            print(f"     ! {w}")


def main():
    ap = argparse.ArgumentParser(description="Legacy DBF -> HRMS Postgres migration")
    ap.add_argument("--src", required=True, help="folder containing the .dbf snapshot")
    ap.add_argument("--dry-run", action="store_true", help="transform & preview only, no DB writes")
    ap.add_argument("--company-id", default=os.environ.get("HRMS_COMPANY_ID", DEFAULT_COMPANY_ID))
    args = ap.parse_args()

    print(f"Reading DBF snapshot from: {args.src}")
    header, detail, deps = read_source(args.src)
    print(f"  payresulth={len(header)}  payresultd={len(detail)}  dependants={len(deps)}")

    plan = transform(header, detail, deps, args.company_id)
    print_preview(plan)

    if args.dry_run:
        print("\nDry-run complete. No data was written.")
        return

    conn = connect()
    try:
        stats = load(plan, conn)
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()
    print("\n=== LOAD COMPLETE ===")
    for k in sorted(stats):
        print(f"   {k:22} {stats[k]}")


if __name__ == "__main__":
    main()
