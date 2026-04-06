package com.expensemanager.repository;

import com.expensemanager.config.DBConnection;
import com.expensemanager.model.Transaction;
import com.expensemanager.model.TransactionType;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TransactionRepository {

    public Transaction create(Transaction tx) {
        String sql = "INSERT INTO transactions(user_id, wallet_id, category_id, amount, type, title, note, transaction_date) " +
                "VALUES(?,?,?,?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tx.getUserId());
            ps.setInt(2, tx.getWalletId());
            ps.setInt(3, tx.getCategoryId());
            ps.setBigDecimal(4, tx.getAmount());
            ps.setString(5, tx.getType().name());
            ps.setString(6, tx.getTitle());
            ps.setString(7, tx.getNote());
            ps.setDate(8, Date.valueOf(tx.getTransactionDate()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    tx.setId(keys.getInt(1));
                }
            }
            return tx;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create transaction", e);
        }
    }

    public boolean update(Transaction tx) {
        String sql = "UPDATE transactions SET wallet_id = ?, category_id = ?, amount = ?, type = ?, title = ?, note = ?, transaction_date = ? " +
                "WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tx.getWalletId());
            ps.setInt(2, tx.getCategoryId());
            ps.setBigDecimal(3, tx.getAmount());
            ps.setString(4, tx.getType().name());
            ps.setString(5, tx.getTitle());
            ps.setString(6, tx.getNote());
            ps.setDate(7, Date.valueOf(tx.getTransactionDate()));
            ps.setInt(8, tx.getId());
            ps.setInt(9, tx.getUserId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update transaction", e);
        }
    }

    public boolean delete(int id, int userId) {
        String sql = "DELETE FROM transactions WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete transaction", e);
        }
    }

    public List<Transaction> findAllByUser(int userId) {
        String sql = "SELECT id, user_id, wallet_id, category_id, amount, type, title, note, transaction_date, created_at " +
                "FROM transactions WHERE user_id = ? ORDER BY transaction_date DESC, created_at DESC";
        List<Transaction> list = new ArrayList<>();
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
            throw new RuntimeException("Failed to list transactions", e);
        }
    }

    public Optional<Transaction> findById(int id, int userId) {
        String sql = "SELECT id, user_id, wallet_id, category_id, amount, type, title, note, transaction_date, created_at " +
                "FROM transactions WHERE id = ? AND user_id = ?";
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
            throw new RuntimeException("Failed to find transaction", e);
        }
    }

    public List<Transaction> findByUserAndMonthYear(int userId, int month, int year) {
        String sql = "SELECT id, user_id, wallet_id, category_id, amount, type, title, note, transaction_date, created_at " +
                "FROM transactions WHERE user_id = ? AND MONTH(transaction_date) = ? AND YEAR(transaction_date) = ? " +
                "ORDER BY transaction_date DESC, created_at DESC";
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list transactions by month", e);
        }
    }

    public BigDecimal totalExpenseForCategoryMonth(int userId, int categoryId, int month, int year) {
        String sql = "SELECT COALESCE(SUM(amount),0) AS total FROM transactions " +
                "WHERE user_id = ? AND category_id = ? AND type = 'EXPENSE' " +
                "AND MONTH(transaction_date) = ? AND YEAR(transaction_date) = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, categoryId);
            ps.setInt(3, month);
            ps.setInt(4, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total");
                }
            }
            return BigDecimal.ZERO;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to sum expense", e);
        }
    }

    public BigDecimal totalByTypeAndMonth(int userId, TransactionType type, int month, int year) {
        String sql = "SELECT COALESCE(SUM(amount),0) AS total FROM transactions WHERE user_id = ? AND type = ? " +
                "AND MONTH(transaction_date) = ? AND YEAR(transaction_date) = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type.name());
            ps.setInt(3, month);
            ps.setInt(4, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total");
                }
            }
            return BigDecimal.ZERO;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to sum by month", e);
        }
    }

    public BigDecimal totalByTypeAndYear(int userId, TransactionType type, int year) {
        String sql = "SELECT COALESCE(SUM(amount),0) AS total FROM transactions WHERE user_id = ? AND type = ? AND YEAR(transaction_date) = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type.name());
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total");
                }
            }
            return BigDecimal.ZERO;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to sum by year", e);
        }
    }

    public BigDecimal totalByTypeAndDate(int userId, TransactionType type, LocalDate date) {
        String sql = "SELECT COALESCE(SUM(amount),0) AS total FROM transactions WHERE user_id = ? AND type = ? AND transaction_date = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type.name());
            ps.setDate(3, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total");
                }
            }
            return BigDecimal.ZERO;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to sum by date", e);
        }
    }

    public Optional<String> topExpenseCategoryName(int userId, int month, int year) {
        String sql = "SELECT c.name, SUM(t.amount) AS total " +
                "FROM transactions t JOIN categories c ON t.category_id = c.id " +
                "WHERE t.user_id = ? AND t.type = 'EXPENSE' AND MONTH(t.transaction_date) = ? AND YEAR(t.transaction_date) = ? " +
                "GROUP BY c.name ORDER BY total DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("name") + " (" + rs.getBigDecimal("total") + ")");
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch top expense category", e);
        }
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        LocalDateTime created = rs.getTimestamp("created_at").toLocalDateTime();
        return new Transaction(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getInt("wallet_id"),
                rs.getInt("category_id"),
                rs.getBigDecimal("amount"),
                TransactionType.valueOf(rs.getString("type")),
                rs.getString("title"),
                rs.getString("note"),
                rs.getDate("transaction_date").toLocalDate(),
                created
        );
    }
}
