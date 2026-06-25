package com.hrms.security;

import java.util.UUID;

/**
 * Lightweight principal placed in the security context for token-authenticated
 * requests (no DB round-trip per request). Exposes the user id and tenant.
 */
public record AuthenticatedUser(UUID userId, String username, UUID companyId) {
}
