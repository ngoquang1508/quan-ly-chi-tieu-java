package com.expensemanager.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Wallet {
    private int id;
    private int userId;
    private String name;
    private BigDecimal balance;
    private WalletType type;
    private LocalDateTime createdAt;

    public Wallet() {
    }

    public Wallet(int id, int userId, String name, BigDecimal balance, WalletType type, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.balance = balance;
        this.type = type;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public WalletType getType() {
        return type;
    }

    public void setType(WalletType type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        if (id == 0) {
            return name;
        }
        return name + " (" + type + ")";
    }
}
