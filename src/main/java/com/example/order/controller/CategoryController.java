package com.example.order.controller;

import com.example.order.dto.CategoryResponse;
import com.example.order.dto.CreateCategoryRequest;
import com.example.order.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Create category
     */
    @PostMapping
    public ResponseEntity<Long> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        Long categoryId = categoryService.createCategory(request);
        return ResponseEntity.created(URI.create("/api/categories/" + categoryId)).body(categoryId);
    }

    /**
     * Get all categories (hierarchical tree)
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.findAllCategories());
    }

    /**
     * Get single category
     */
    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(categoryService.findCategory(categoryId));
    }
}
