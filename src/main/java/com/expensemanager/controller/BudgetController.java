package com.expensemanager.controller;

import com.expensemanager.model.Budget;
import com.expensemanager.model.Category;
import com.expensemanager.service.BudgetService;
import com.expensemanager.service.CategoryService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class BudgetController {

    private final BudgetService budgetService;
    private final CategoryService categoryService;

    public BudgetController(BudgetService budgetService, CategoryService categoryService) {
        this.budgetService = budgetService;
        this.categoryService = categoryService;
    }

    public void menu(Scanner sc, int userId) {
        while (true) {
            System.out.println("\n=== NGÂN SÁCH ===");
            System.out.println("1. Đặt ngân sách theo danh mục");
            System.out.println("2. Xem ngân sách");
            System.out.println("0. Quay lại");
            System.out.print("Chọn: ");
            String c = sc.nextLine();
            switch (c) {
                case "1" -> setBudget(sc, userId);
                case "2" -> list(userId);
                case "0" -> {
                    return;
                }
                default -> System.out.println("Sai lựa chọn");
            }
        }
    }

    private void setBudget(Scanner sc, int userId) {
        List<Category> categories = categoryService.list(userId);
        if (categories.isEmpty()) {
            System.out.println("Chưa có danh mục. Tạo trước.");
            return;
        }
        categories.forEach(c -> System.out.printf("  %d - %s (%s)%n", c.getId(), c.getName(), c.getType()));
        System.out.print("Chọn ID danh mục: ");
        int categoryId = Integer.parseInt(sc.nextLine());
        System.out.print("Tháng (1-12): ");
        int month = Integer.parseInt(sc.nextLine());
        System.out.print("Năm (yyyy): ");
        int year = Integer.parseInt(sc.nextLine());
        System.out.print("Giới hạn (amount): ");
        BigDecimal amount = new BigDecimal(sc.nextLine());
        Budget budget = budgetService.setBudget(userId, categoryId, amount, month, year);
        System.out.println("Đã lưu ngân sách #" + budget.getId());
    }

    private void list(int userId) {
        List<Budget> budgets = budgetService.list(userId);
        if (budgets.isEmpty()) {
            System.out.println("Chưa có ngân sách.");
            return;
        }
        budgets.forEach(b ->
                System.out.printf("ID:%d | DM:%s | %02d/%d | Giới hạn: %s%n",
                        b.getId(),
                        b.getCategoryId(),
                        b.getMonth(),
                        b.getYear(),
                        b.getAmountLimit()));
    }
}
