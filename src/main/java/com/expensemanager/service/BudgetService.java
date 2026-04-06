package com.expensemanager.service;

import com.expensemanager.model.Budget;
import com.expensemanager.repository.BudgetRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class BudgetService {

    private final BudgetRepository budgetRepository;

    public BudgetService(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    public Budget setBudget(int userId, Integer categoryId, BigDecimal amount, int month, int year) {
        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setCategoryId(categoryId);
        budget.setAmountLimit(amount);
        budget.setMonth(month);
        budget.setYear(year);
        return budgetRepository.saveOrUpdate(budget);
    }

    public List<Budget> list(int userId) {
        return budgetRepository.findByUser(userId);
    }

    public Optional<Budget> find(int userId, Integer categoryId, int month, int year) {
        return budgetRepository.findByUserAndCategoryAndMonth(userId, categoryId, month, year);
    }
}
