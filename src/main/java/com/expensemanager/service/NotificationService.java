package com.expensemanager.service;

import com.expensemanager.model.Notification;
import com.expensemanager.repository.NotificationRepository;

import java.util.List;

public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification push(int userId, String title, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        return notificationRepository.create(notification);
    }

    public List<Notification> list(int userId) {
        return notificationRepository.findByUser(userId);
    }

    public boolean markAsRead(int id, int userId) {
        return notificationRepository.markAsRead(id, userId);
    }
}
