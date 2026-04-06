package com.expensemanager.service;

import com.expensemanager.model.TransactionType;
import com.expensemanager.model.Wallet;
import com.expensemanager.repository.TransactionRepository;
import com.expensemanager.repository.WalletRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReportService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public ReportService(TransactionRepository transactionRepository, WalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    public BigDecimal totalIncomeByMonth(int userId, int month, int year) {
        return transactionRepository.totalByTypeAndMonth(userId, TransactionType.INCOME, month, year);
    }

    public BigDecimal totalExpenseByMonth(int userId, int month, int year) {
        return transactionRepository.totalByTypeAndMonth(userId, TransactionType.EXPENSE, month, year);
    }

    public BigDecimal totalIncomeByDate(int userId, LocalDate date) {
        return transactionRepository.totalByTypeAndDate(userId, TransactionType.INCOME, date);
    }

    public BigDecimal totalExpenseByDate(int userId, LocalDate date) {
        return transactionRepository.totalByTypeAndDate(userId, TransactionType.EXPENSE, date);
    }

    public BigDecimal totalIncomeByYear(int userId, int year) {
        return transactionRepository.totalByTypeAndYear(userId, TransactionType.INCOME, year);
    }

    public BigDecimal totalExpenseByYear(int userId, int year) {
        return transactionRepository.totalByTypeAndYear(userId, TransactionType.EXPENSE, year);
    }

    public Optional<String> topExpenseCategory(int userId, int month, int year) {
        return transactionRepository.topExpenseCategoryName(userId, month, year);
    }

    public BigDecimal totalWalletBalance(int userId) {
        List<Wallet> wallets = walletRepository.findAllByUser(userId);
        return wallets.stream()
                .map(Wallet::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
