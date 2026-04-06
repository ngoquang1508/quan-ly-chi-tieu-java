package com.expensemanager.service;

import com.expensemanager.model.Budget;
import com.expensemanager.model.Transaction;
import com.expensemanager.model.TransactionType;
import com.expensemanager.model.Wallet;
import com.expensemanager.repository.TransactionRepository;
import com.expensemanager.repository.WalletRepository;
import com.expensemanager.repository.CategoryRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.List;
import java.util.Optional;

public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;
    private final NotificationService notificationService;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              BudgetService budgetService,
                              NotificationService notificationService,
                              WalletRepository walletRepository,
                              CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.budgetService = budgetService;
        this.notificationService = notificationService;
        this.walletRepository = walletRepository;
        this.categoryRepository = categoryRepository;
    }

    public Transaction add(Transaction tx) {
        Transaction saved = transactionRepository.create(tx);
        checkBudgetAndNotify(saved);
        pushBalanceChangeNotification(saved, "Thêm giao dịch");
        return saved;
    }

    public boolean update(Transaction tx) {
        boolean ok = transactionRepository.update(tx);
        if (ok) {
            checkBudgetAndNotify(tx);
            pushBalanceChangeNotification(tx, "Cập nhật giao dịch");
        }
        return ok;
    }

    public boolean delete(int id, int userId) {
        Optional<Transaction> existing = transactionRepository.findById(id, userId);
        boolean ok = transactionRepository.delete(id, userId);
        existing.ifPresent(tx -> {
            if (ok) {
                pushBalanceChangeNotification(tx, "Xóa giao dịch");
            }
        });
        return ok;
    }

    public List<Transaction> list(int userId) {
        return transactionRepository.findAllByUser(userId);
    }

    public Optional<Transaction> get(int id, int userId) {
        return transactionRepository.findById(id, userId);
    }

    private void checkBudgetAndNotify(Transaction tx) {
        if (tx.getType() != TransactionType.EXPENSE) {
            return; // budget only for expense
        }
        LocalDate date = tx.getTransactionDate();
        Optional<Budget> budgetOpt = budgetService.find(tx.getUserId(), tx.getCategoryId(), date.getMonthValue(), date.getYear());
        if (budgetOpt.isEmpty()) {
            return;
        }
        Budget budget = budgetOpt.get();
        BigDecimal spent = transactionRepository.totalExpenseForCategoryMonth(tx.getUserId(), tx.getCategoryId(), date.getMonthValue(), date.getYear());
        if (spent.compareTo(budget.getAmountLimit()) > 0) {
            String title = "Vượt ngân sách " + date.getMonthValue() + "/" + date.getYear();
            String categoryName = categoryRepository.findById(tx.getCategoryId(), tx.getUserId())
                    .map(c -> c.getName() + " (" + c.getType() + ")")
                    .orElse("Danh mục #" + tx.getCategoryId());
            String message = String.format("%s đã chi %s / giới hạn %s",
                    categoryName, formatVnd(spent), formatVnd(budget.getAmountLimit()));
            notificationService.push(tx.getUserId(), title, message);
        }
    }

    private void pushBalanceChangeNotification(Transaction tx, String action) {
        Optional<Wallet> walletOpt = walletRepository.findById(tx.getWalletId(), tx.getUserId());
        String walletName = walletOpt.map(Wallet::getName).orElse("Ví #" + tx.getWalletId());
        BigDecimal balance = walletOpt.map(Wallet::getBalance).orElse(BigDecimal.ZERO);
        String categoryName = categoryRepository.findById(tx.getCategoryId(), tx.getUserId())
                .map(c -> c.getName() + " (" + c.getType() + ")").orElse("Danh mục #" + tx.getCategoryId());
        String amount = formatVnd(tx.getAmount());
        String balanceStr = formatVnd(balance);
        String title = action;
        String msg = String.format("%s %s | %s | Ví: %s | Số dư mới: %s",
                tx.getType(), amount, categoryName, walletName, balanceStr);
        notificationService.push(tx.getUserId(), title, msg);
    }

    private String formatVnd(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(value);
    }
}
