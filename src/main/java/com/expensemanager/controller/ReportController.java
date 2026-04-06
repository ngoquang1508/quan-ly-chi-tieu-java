package com.expensemanager.controller;

import com.expensemanager.service.ReportService;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Scanner;

public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    public void menu(Scanner sc, int userId) {
        while (true) {
            System.out.println("\n=== THỐNG KÊ ===");
            System.out.println("1. Tổng thu/chi theo tháng");
            System.out.println("2. Tổng thu/chi theo ngày");
            System.out.println("3. Danh mục chi nhiều nhất");
            System.out.println("4. Số dư tất cả ví");
            System.out.println("0. Quay lại");
            System.out.print("Chọn: ");
            String c = sc.nextLine();
            switch (c) {
                case "1" -> byMonth(sc, userId);
                case "2" -> byDate(sc, userId);
                case "3" -> topCategory(sc, userId);
                case "4" -> wallets(userId);
                case "0" -> {
                    return;
                }
                default -> System.out.println("Sai lựa chọn");
            }
        }
    }

    private void byMonth(Scanner sc, int userId) {
        int month = askInt(sc, "Tháng (1-12): ");
        int year = askInt(sc, "Năm (yyyy): ");
        var income = reportService.totalIncomeByMonth(userId, month, year);
        var expense = reportService.totalExpenseByMonth(userId, month, year);
        System.out.printf("Tháng %02d/%d -> Thu: %s | Chi: %s%n", month, year, income, expense);
    }

    private void byDate(Scanner sc, int userId) {
        LocalDate date = askDate(sc);
        var income = reportService.totalIncomeByDate(userId, date);
        var expense = reportService.totalExpenseByDate(userId, date);
        System.out.printf("Ngày %s -> Thu: %s | Chi: %s%n", date, income, expense);
    }

    private void topCategory(Scanner sc, int userId) {
        int month = askInt(sc, "Tháng: ");
        int year = askInt(sc, "Năm: ");
        Optional<String> top = reportService.topExpenseCategory(userId, month, year);
        System.out.println(top.map(s -> "Danh mục chi nhiều nhất: " + s)
                .orElse("Chưa có dữ liệu"));
    }

    private void wallets(int userId) {
        var total = reportService.totalWalletBalance(userId);
        System.out.println("Tổng số dư tất cả ví: " + total);
    }

    private int askInt(Scanner sc, String prompt) {
        System.out.print(prompt);
        return Integer.parseInt(sc.nextLine());
    }

    private LocalDate askDate(Scanner sc) {
        while (true) {
            System.out.print("Ngày (yyyy-MM-dd): ");
            String input = sc.nextLine();
            try {
                return LocalDate.parse(input);
            } catch (Exception e) {
                System.out.println("Sai định dạng, nhập lại.");
            }
        }
    }
}
