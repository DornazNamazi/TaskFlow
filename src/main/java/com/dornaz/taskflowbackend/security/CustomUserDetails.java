package com.dornaz.taskflowbackend.security;

import com.dornaz.taskflowbackend.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    // === Custom getters (useful inside controllers/services) ===
    public Long getId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getRole() {
        return user.getRole().name();
    }

    public String getDisplayName() {
        return user.getUsername();
    }

    /**
     * IMPORTANT:
     * Spring Security treats getUsername() as the "login identifier".
     * Your CustomUserDetailsService loads users BY EMAIL,
     * so getUsername() MUST return email.
     */
    @Override
    public String getUsername() {
        return user.getEmail(); // ✅ login identity = email
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
