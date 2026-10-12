package com.nithishramurthy.farm.fresh.details;

import com.nithishramurthy.farm.fresh.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections; // For Collections.singletonList()

/** Adapts our User entity to Spring Security's UserDetails, using the email as the username. */
public class CustomUserDetails implements UserDetails {

    private User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // This is where user.getAuthorities() would be relevant if User entity had roles
        // For now, assuming a single role string like "ROLE_USER"
        return Collections.singletonList(new SimpleGrantedAuthority(user.getRole()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail(); // Use email as the username for Spring Security
    }

    // Below are additional methods from UserDetails interface.
    // Implement logic if you need to manage account non-expiration, locking, etc.
    @Override
    public boolean isAccountNonExpired() {
        return true; // You can add logic for account expiration if needed
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // You can add logic for account locking if needed
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // You can add logic for credential expiration if needed
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled(); // Use the 'enabled' field from your User entity
    }

    // You might want to provide access to the underlying User object
    public User getUser() {
        return user;
    }
}