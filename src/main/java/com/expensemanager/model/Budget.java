package com.expensemanager.model;

import java.math.BigDecimal;

public class Budget {
    private int id;
    private int userId;
    private Integer categoryId;
    private BigDecimal amountLimit;
    private int month;
    private int year;

    public Budget() {
    }

    public Budget(int id, int userId, Integer categoryId, BigDecimal amountLimit, int month, int year) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.amountLimit = amountLimit;
        this.month = month;
        this.year = year;
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

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getAmountLimit() {
        return amountLimit;
    }

    public void setAmountLimit(BigDecimal amountLimit) {
        this.amountLimit = amountLimit;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }
}
