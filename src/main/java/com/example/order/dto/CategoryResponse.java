package com.example.order.dto;

import com.example.order.domain.Category;
import lombok.Getter;

import java.util.List;

@Getter
public class CategoryResponse {

    private final Long categoryId;
    private final String name;
    private final List<CategoryResponse> children;

    public CategoryResponse(Category category) {
        this.categoryId = category.getId();
        this.name = category.getName();
        this.children = category.getChildren().stream()
                .map(CategoryResponse::new)
                .toList();
    }
}
