package com.expensemanager.controller;

import com.expensemanager.model.Transaction;
import com.expensemanager.model.TransactionType;
import com.expensemanager.model.Wallet;
import com.expensemanager.model.Category;
import com.expensemanager.service.TransactionService;
import com.expensemanager.service.WalletService;
import com.expensemanager.service.CategoryService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;

public class TransactionController {

    private final TransactionService transactionService;
    private final WalletService walletService;
    private final CategoryService categoryService;

    public TransactionController(TransactionService transactionService,
                                 WalletService walletService,
                                 CategoryService categoryService) {
        this.transactionService = transactionService;
        this.walletService = walletService;
        this.categoryService = categoryService;
    }

    public void menu(Scanner sc, int userId) {
        while (true) {
            System.out.println("\n=== GIAO DỊCH ===");
            System.out.println("1. Thêm giao dịch");
            System.out.println("2. Danh sách giao dịch");
            System.out.println("3. Sửa giao dịch");
            System.out.println("4. Xóa giao dịch");
            System.out.println("0. Quay lại");
            System.out.print("Chọn: ");
            String c = sc.nextLine();
            switch (c) {
                case "1" -> add(sc, userId);
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

    private void add(Scanner sc, int userId) {
        listWallets(userId);
        System.out.print("Chọn ID ví: ");
        int walletId = Integer.parseInt(sc.nextLine());
        listCategories(userId);
        System.out.print("Chọn ID danh mục: ");
        int categoryId = Integer.parseInt(sc.nextLine());
        TransactionType type = askType(sc);
        System.out.print("Số tiền: ");
        BigDecimal amount = new BigDecimal(sc.nextLine());
        System.out.print("Tiêu đề/ngắn gọn: ");
        String title = sc.nextLine();
        System.out.print("Ghi chú: ");
        String note = sc.nextLine();
        LocalDate date = askDate(sc);

        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setWalletId(walletId);
        tx.setCategoryId(categoryId);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setTitle(title);
        tx.setNote(note);
        tx.setTransactionDate(date);
        transactionService.add(tx);
        System.out.println("Đã thêm giao dịch và trigger đã cập nhật số dư ví.");
    }

    private void list(int userId) {
        List<Transaction> list = transactionService.list(userId);
        if (list.isEmpty()) {
            System.out.println("Chưa có giao dịch.");
            return;
        }
        list.forEach(t ->
                System.out.printf("ID:%d | %s | %s | %s | %s | %s | %s%n",
                        t.getId(),
                        t.getTransactionDate(),
                        t.getType(),
                        t.getAmount(),
                        "Ví:" + t.getWalletId(),
                        "DM:" + t.getCategoryId(),
                        t.getTitle()));
    }

    private void update(Scanner sc, int userId) {
        System.out.print("ID giao dịch: ");
        int id = Integer.parseInt(sc.nextLine());
        Optional<Transaction> opt = transactionService.get(id, userId);
        if (opt.isEmpty()) {
            System.out.println("Không tìm thấy.");
            return;
        }
        listWallets(userId);
        System.out.print("Ví mới: ");
        int walletId = Integer.parseInt(sc.nextLine());
        listCategories(userId);
        System.out.print("Danh mục mới: ");
        int categoryId = Integer.parseInt(sc.nextLine());
        TransactionType type = askType(sc);
        System.out.print("Số tiền mới: ");
        BigDecimal amount = new BigDecimal(sc.nextLine());
        System.out.print("Tiêu đề mới: ");
        String title = sc.nextLine();
        System.out.print("Ghi chú mới: ");
        String note = sc.nextLine();
        LocalDate date = askDate(sc);

        Transaction tx = opt.get();
        tx.setWalletId(walletId);
        tx.setCategoryId(categoryId);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setTitle(title);
        tx.setNote(note);
        tx.setTransactionDate(date);
        boolean ok = transactionService.update(tx);
        System.out.println(ok ? "Đã cập nhật." : "Cập nhật thất bại.");
    }

    private void delete(Scanner sc, int userId) {
        System.out.print("ID giao dịch cần xóa: ");
        int id = Integer.parseInt(sc.nextLine());
        boolean ok = transactionService.delete(id, userId);
        System.out.println(ok ? "Đã xóa." : "Không tìm thấy.");
    }

    private void listWallets(int userId) {
        List<Wallet> wallets = walletService.listByUser(userId);
        System.out.println("Ví hiện có:");
        wallets.forEach(w -> System.out.printf("  %d - %s (%s) số dư: %s%n", w.getId(), w.getName(), w.getType(), w.getBalance()));
    }

    private void listCategories(int userId) {
        List<Category> categories = categoryService.list(userId);
        System.out.println("Danh mục:");
        categories.forEach(c -> System.out.printf("  %d - %s (%s)%n", c.getId(), c.getName(), c.getType()));
    }

    private TransactionType askType(Scanner sc) {
        while (true) {
            System.out.print("Loại (EXPENSE/INCOME): ");
            String type = sc.nextLine().trim().toUpperCase(Locale.ROOT);
            try {
                return TransactionType.valueOf(type);
            } catch (IllegalArgumentException e) {
                System.out.println("Sai loại, nhập lại.");
            }
        }
    }

    private LocalDate askDate(Scanner sc) {
        while (true) {
            System.out.print("Ngày (yyyy-MM-dd): ");
            String input = sc.nextLine();
            try {
                return LocalDate.parse(input);
            } catch (Exception e) {
                System.out.println("Sai định dạng ngày, nhập lại.");
            }
        }
    }
}
