package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.entity.Role;
import com.nithishramurthy.farm.fresh.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Makes the account in OWNER_EMAIL the shop owner, so the first owner doesn't
 * need SQL. Runs at startup for an existing account, and is asked for the role
 * of every new sign-up.
 */
@Component
public class OwnerAccountSetup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OwnerAccountSetup.class);

    private final UserRepository userRepository;
    private final String ownerEmail;

    public OwnerAccountSetup(UserRepository userRepository, @Value("${app.owner-email:}") String ownerEmail) {
        this.userRepository = userRepository;
        this.ownerEmail = ownerEmail == null ? "" : ownerEmail.trim();
    }

    /** Role a brand-new account starts with. */
    public Role roleForNewAccount(String email) {
        return !ownerEmail.isEmpty() && ownerEmail.equalsIgnoreCase(email) ? Role.OWNER : Role.USER;
    }

    /**
     * Runs once when the app starts. If the OWNER_EMAIL account already exists but isn't
     * an owner yet, it is promoted. If it doesn't exist, registerUser gives it OWNER on sign-up.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (ownerEmail.isEmpty()) {
            return;
        }
        userRepository.findByEmail(ownerEmail)
                .filter(user -> user.roleEnum() != Role.OWNER)
                .ifPresent(user -> {
                    user.setRole(Role.OWNER.authority());
                    userRepository.save(user);
                    log.info("Made {} the shop owner (OWNER_EMAIL)", ownerEmail);
                });
    }
}
