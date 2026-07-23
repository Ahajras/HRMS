package com.hrms.employee.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentExpiryNotificationService {
    private static final Logger log = LoggerFactory.getLogger(DocumentExpiryNotificationService.class);
    private static final String SETTING_CATEGORY = "EMPLOYEE_DOCUMENT_ALERT_SETTING";
    private static final String ALERT_DAYS_CODE = "ALERT_DAYS";
    private static final String ALERT_ROLES_CODE = "ALERT_ROLES";

    private final JdbcTemplate jdbc;
    private final JavaMailSender mailSender;
    private final Environment environment;

    public DocumentExpiryNotificationService(JdbcTemplate jdbc, JavaMailSender mailSender, Environment environment) {
        this.jdbc = jdbc;
        this.mailSender = mailSender;
        this.environment = environment;
    }

    @Scheduled(initialDelayString = "${hrms.notifications.document-expiry.initial-delay-ms:60000}",
            fixedDelayString = "${hrms.notifications.document-expiry.fixed-delay-ms:3600000}")
    public void sendDueExpiryAlerts() {
        String host = environment.getProperty("spring.mail.host");
        if (!StringUtils.hasText(host)) {
            return;
        }
        try {
            List<UUID> companyIds = jdbc.queryForList("""
                    select distinct e.company_id
                    from employee e
                    where e.company_id is not null
                    """, UUID.class);
            for (UUID companyId : companyIds) {
                sendForCompany(companyId);
            }
        } catch (Exception ex) {
            log.warn("Could not send document expiry notifications", ex);
        }
    }

    private void sendForCompany(UUID companyId) {
        List<Integer> alertDays = alertDays(companyId);
        Set<String> roleCodes = alertRoleCodes(companyId);
        if (alertDays.isEmpty() || roleCodes.isEmpty()) {
            return;
        }

        int maxDays = alertDays.stream().mapToInt(Integer::intValue).max().orElse(0);
        LocalDate today = LocalDate.now();
        List<DocumentExpiryRow> rows = jdbc.query("""
                select e.employee_number,
                       trim(coalesce(e.first_name, '') || ' ' || coalesce(e.last_name, '')) as employee_name,
                       d.document_type,
                       d.document_number,
                       d.expiry_date
                from employee_document d
                join employee e on e.id = d.employee_id
                where e.company_id = ?
                  and upper(coalesce(e.status, '')) = 'ACTIVE'
                  and upper(coalesce(d.status, '')) = 'ACTIVE'
                  and d.expiry_date is not null
                  and d.expiry_date between ? and ?
                order by d.expiry_date, e.employee_number, d.document_type
                """, (rs, rowNum) -> new DocumentExpiryRow(
                rs.getString("employee_number"),
                rs.getString("employee_name"),
                rs.getString("document_type"),
                rs.getString("document_number"),
                rs.getDate("expiry_date").toLocalDate()
        ), companyId, today, today.plusDays(maxDays));

        Set<Integer> alertDaySet = new LinkedHashSet<>(alertDays);
        List<DocumentExpiryRow> dueRows = rows.stream()
                .filter(row -> alertDaySet.contains((int) ChronoUnit.DAYS.between(today, row.expiryDate())))
                .toList();
        if (dueRows.isEmpty()) {
            return;
        }

        List<String> recipients = recipients(companyId, roleCodes);
        if (recipients.isEmpty()) {
            log.info("Document expiry alerts found for company {}, but no recipient email for roles {}", companyId, roleCodes);
            return;
        }

        String subject = "HRMS document expiry alert";
        String body = buildBody(today, dueRows);
        for (String recipient : recipients) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(recipient);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
            } catch (Exception ex) {
                log.warn("Could not send document expiry alert to {}", recipient, ex);
            }
        }
    }

    private List<Integer> alertDays(UUID companyId) {
        String value = setting(companyId, ALERT_DAYS_CODE, "60,30,7");
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::parseIntOrNull)
                .filter(v -> v != null && v >= 0)
                .distinct()
                .sorted((a, b) -> Integer.compare(b, a))
                .toList();
    }

    private Set<String> alertRoleCodes(UUID companyId) {
        String value = setting(companyId, ALERT_ROLES_CODE, "");
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String setting(UUID companyId, String code, String fallback) {
        List<String> values = jdbc.queryForList("""
                select label
                from lookup_value
                where category = ?
                  and code = ?
                  and upper(coalesce(status, 'ACTIVE')) = 'ACTIVE'
                  and (company_id = ? or company_id is null)
                order by case when company_id = ? then 0 else 1 end
                limit 1
                """, String.class, SETTING_CATEGORY, code, companyId, companyId);
        return values.isEmpty() || !StringUtils.hasText(values.get(0)) ? fallback : values.get(0);
    }

    private List<String> recipients(UUID companyId, Set<String> roleCodes) {
        String codes = String.join(",", roleCodes);
        return jdbc.queryForList("""
                select distinct u.email
                from app_user u
                join user_role ur on ur.user_id = u.id
                join role r on r.id = ur.role_id
                where (u.company_id = ? or u.company_id is null)
                  and upper(coalesce(u.status, '')) = 'ACTIVE'
                  and u.email is not null
                  and trim(u.email) <> ''
                  and upper(r.code) = any (string_to_array(?, ','))
                order by u.email
                """, String.class, companyId, codes);
    }

    private String buildBody(LocalDate today, List<DocumentExpiryRow> rows) {
        StringBuilder body = new StringBuilder();
        body.append("The following employee documents reached a configured expiry alert checkpoint.\n\n");
        body.append("As of: ").append(today).append("\n\n");
        for (DocumentExpiryRow row : rows) {
            long days = ChronoUnit.DAYS.between(today, row.expiryDate());
            body.append("- ")
                    .append(row.employeeNumber()).append(" - ").append(row.employeeName())
                    .append(" | ").append(row.documentType())
                    .append(" | ").append(row.documentNumber())
                    .append(" | expires ").append(row.expiryDate())
                    .append(" | ").append(days).append(" day(s) remaining")
                    .append("\n");
        }
        body.append("\nPlease open HRMS > Employees > Documents to review and renew.");
        return body.toString();
    }

    private Integer parseIntOrNull(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record DocumentExpiryRow(String employeeNumber, String employeeName, String documentType,
                                     String documentNumber, LocalDate expiryDate) {
    }
}
