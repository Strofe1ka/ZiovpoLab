package ru.ziovpo.backend.security;

import java.util.Collection;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AppUserPrincipal implements UserDetails {

    private final UUID id;
    private final String name;
    private final String passwordHash;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;

    public AppUserPrincipal(UUID id,
                            String name,
                            String passwordHash,
                            Collection<? extends GrantedAuthority> authorities,
                            boolean accountNonExpired,
                            boolean accountNonLocked,
                            boolean credentialsNonExpired,
                            boolean enabled) {
        this.id = id;
        this.name = name;
        this.passwordHash = passwordHash;
        this.authorities = authorities;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.enabled = enabled;
    }

    public UUID getId() {
        return id;
    }

    public boolean isAdmin() {
        return authorities.stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return name;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
