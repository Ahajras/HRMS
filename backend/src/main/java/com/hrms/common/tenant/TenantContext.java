package com.hrms.common.tenant;

import com.hrms.common.exception.BusinessRuleException;

import java.util.Optional;
import java.util.UUID;

/**
 * Holds the current request's company (tenant) scope.
 *
 * <p>Set by {@link TenantFilter} from the request and cleared at the end of the request.
 * Module services read this to enforce company scoping. In Phase 2 this same context will
 * drive database row-level security (FTDD Vol.2 Ch.31).
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_COMPANY = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setCompanyId(UUID companyId) {
        CURRENT_COMPANY.set(companyId);
    }

    public static Optional<UUID> getCompanyId() {
        return Optional.ofNullable(CURRENT_COMPANY.get());
    }

    public static UUID requireCompanyId() {
        UUID id = CURRENT_COMPANY.get();
        if (id == null) {
            throw new BusinessRuleException("tenant.required",
                    "No company selected. Platform admins must set a Company ID (X-Company-Id) "
                            + "for company-scoped data.");
        }
        return id;
    }

    public static void clear() {
        CURRENT_COMPANY.remove();
    }
}
