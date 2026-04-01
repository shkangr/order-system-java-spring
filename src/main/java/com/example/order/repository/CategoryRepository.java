package com.example.order.repository;

import com.example.order.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find root categories (no parent)
     */
    List<Category> findByParentIsNull();

    /**
     * Find root categories with children fetch join
     */
    @Query("select c from Category c left join fetch c.children where c.parent is null")
    List<Category> findRootsWithChildren();
}
