package com.nithishramurthy.farm.fresh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nithishramurthy.farm.fresh.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {}
