package com.expensemanager.repository;

import com.expensemanager.config.DBConnection;
import com.expensemanager.model.Category;
import com.expensemanager.model.CategoryType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryRepository {

    public Category create(Category category) {
        String sql = "INSERT INTO categories(user_id, name, type, icon) VALUES(?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, category.getUserId());
            ps.setString(2, category.getName());
            ps.setString(3, category.getType().name());
            ps.setString(4, category.getIcon());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    category.setId(keys.getInt(1));
                }
            }
            return category;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create category", e);
        }
    }

    public boolean update(Category category) {
        String sql = "UPDATE categories SET name = ?, type = ?, icon = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getType().name());
            ps.setString(3, category.getIcon());
            ps.setInt(4, category.getId());
            ps.setInt(5, category.getUserId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update category", e);
        }
    }

    public boolean delete(int id, int userId) {
        String sql = "DELETE FROM categories WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete category", e);
        }
    }

    public List<Category> findAllByUser(int userId) {
        String sql = "SELECT id, user_id, name, type, icon, created_at FROM categories WHERE user_id = ? ORDER BY created_at DESC";
        List<Category> list = new ArrayList<>();
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
            throw new RuntimeException("Failed to list categories", e);
        }
    }

    public Optional<Category> findById(int id, int userId) {
        String sql = "SELECT id, user_id, name, type, icon, created_at FROM categories WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find category", e);
        }
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        LocalDateTime created = rs.getTimestamp("created_at").toLocalDateTime();
        return new Category(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("name"),
                CategoryType.valueOf(rs.getString("type")),
                rs.getString("icon"),
                created
        );
    }
}
