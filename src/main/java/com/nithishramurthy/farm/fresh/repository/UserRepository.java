package com.nithishramurthy.farm.fresh.repository;

import com.nithishramurthy.farm.fresh.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Database access for users, looked up by email. */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
