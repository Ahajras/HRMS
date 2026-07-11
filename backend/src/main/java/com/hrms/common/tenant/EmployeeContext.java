package com.hrms.common.tenant;

import java.util.Optional;
import java.util.UUID;

/**
 * Holds the current request's employee scope — the employee record the
 * authenticated login account represents (e.g. a self-service account),
 * if any. Set by {@link com.hrms.security.jwt.JwtAuthenticationFilter}
 * from the token's employee claim, never from client input, so a
 * self-service endpoint can never be tricked into returning another
 * employee's data by passing a different id.
 *
 * <p>Admin/manager accounts (no personal employee record) leave this
 * empty — {@link #requireEmployeeId()} is only for endpoints that are
 * exclusively "my own data" self-service, not general admin screens.
 */
public final class EmployeeContext {

    private static final ThreadLocal<UUID> CURRENT_EMPLOYEE = new ThreadLocal<>();

    private EmployeeContext() {
    }

    public static void setEmployeeId(UUID employeeId) {
        CURRENT_EMPLOYEE.set(employeeId);
    }

    public static Optional<UUID> getEmployeeId() {
        return Optional.ofNullable(CURRENT_EMPLOYEE.get());
    }

    public static UUID requireEmployeeId() {
        UUID id = CURRENT_EMPLOYEE.get();
        if (id == null) {
            throw new com.hrms.common.exception.BusinessRuleException("self.no_employee_link",
                    "This login account is not linked to an employee record, so self-service data is not available.");
        }
        return id;
    }

    public static void clear() {
        CURRENT_EMPLOYEE.remove();
    }
}
