package com.expensemanager.model;

public enum WalletType {
    CASH,
    BANK,
    EWALLET;

    @Override
    public String toString() {
        return switch (this) {
            case CASH -> "Tiền mặt";
            case BANK -> "Ngân hàng";
            case EWALLET -> "Ví điện tử";
        };
    }
}
