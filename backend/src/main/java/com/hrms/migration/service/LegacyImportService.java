package com.hrms.migration.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.employee.domain.Contract;
import com.hrms.employee.domain.ContractPayItem;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.domain.EmployeeDependent;
import com.hrms.employee.domain.EmployeeDocument;
import com.hrms.employee.repository.ContractPayItemRepository;
import com.hrms.employee.repository.ContractRepository;
import com.hrms.employee.repository.EmployeeDependentRepository;
import com.hrms.employee.repository.EmployeeDocumentRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.migration.NationalityMap;
import com.hrms.migration.dto.ImportSummary;
import com.hrms.payroll.domain.PayrollComponent;
import com.hrms.payroll.repository.PayrollComponentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Imports a legacy FoxPro/DBF HR snapshot into the new HRMS schema, on the
 * server, behind an admin screen. This is the web equivalent of the one-off CLI
 * tool ({@code migration/legacy_migrate.py}) and shares its rules:
 *
 * <ul>
 *   <li><b>Current-state cutover</b> — only each employee's current state is
 *       imported; the action-sheet number is preserved on each pay item's
 *       {@code remarks} for traceability.</li>
 *   <li><b>Idempotent</b> — re-importing a fresh snapshot updates existing rows
 *       instead of duplicating (natural keys below), so it is safe to run after
 *       every legacy export.</li>
 * </ul>
 *
 * Natural keys: employee = (company, employee_number = BADGE_CD);
 * contract = MIG-&lt;BADGE_CD&gt;; component = (company, code = LEG&lt;CODE&gt;);
 * pay item = (contract, component, effective_from);
 * document = (employee, type, number); dependent = (employee, full_name).
 */
@Service
public class LegacyImportService {

    private static final LocalDate EPOCH = LocalDate.of(2000, 1, 1);

    private final PayrollComponentRepository componentRepo;
    private final EmployeeRepository employeeRepo;
    private final ContractRepository contractRepo;
    private final ContractPayItemRepository payItemRepo;
    private final EmployeeDocumentRepository documentRepo;
    private final EmployeeDependentRepository dependentRepo;

    public LegacyImportService(PayrollComponentRepository componentRepo,
                               EmployeeRepository employeeRepo,
                               ContractRepository contractRepo,
                               ContractPayItemRepository payItemRepo,
                               EmployeeDocumentRepository documentRepo,
                               EmployeeDependentRepository dependentRepo) {
        this.componentRepo = componentRepo;
        this.employeeRepo = employeeRepo;
        this.contractRepo = contractRepo;
        this.payItemRepo = payItemRepo;
        this.documentRepo = documentRepo;
        this.dependentRepo = dependentRepo;
    }

    /**
     * @param srcDir directory holding the uploaded .dbf snapshot
     * @param commit false = preview only (no writes); true = persist changes
     */
    @Transactional
    public ImportSummary run(File srcDir, boolean commit) {
        UUID companyId = TenantContext.requireCompanyId();

        File headerFile = require(srcDir, "payresulth.dbf");
        File detailFile = require(srcDir, "payresultd.dbf");
        File depFile = find(srcDir, "dependants.dbf");

        List<Map<String, Object>> header = DbfReader.read(headerFile);
        List<Map<String, Object>> detail = DbfReader.read(detailFile);
        List<Map<String, Object>> deps = depFile != null ? DbfReader.read(depFile) : List.of();

        ImportSummary sum = new ImportSummary();
        sum.setCommitted(commit);
        sum.setSourceHeaderRows(header.size());
        sum.setSourceDetailRows(detail.size());
        sum.setSourceDependentRows(deps.size());

        // group detail / dependents by badge
        Map<String, List<Map<String, Object>>> detailByBadge = groupByBadge(detail);
        Map<String, List<Map<String, Object>>> depsByBadge = groupByBadge(deps);

        // 1) components (one master per distinct legacy CODE)
        Map<String, UUID> compIds = new LinkedHashMap<>();
        Map<String, Map<String, Object>> components = new LinkedHashMap<>();
        for (Map<String, Object> d : detail) {
            String code = s(d.get("CODE"));
            String compCode = compCode(code);
            components.computeIfAbsent(compCode, k -> {
                Map<String, Object> c = new LinkedHashMap<>();
                c.put("code", compCode);
                c.put("name", compName(code, s(d.get("DESCR"))));
                c.put("category", "00".equals(trim(code)) ? "SALARY" : "ALLOWANCE");
                c.put("calc", "AM".equals(s(d.get("TYPE"))) ? "FIXED" : "PERCENTAGE");
                c.put("freq", frequency(s(d.get("FREQUENCY"))));
                c.put("currency", currency(d.get("CURRENCY")));
                return c;
            });
        }
        for (Map<String, Object> c : components.values()) {
            String code = (String) c.get("code");
            Optional<PayrollComponent> existing = componentRepo.findByCompanyIdAndCode(companyId, code);
            if (existing.isPresent()) {
                PayrollComponent pc = existing.get();
                compIds.put(code, pc.getId());
                if (commit) {
                    pc.setName((String) c.get("name"));
                    pc.setCategory((String) c.get("category"));
                    pc.setPaymentFrequency((String) c.get("freq"));
                    pc.setCalculationMethod((String) c.get("calc"));
                    pc.setCurrencyCode((String) c.get("currency"));
                    componentRepo.save(pc);
                }
                sum.bump("component_updated");
            } else {
                if (commit) {
                    PayrollComponent pc = new PayrollComponent();
                    pc.setCompanyId(companyId);
                    pc.setCode(code);
                    pc.setName((String) c.get("name"));
                    pc.setCategory((String) c.get("category"));
                    pc.setComponentType("EARNING");
                    pc.setPaymentFrequency((String) c.get("freq"));
                    pc.setCalculationMethod((String) c.get("calc"));
                    pc.setCurrencyCode((String) c.get("currency"));
                    pc.setEffectiveFrom(EPOCH);
                    compIds.put(code, componentRepo.save(pc).getId());
                }
                sum.bump("component_inserted");
            }
        }

        // 2) employees + children
        int sampled = 0;
        for (Map<String, Object> h : header) {
            String badge = firstNonNull(s(h.get("BADGE_CD")), s(h.get("BADGE_CHR")));
            if (badge == null) {
                sum.getWarnings().add("Skipped a header row with no BADGE_CD");
                continue;
            }
            String[] name = splitName(s(h.get("NAME")));
            String natLegacy = s(h.get("NATION"));
            String iso2 = natLegacy != null ? NationalityMap.toIso2(natLegacy) : null;
            if (natLegacy != null && iso2 == null) {
                sum.getWarnings().add("Badge " + badge + ": unmapped nationality '" + natLegacy + "' -> stored NULL");
            }
            LocalDate hire = firstNonNull(date(h.get("DTCONTRACT")), date(h.get("EFF_FROM")));
            if (hire == null) {
                sum.getWarnings().add("Badge " + badge + ": no DTCONTRACT/EFF_FROM -> hire date defaulted to " + EPOCH);
                hire = EPOCH;
            }
            List<Map<String, Object>> lines = detailByBadge.getOrDefault(badge, List.of());
            String baseCcy = firstNonNull(currency(h.get("CURRENCY")),
                    lines.isEmpty() ? null : currency(lines.get(0).get("CURRENCY")));

            // upsert employee
            Optional<Employee> existingEmp = employeeRepo.findByCompanyIdAndEmployeeNumber(companyId, badge);
            boolean empIsNew = existingEmp.isEmpty();
            UUID empId = existingEmp.map(Employee::getId).orElse(null);
            if (!empIsNew) {
                Employee e = existingEmp.get();
                if (commit) {
                    e.setFirstName(name[0]);
                    e.setLastName(name[1]);
                    e.setNationalityCountryCode(iso2);
                    e.setDateOfBirth(date(h.get("DTBIRTH")));
                    e.setGender(gender(s(h.get("SEX"))));
                    e.setHireDate(hire);
                    e.setTerminationDate(date(h.get("DTERMINATE")));
                    e.setStatus(truthy(h.get("ACTIVE")) ? "ACTIVE" : "TERMINATED");
                    employeeRepo.save(e);
                }
                sum.bump("employee_updated");
            } else {
                if (commit) {
                    Employee e = new Employee();
                    e.setCompanyId(companyId);
                    e.setEmployeeNumber(badge);
                    e.setFirstName(name[0]);
                    e.setLastName(name[1]);
                    e.setNationalityCountryCode(iso2);
                    e.setDateOfBirth(date(h.get("DTBIRTH")));
                    e.setGender(gender(s(h.get("SEX"))));
                    e.setHireDate(hire);
                    e.setTerminationDate(date(h.get("DTERMINATE")));
                    e.setStatus(truthy(h.get("ACTIVE")) ? "ACTIVE" : "TERMINATED");
                    empId = employeeRepo.save(e).getId();
                }
                sum.bump("employee_inserted");
            }

            // contract (one per employee, keyed by MIG-<badge>)
            String contractNumber = "MIG-" + badge;
            UUID contractId = null;
            Optional<Contract> existingCt = empId != null
                    ? contractRepo.findByEmployeeIdAndContractNumber(empId, contractNumber)
                    : Optional.empty();
            if (existingCt.isPresent()) {
                Contract ct = existingCt.get();
                contractId = ct.getId();
                if (commit) {
                    ct.setContractType("PERMANENT");
                    ct.setEffectiveFrom(hire);
                    ct.setEffectiveTo(date(h.get("CONTR_END")));
                    ct.setBaseCurrencyCode(baseCcy);
                    contractRepo.save(ct);
                }
                sum.bump("contract_updated");
            } else {
                if (commit && empId != null) {
                    Contract ct = new Contract();
                    ct.setEmployeeId(empId);
                    ct.setContractNumber(contractNumber);
                    ct.setContractType("PERMANENT");
                    ct.setEffectiveFrom(hire);
                    ct.setEffectiveTo(date(h.get("CONTR_END")));
                    ct.setBaseCurrencyCode(baseCcy);
                    contractId = contractRepo.save(ct).getId();
                }
                sum.bump("contract_inserted");
            }

            // document: PERSONALNO -> PERSONAL_NO
            String personalNo = s(h.get("PERSONALNO"));
            if (personalNo != null) {
                String docNum = trunc(personalNo, 100);
                Optional<EmployeeDocument> existingDoc = empId != null
                        ? documentRepo.findByEmployeeIdAndDocumentTypeAndDocumentNumber(empId, "PERSONAL_NO", docNum)
                        : Optional.empty();
                if (existingDoc.isPresent()) {
                    sum.bump("document_skipped");
                } else {
                    if (commit && empId != null) {
                        EmployeeDocument doc = new EmployeeDocument();
                        doc.setEmployeeId(empId);
                        doc.setDocumentType("PERSONAL_NO");
                        doc.setDocumentNumber(docNum);
                        doc.setIssuingCountryCode(iso2);
                        documentRepo.save(doc);
                    }
                    sum.bump("document_inserted");
                }
            }

            // pay items
            String payPreview = null;
            for (Map<String, Object> d : lines) {
                String compCode = compCode(s(d.get("CODE")));
                BigDecimal amount = decimal(d.get("AMOUNT"));
                if (amount == null) {
                    sum.getWarnings().add("Badge " + badge + ": pay line " + compCode + " has NULL amount -> skipped");
                    continue;
                }
                String ccy = currency(d.get("CURRENCY"));
                if (payPreview == null) {
                    payPreview = amount.stripTrailingZeros().toPlainString() + (ccy != null ? " " + ccy : "");
                }
                LocalDate effFrom = firstNonNull(date(d.get("EFF_FROM")), hire);
                UUID compId = compIds.get(compCode);
                Optional<ContractPayItem> existingPi = (contractId != null && compId != null)
                        ? payItemRepo.findByContractIdAndPayComponentIdAndEffectiveFrom(contractId, compId, effFrom)
                        : Optional.empty();
                if (existingPi.isPresent()) {
                    ContractPayItem pi = existingPi.get();
                    if (commit) {
                        pi.setAmount(amount);
                        pi.setCurrencyCode(ccy);
                        pi.setEffectiveTo(date(d.get("EFF_TO")));
                        pi.setRemarks(actionSheet(d.get("ACTSHT_NO")));
                        payItemRepo.save(pi);
                    }
                    sum.bump("pay_item_updated");
                } else {
                    if (commit && contractId != null && compId != null) {
                        ContractPayItem pi = new ContractPayItem();
                        pi.setContractId(contractId);
                        pi.setEmployeeId(empId);
                        pi.setPayComponentId(compId);
                        pi.setAmount(amount);
                        pi.setCurrencyCode(ccy);
                        pi.setEffectiveFrom(effFrom);
                        pi.setEffectiveTo(date(d.get("EFF_TO")));
                        pi.setRemarks(actionSheet(d.get("ACTSHT_NO")));
                        payItemRepo.save(pi);
                    }
                    sum.bump("pay_item_inserted");
                }
            }

            // dependents
            for (Map<String, Object> dp : depsByBadge.getOrDefault(badge, List.of())) {
                String full = s(dp.get("NAME"));
                if (full == null) {
                    continue;
                }
                full = trunc(full, 150);
                Optional<EmployeeDependent> existingDep = empId != null
                        ? dependentRepo.findByEmployeeIdAndFullName(empId, full)
                        : Optional.empty();
                if (existingDep.isPresent()) {
                    sum.bump("dependent_skipped");
                } else {
                    if (commit && empId != null) {
                        EmployeeDependent dep = new EmployeeDependent();
                        dep.setEmployeeId(empId);
                        dep.setFullName(full);
                        dep.setRelationship(s(dp.get("RELATION")));
                        dep.setDateOfBirth(firstNonNull(date(dp.get("DOB")), date(dp.get("DTBIRTH"))));
                        dependentRepo.save(dep);
                    }
                    sum.bump("dependent_inserted");
                }
            }

            if (sampled < 5) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("employeeNumber", badge);
                row.put("name", name[0] + " " + name[1]);
                row.put("nationality", iso2);
                row.put("hireDate", hire.toString());
                row.put("pay", payPreview);
                row.put("action", empIsNew ? "NEW" : "UPDATE");
                sum.getSample().add(row);
                sampled++;
            }
        }

        return sum;
    }

    // ---- helpers -----------------------------------------------------------

    private static Map<String, List<Map<String, Object>>> groupByBadge(List<Map<String, Object>> rows) {
        Map<String, List<Map<String, Object>>> by = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            String badge = firstNonNull(s(r.get("BADGE_CD")), s(r.get("BADGE_CHR")));
            if (badge != null) {
                by.computeIfAbsent(badge, k -> new ArrayList<>()).add(r);
            }
        }
        return by;
    }

    private static File require(File dir, String name) {
        File f = find(dir, name);
        if (f == null) {
            throw new BusinessRuleException("import.file.missing",
                    "Required file not found in the upload: " + name);
        }
        return f;
    }

    private static File find(File dir, String name) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().equalsIgnoreCase(name)) {
                    return f;
                }
            }
        }
        return null;
    }

    private static String compCode(String code) {
        String c = trim(code);
        return (c == null || c.isEmpty()) ? "LEG_UNK" : trunc("LEG" + c, 50);
    }

    private static String compName(String code, String descr) {
        String d = descr != null ? descr.trim() : "";
        if (d.isEmpty()) {
            d = "Component " + trim(code);
        }
        return trunc(d, 150);
    }

    private static String frequency(String v) {
        if (v == null) {
            return "MONTHLY";
        }
        String u = v.toUpperCase();
        if (u.startsWith("DAIL")) return "DAILY";
        if (u.startsWith("WEEK")) return "WEEKLY";
        if (u.startsWith("MONTH")) return "MONTHLY";
        if (u.startsWith("ANNUAL") || u.startsWith("YEAR")) return "ANNUAL";
        return "MONTHLY";
    }

    private static String gender(String v) {
        if (v == null) {
            return null;
        }
        switch (v.toUpperCase()) {
            case "M": return "MALE";
            case "F": return "FEMALE";
            default: return null;
        }
    }

    private static String[] splitName(String full) {
        String f = full == null ? "" : full.replaceAll("\\s+", " ").trim();
        if (f.isEmpty()) {
            return new String[] {"Unknown", "Unknown"};
        }
        int sp = f.indexOf(' ');
        String first = sp < 0 ? f : f.substring(0, sp);
        String last = sp < 0 ? first : f.substring(sp + 1);
        return new String[] {trunc(first, 100), trunc(last, 100)};
    }

    private static String actionSheet(Object v) {
        String a = s(v);
        return a != null ? trunc("Action Sheet " + a, 255) : null;
    }

    private static String currency(Object v) {
        String c = s(v);
        return c != null ? trunc(c, 3) : null;
    }

    /** Trimmed string; numbers rendered without trailing zeros; "" -> null. */
    private static String s(Object o) {
        if (o == null) {
            return null;
        }
        String t = (o instanceof Number)
                ? new BigDecimal(o.toString()).stripTrailingZeros().toPlainString()
                : o.toString().trim();
        return t.isEmpty() ? null : t;
    }

    private static String trim(String v) {
        return v == null ? null : v.trim();
    }

    private static String trunc(String v, int max) {
        if (v == null) {
            return null;
        }
        return v.length() > max ? v.substring(0, max) : v;
    }

    private static BigDecimal decimal(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof BigDecimal bd) {
            return bd;
        }
        if (o instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        try {
            return new BigDecimal(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate date(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof LocalDate ld) {
            return ld;
        }
        if (o instanceof Date d) {
            return d.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        }
        return null;
    }

    private static boolean truthy(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Boolean b) {
            return b;
        }
        if (o instanceof Number n) {
            return n.doubleValue() != 0;
        }
        String v = o.toString().trim();
        return v.equalsIgnoreCase("T") || v.equalsIgnoreCase("Y")
                || v.equalsIgnoreCase("true") || v.equals("1");
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... vals) {
        for (T v : vals) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }
}
