package com.pooja.farm.fresh.repository;

import com.pooja.farm.fresh.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email); // or findByEmail if that's the correct field
    boolean existsByEmail(String email);
}
