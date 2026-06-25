package com.hrms.security;

import java.security.Principal;
import java.util.UUID;

/**
 * Lightweight principal placed in the security context for token-authenticated
 * requests (no DB round-trip per request). Exposes the user id and tenant.
 *
 * <p>Implements {@link Principal} so {@code Authentication.getName()} resolves to
 * the username (e.g. "manager") rather than the record's verbose {@code toString()}
 * - the latter overflowed the 100-char audit columns (created_by/updated_by).
 */
public record AuthenticatedUser(UUID userId, String username, UUID companyId) implements Principal {

    @Override
    public String getName() {
        return username;
    }
}
