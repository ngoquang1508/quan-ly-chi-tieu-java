package com.expensemanager.controller;

import com.expensemanager.model.Category;
import com.expensemanager.model.CategoryType;
import com.expensemanager.service.CategoryService;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;

public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    public void menu(Scanner sc, int userId) {
        while (true) {
            System.out.println("\n=== DANH MỤC ===");
            System.out.println("1. Thêm danh mục");
            System.out.println("2. Danh sách");
            System.out.println("3. Sửa danh mục");
            System.out.println("4. Xóa danh mục");
            System.out.println("0. Quay lại");
            System.out.print("Chọn: ");
            String c = sc.nextLine();
            switch (c) {
                case "1" -> create(sc, userId);
                case "2" -> list(userId);
                case "3" -> update(sc, userId);
                case "4" -> delete(sc, userId);
                case "0" -> {
                    return;
                }
                default -> System.out.println("Sai lựa chọn");
            }
        }
    }

    private void create(Scanner sc, int userId) {
        System.out.print("Tên danh mục: ");
        String name = sc.nextLine();
        CategoryType type = askType(sc);
        System.out.print("Icon (tùy chọn): ");
        String icon = sc.nextLine();
        Category category = categoryService.create(userId, name, type, icon);
        System.out.println("Đã thêm danh mục #" + category.getId());
    }

    private void list(int userId) {
        List<Category> list = categoryService.list(userId);
        if (list.isEmpty()) {
            System.out.println("Chưa có danh mục.");
            return;
        }
        list.forEach(c ->
                System.out.printf("ID:%d | %s | %s%n", c.getId(), c.getName(), c.getType()));
    }

    private void update(Scanner sc, int userId) {
        System.out.print("ID danh mục: ");
        int id = Integer.parseInt(sc.nextLine());
        Optional<Category> category = categoryService.get(id, userId);
        if (category.isEmpty()) {
            System.out.println("Không tìm thấy.");
            return;
        }
        System.out.print("Tên mới: ");
        String name = sc.nextLine();
        CategoryType type = askType(sc);
        System.out.print("Icon mới (có thể bỏ trống): ");
        String icon = sc.nextLine();
        boolean ok = categoryService.update(userId, id, name, type, icon);
        System.out.println(ok ? "Đã cập nhật." : "Cập nhật thất bại.");
    }

    private void delete(Scanner sc, int userId) {
        System.out.print("ID danh mục cần xóa: ");
        int id = Integer.parseInt(sc.nextLine());
        boolean ok = categoryService.delete(userId, id);
        System.out.println(ok ? "Đã xóa." : "Không tìm thấy.");
    }

    private CategoryType askType(Scanner sc) {
        while (true) {
            System.out.print("Loại (EXPENSE/INCOME): ");
            String type = sc.nextLine().trim().toUpperCase(Locale.ROOT);
            try {
                return CategoryType.valueOf(type);
            } catch (IllegalArgumentException e) {
                System.out.println("Sai loại, nhập lại.");
            }
        }
    }
}
