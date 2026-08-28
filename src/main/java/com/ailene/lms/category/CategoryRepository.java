package com.ailene.lms.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Short> {

    List<Category> findAllByOrderByNameAsc();
}
