package com.hrms.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Establishes the tenant (company) scope for each request.
 *
 * <p>Phase 2: the authenticated principal's company (carried in the JWT) is the
 * authoritative tenant and is set earlier by the JWT filter. This filter now only
 * acts as a fallback: it reads the company id from a header <em>when no tenant has
 * already been established</em> - useful for platform/super-admin accounts (whose
 * token carries no company) that explicitly target a company. It always clears the
 * context at the end of the request to prevent thread-local leakage.
 */
@Component
@Order(50)
public class TenantFilter extends OncePerRequestFilter {

    private final String tenantHeader;

    public TenantFilter(@Value("${hrms.tenant.header:X-Company-Id}") String tenantHeader) {
        this.tenantHeader = tenantHeader;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Only honour the header when the authenticated principal didn't already
            // establish a tenant (i.e. platform/super-admin acting on a chosen company).
            if (TenantContext.getCompanyId().isEmpty()) {
                String raw = request.getHeader(tenantHeader);
                if (StringUtils.hasText(raw)) {
                    try {
                        TenantContext.setCompanyId(UUID.fromString(raw.trim()));
                    } catch (IllegalArgumentException ignored) {
                        // Malformed header -> no tenant; scoped queries will require one explicitly.
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            com.hrms.common.tenant.EmployeeContext.clear();
        }
    }
}
