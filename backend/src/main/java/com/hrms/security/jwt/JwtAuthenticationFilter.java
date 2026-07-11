package com.hrms.security.jwt;

import com.hrms.common.tenant.TenantContext;
import com.hrms.security.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Validates the {@code Authorization: Bearer <jwt>} header on each request.
 *
 * <p>On a valid token it populates the {@link SecurityContextHolder} with the
 * authorities carried in the token and sets the {@link TenantContext} from the
 * token's company claim - so the tenant is derived from the authenticated
 * principal, not from a client-supplied header (FTDD Vol.2 Ch.31).
 *
 * <p>Instantiated and inserted into the chain by {@code SecurityConfig} (not a
 * component) so it is not also auto-registered as a plain servlet filter.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(PREFIX.length()).trim();
            try {
                Claims claims = jwtService.parse(token);
                authenticate(claims, request);
            } catch (JwtException | IllegalArgumentException ex) {
                // Invalid/expired token -> leave context unauthenticated; entry point returns 401.
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(Claims claims, HttpServletRequest request) {
        UUID userId = UUID.fromString(claims.get("uid", String.class));
        String cid = claims.get("cid", String.class);
        UUID companyId = StringUtils.hasText(cid) ? UUID.fromString(cid) : null;
        String eid = claims.get("eid", String.class);
        UUID employeeId = StringUtils.hasText(eid) ? UUID.fromString(eid) : null;

        @SuppressWarnings("unchecked")
        List<String> authorities = claims.get("authorities", List.class);
        List<SimpleGrantedAuthority> grantedAuthorities = authorities == null ? List.of()
                : authorities.stream().map(SimpleGrantedAuthority::new).toList();

        AuthenticatedUser principal = new AuthenticatedUser(userId, claims.getSubject(), companyId, employeeId);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, grantedAuthorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (companyId != null) {
            TenantContext.setCompanyId(companyId);
        }
        if (employeeId != null) {
            com.hrms.common.tenant.EmployeeContext.setEmployeeId(employeeId);
        }
    }
}
