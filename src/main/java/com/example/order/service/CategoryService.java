package com.example.order.service;

import com.example.order.domain.Category;
import com.example.order.dto.CategoryResponse;
import com.example.order.dto.CreateCategoryRequest;
import com.example.order.exception.EntityNotFoundException;
import com.example.order.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Long createCategory(CreateCategoryRequest request) {
        Category category = Category.createCategory(request.getName());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent category not found. id=" + request.getParentId()));
            category.setParent(parent);
        }

        categoryRepository.save(category);
        return category.getId();
    }

    /**
     * Find all root categories with children (hierarchical)
     */
    public List<CategoryResponse> findAllCategories() {
        return categoryRepository.findRootsWithChildren().stream()
                .map(CategoryResponse::new)
                .toList();
    }

    public CategoryResponse findCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found. id=" + categoryId));
        return new CategoryResponse(category);
    }
}
