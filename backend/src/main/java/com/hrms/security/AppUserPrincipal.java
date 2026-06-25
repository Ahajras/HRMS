package com.hrms.security;

import com.hrms.security.domain.AppUser;
import com.hrms.security.domain.Permission;
import com.hrms.security.domain.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Spring Security {@link UserDetails} adapter over {@link AppUser}.
 *
 * <p>Authorities are both the role codes (prefixed {@code ROLE_}) and the flat
 * permission codes, so endpoints can guard with either {@code hasRole(..)} or
 * {@code hasAuthority('employee.write')}.
 */
public class AppUserPrincipal implements UserDetails {

    private final UUID userId;
    private final UUID companyId;
    private final String username;
    private final String passwordHash;
    private final boolean enabled;
    private final Set<GrantedAuthority> authorities;

    public AppUserPrincipal(AppUser user) {
        this.userId = user.getId();
        this.companyId = user.getCompanyId();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.enabled = "ACTIVE".equalsIgnoreCase(user.getStatus());
        this.authorities = new LinkedHashSet<>();
        for (Role role : user.getRoles()) {
            this.authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
            for (Permission p : role.getPermissions()) {
                this.authorities.add(new SimpleGrantedAuthority(p.getCode()));
            }
        }
    }

    public UUID getUserId() { return userId; }

    public UUID getCompanyId() { return companyId; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return enabled; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }
}
