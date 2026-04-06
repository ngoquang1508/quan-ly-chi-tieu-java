package com.expensemanager.repository;

import com.expensemanager.config.DBConnection;
import com.expensemanager.model.Budget;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BudgetRepository {

    public Budget create(Budget budget) {
        return insert(budget);
    }

    public Budget saveOrUpdate(Budget budget) {
        Optional<Budget> existing = findByUserAndCategoryAndMonth(budget.getUserId(), budget.getCategoryId(),
                budget.getMonth(), budget.getYear());
        if (existing.isPresent()) {
            budget.setId(existing.get().getId());
            updateAmount(budget);
            return budget;
        }
        return insert(budget);
    }

    private Budget insert(Budget budget) {
        String sql = "INSERT INTO budgets(user_id, category_id, amount_limit, month, year) VALUES(?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, budget.getUserId());
            if (budget.getCategoryId() == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, budget.getCategoryId());
            }
            ps.setBigDecimal(3, budget.getAmountLimit());
            ps.setInt(4, budget.getMonth());
            ps.setInt(5, budget.getYear());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    budget.setId(keys.getInt(1));
                }
            }
            return budget;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert budget", e);
        }
    }

    private void updateAmount(Budget budget) {
        String sql = "UPDATE budgets SET amount_limit = ?, category_id = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, budget.getAmountLimit());
            if (budget.getCategoryId() == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, budget.getCategoryId());
            }
            ps.setInt(3, budget.getId());
            ps.setInt(4, budget.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update budget", e);
        }
    }

    public boolean update(Budget budget) {
        String sql = "UPDATE budgets SET category_id = ?, amount_limit = ?, month = ?, year = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (budget.getCategoryId() == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, budget.getCategoryId());
            }
            ps.setBigDecimal(2, budget.getAmountLimit());
            ps.setInt(3, budget.getMonth());
            ps.setInt(4, budget.getYear());
            ps.setInt(5, budget.getId());
            ps.setInt(6, budget.getUserId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update budget by id", e);
        }
    }

    public boolean delete(int id, int userId) {
        String sql = "DELETE FROM budgets WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete budget", e);
        }
    }

    public List<Budget> findByUser(int userId) {
        String sql = "SELECT id, user_id, category_id, amount_limit, month, year FROM budgets WHERE user_id = ? ORDER BY year DESC, month DESC";
        List<Budget> list = new ArrayList<>();
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
            throw new RuntimeException("Failed to list budgets", e);
        }
    }

    public Optional<Budget> findByUserAndCategoryAndMonth(int userId, Integer categoryId, int month, int year) {
        String sql = "SELECT id, user_id, category_id, amount_limit, month, year FROM budgets " +
                "WHERE user_id = ? AND category_id " + (categoryId == null ? "IS NULL" : "= ?") +
                " AND month = ? AND year = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, userId);
            if (categoryId != null) {
                ps.setInt(idx++, categoryId);
            }
            ps.setInt(idx++, month);
            ps.setInt(idx, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find budget", e);
        }
    }

    private Budget mapRow(ResultSet rs) throws SQLException {
        Integer categoryId = rs.getObject("category_id") != null ? rs.getInt("category_id") : null;
        BigDecimal amount = rs.getBigDecimal("amount_limit");
        return new Budget(
                rs.getInt("id"),
                rs.getInt("user_id"),
                categoryId,
                amount,
                rs.getInt("month"),
                rs.getInt("year")
        );
    }
}
