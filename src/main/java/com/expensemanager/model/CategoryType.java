package com.expensemanager.model;

public enum CategoryType {
    EXPENSE,
    INCOME;

    @Override
    public String toString() {
        return this == INCOME ? "Thu nhập" : "Chi tiêu";
    }
}
