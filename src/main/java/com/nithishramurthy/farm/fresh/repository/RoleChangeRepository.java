package com.nithishramurthy.farm.fresh.repository;

import com.nithishramurthy.farm.fresh.entity.RoleChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Role change audit log. */
public interface RoleChangeRepository extends JpaRepository<RoleChange, Long> {
    List<RoleChange> findTop50ByOrderByChangedAtDescIdDesc();
}
