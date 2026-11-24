package com.nithishramurthy.farm.fresh.securityconfig;

import com.nithishramurthy.farm.fresh.details.CustomUserDetails;
import com.nithishramurthy.farm.fresh.entity.User;
import com.nithishramurthy.farm.fresh.repository.UserRepository; // Import your UserRepository

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Good practice for database operations

/** Loads a user by email for Spring Security. */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Called by password login and by the JWT filter on every request. */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // The email is the username in this app.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Wrap our User so Spring Security can read its password hash, role and enabled flag.
        return new CustomUserDetails(user);
    }
}