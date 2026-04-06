package com.expensemanager.repository;

import com.expensemanager.config.DBConnection;
import com.expensemanager.model.Notification;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {

    public Notification create(Notification notification) {
        String sql = "INSERT INTO notifications(user_id, title, message, is_read) VALUES(?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, notification.getUserId());
            ps.setString(2, notification.getTitle());
            ps.setString(3, notification.getMessage());
            ps.setBoolean(4, notification.isRead());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    notification.setId(keys.getInt(1));
                }
            }
            return notification;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create notification", e);
        }
    }

    public List<Notification> findByUser(int userId) {
        String sql = "SELECT id, user_id, title, message, is_read, created_at FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch notifications", e);
        }
    }

    public boolean markAsRead(int id, int userId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark notification as read", e);
        }
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        LocalDateTime created = rs.getTimestamp("created_at").toLocalDateTime();
        return new Notification(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("title"),
                rs.getString("message"),
                rs.getBoolean("is_read"),
                created
        );
    }
}
