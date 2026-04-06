package com.expensemanager.controller;

import com.expensemanager.model.Notification;
import com.expensemanager.service.NotificationService;

import java.util.List;
import java.util.Scanner;

public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void menu(Scanner sc, int userId) {
        while (true) {
            System.out.println("\n=== THÔNG BÁO ===");
            System.out.println("1. Xem danh sách thông báo");
            System.out.println("2. Đánh dấu đã đọc");
            System.out.println("0. Quay lại");
            System.out.print("Chọn: ");
            String c = sc.nextLine();
            switch (c) {
                case "1" -> list(userId);
                case "2" -> mark(sc, userId);
                case "0" -> {
                    return;
                }
                default -> System.out.println("Sai lựa chọn");
            }
        }
    }

    private void list(int userId) {
        List<Notification> list = notificationService.list(userId);
        if (list.isEmpty()) {
            System.out.println("Không có thông báo.");
            return;
        }
        list.forEach(n ->
                System.out.printf("ID:%d | %s | %s | Đọc:%s%n",
                        n.getId(), n.getTitle(), n.getMessage(), n.isRead()));
    }

    private void mark(Scanner sc, int userId) {
        System.out.print("ID thông báo: ");
        int id = Integer.parseInt(sc.nextLine());
        boolean ok = notificationService.markAsRead(id, userId);
        System.out.println(ok ? "Đã đánh dấu." : "Không tìm thấy.");
    }
}
