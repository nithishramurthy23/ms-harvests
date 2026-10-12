package com.nithishramurthy.farm.fresh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nithishramurthy.farm.fresh.entity.Category;

/** Database access for categories. */
public interface CategoryRepository extends JpaRepository<Category, Long> {}
