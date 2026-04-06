package com.expensemanager.controller;

import com.expensemanager.model.Wallet;
import com.expensemanager.model.WalletType;
import com.expensemanager.service.WalletService;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;

public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    public void menu(Scanner sc, int userId) {
        while (true) {
            System.out.println("\n=== QUẢN LÝ VÍ ===");
            System.out.println("1. Tạo ví");
            System.out.println("2. Xem danh sách ví");
            System.out.println("3. Cập nhật ví");
            System.out.println("4. Xóa ví");
            System.out.println("0. Quay lại");
            System.out.print("Chọn: ");
            String choice = sc.nextLine();
            switch (choice) {
                case "1" -> create(sc, userId);
                case "2" -> list(userId);
                case "3" -> update(sc, userId);
                case "4" -> delete(sc, userId);
                case "0" -> {
                    return;
                }
                default -> System.out.println("Lựa chọn không hợp lệ");
            }
        }
    }

    private void create(Scanner sc, int userId) {
        System.out.print("Tên ví: ");
        String name = sc.nextLine();
        WalletType type = askType(sc);
        Wallet wallet = walletService.create(userId, name, type.name());
        System.out.println("Đã tạo ví #" + wallet.getId());
    }

    private void list(int userId) {
        List<Wallet> wallets = walletService.listByUser(userId);
        if (wallets.isEmpty()) {
            System.out.println("Chưa có ví nào.");
            return;
        }
        wallets.forEach(w ->
                System.out.printf("ID: %d | Tên: %s | Loại: %s | Số dư: %s%n",
                        w.getId(), w.getName(), w.getType(), w.getBalance()));
    }

    private void update(Scanner sc, int userId) {
        System.out.print("Nhập ID ví cần sửa: ");
        int id = Integer.parseInt(sc.nextLine());
        Optional<Wallet> wallet = walletService.get(id, userId);
        if (wallet.isEmpty()) {
            System.out.println("Không tìm thấy ví.");
            return;
        }
        System.out.print("Tên mới: ");
        String name = sc.nextLine();
        WalletType type = askType(sc);
        boolean ok = walletService.update(userId, id, name, type.name());
        System.out.println(ok ? "Đã cập nhật." : "Cập nhật thất bại.");
    }

    private void delete(Scanner sc, int userId) {
        System.out.print("Nhập ID ví cần xóa: ");
        int id = Integer.parseInt(sc.nextLine());
        boolean ok = walletService.delete(userId, id);
        System.out.println(ok ? "Đã xóa." : "Không tìm thấy ví.");
    }

    private WalletType askType(Scanner sc) {
        while (true) {
            System.out.print("Loại (CASH/BANK/EWALLET): ");
            String type = sc.nextLine().trim().toUpperCase(Locale.ROOT);
            try {
                return WalletType.valueOf(type);
            } catch (IllegalArgumentException e) {
                System.out.println("Sai loại, nhập lại.");
            }
        }
    }
}
