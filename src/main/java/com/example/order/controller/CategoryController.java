package com.example.order.controller;

import com.example.order.dto.CategoryResponse;
import com.example.order.dto.CreateCategoryRequest;
import com.example.order.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Categories", description = "Category management API")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Create category", description = "Set parentId for sub-category, null for root")
    @PostMapping
    public ResponseEntity<Long> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        Long categoryId = categoryService.createCategory(request);
        return ResponseEntity.created(URI.create("/api/categories/" + categoryId)).body(categoryId);
    }

    @Operation(summary = "Get all categories", description = "Returns hierarchical tree structure")
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.findAllCategories());
    }

    @Operation(summary = "Get single category")
    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(categoryService.findCategory(categoryId));
    }
}
