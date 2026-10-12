package com.pooja.farm.fresh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pooja.farm.fresh.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {}
