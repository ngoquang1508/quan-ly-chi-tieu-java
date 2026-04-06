package com.expensemanager.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Transaction {
    private int id;
    private int userId;
    private int walletId;
    private int categoryId;
    private BigDecimal amount;
    private TransactionType type;
    private String title;
    private String note;
    private LocalDate transactionDate;
    private LocalDateTime createdAt;

    public Transaction() {
    }

    public Transaction(int id, int userId, int walletId, int categoryId, BigDecimal amount, TransactionType type,
                       String title, String note, LocalDate transactionDate, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.walletId = walletId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.type = type;
        this.title = title;
        this.note = note;
        this.transactionDate = transactionDate;
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

    public int getWalletId() {
        return walletId;
    }

    public void setWalletId(int walletId) {
        this.walletId = walletId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
