package com.expensemanager.service;

import com.expensemanager.model.Category;
import com.expensemanager.model.CategoryType;
import com.expensemanager.repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category create(int userId, String name, CategoryType type, String icon) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName(name);
        category.setType(type);
        category.setIcon(icon);
        return categoryRepository.create(category);
    }

    public List<Category> list(int userId) {
        return categoryRepository.findAllByUser(userId);
    }

    public boolean update(int userId, int id, String name, CategoryType type, String icon) {
        Optional<Category> opt = categoryRepository.findById(id, userId);
        if (opt.isEmpty()) {
            return false;
        }
        Category category = opt.get();
        category.setName(name);
        category.setType(type);
        category.setIcon(icon);
        return categoryRepository.update(category);
    }

    public boolean delete(int userId, int id) {
        return categoryRepository.delete(id, userId);
    }

    public Optional<Category> get(int id, int userId) {
        return categoryRepository.findById(id, userId);
    }
}
