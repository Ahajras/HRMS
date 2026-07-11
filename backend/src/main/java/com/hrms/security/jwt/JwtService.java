package com.hrms.security.jwt;

import com.hrms.security.AppUserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Issues and verifies HS256 JWT access tokens.
 *
 * <p>Claims carry the user id ({@code uid}), tenant/company ({@code cid}, nullable
 * for platform admins) and the flat list of granted authorities, so each request
 * can be authorised without a database round-trip (FTDD Vol.2 Ch.31).
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;
    private final String issuer;

    public JwtService(
            @Value("${hrms.security.jwt.secret}") String secret,
            @Value("${hrms.security.jwt.expiration-minutes:480}") long expirationMinutes,
            @Value("${hrms.security.jwt.issuer:hrms}") String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
        this.issuer = issuer;
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    public String generateToken(AppUserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        List<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return Jwts.builder()
                .subject(principal.getUsername())
                .issuer(issuer)
                .claim("uid", principal.getUserId().toString())
                .claim("cid", principal.getCompanyId() == null ? null : principal.getCompanyId().toString())
                .claim("eid", principal.getEmployeeId() == null ? null : principal.getEmployeeId().toString())
                .claim("authorities", authorities)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
