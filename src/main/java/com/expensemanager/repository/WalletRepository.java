package com.expensemanager.repository;

import com.expensemanager.config.DBConnection;
import com.expensemanager.model.Wallet;
import com.expensemanager.model.WalletType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WalletRepository {

    public Wallet create(Wallet wallet) {
        String sql = "INSERT INTO wallets(user_id, name, balance, type) VALUES(?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, wallet.getUserId());
            ps.setString(2, wallet.getName());
            ps.setBigDecimal(3, wallet.getBalance());
            ps.setString(4, wallet.getType().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    wallet.setId(keys.getInt(1));
                }
            }
            return wallet;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create wallet", e);
        }
    }

    public boolean update(Wallet wallet) {
        String sql = "UPDATE wallets SET name = ?, type = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, wallet.getName());
            ps.setString(2, wallet.getType().name());
            ps.setInt(3, wallet.getId());
            ps.setInt(4, wallet.getUserId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update wallet", e);
        }
    }

    public boolean delete(int id, int userId) {
        String sql = "DELETE FROM wallets WHERE id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete wallet", e);
        }
    }

    public List<Wallet> findAllByUser(int userId) {
        String sql = "SELECT id, user_id, name, balance, type, created_at FROM wallets WHERE user_id = ? ORDER BY created_at DESC";
        List<Wallet> list = new ArrayList<>();
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
            throw new RuntimeException("Failed to list wallets", e);
        }
    }

    public Optional<Wallet> findById(int id, int userId) {
        String sql = "SELECT id, user_id, name, balance, type, created_at FROM wallets WHERE id = ? AND user_id = ?";
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
            throw new RuntimeException("Failed to find wallet", e);
        }
    }

    private Wallet mapRow(ResultSet rs) throws SQLException {
        LocalDateTime created = rs.getTimestamp("created_at").toLocalDateTime();
        return new Wallet(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("name"),
                rs.getBigDecimal("balance"),
                WalletType.valueOf(rs.getString("type")),
                created
        );
    }
}
