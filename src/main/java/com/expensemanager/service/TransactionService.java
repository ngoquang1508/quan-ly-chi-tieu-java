package com.expensemanager.service;

import com.expensemanager.model.Budget;
import com.expensemanager.model.Transaction;
import com.expensemanager.model.TransactionType;
import com.expensemanager.model.Wallet;
import com.expensemanager.repository.CategoryRepository;
import com.expensemanager.repository.TransactionRepository;
import com.expensemanager.repository.WalletRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
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
        enforceBudgetLimitBeforeAdd(tx);
        Transaction saved = transactionRepository.create(tx);
        checkBudgetAndNotify(saved);
        pushBalanceChangeNotification(saved, "Biến động số dư");
        return saved;
    }

    public boolean update(Transaction tx) {
        Optional<Transaction> existingOpt = transactionRepository.findById(tx.getId(), tx.getUserId());
        if (existingOpt.isEmpty()) {
            return false;
        }

        enforceBudgetLimitBeforeUpdate(tx, existingOpt.get());

        boolean ok = transactionRepository.update(tx);
        if (ok) {
            checkBudgetAndNotify(tx);
            pushBalanceChangeNotification(tx, "Biến động số dư");
        }
        return ok;
    }

    public boolean delete(int id, int userId) {
        Optional<Transaction> existing = transactionRepository.findById(id, userId);
        boolean ok = transactionRepository.delete(id, userId);
        existing.ifPresent(tx -> {
            if (ok) {
                pushBalanceChangeNotification(tx, "Biến động số dư");
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

    public Optional<String> budgetExceededWarning(Transaction tx) {
        return buildBudgetExceededMessage(tx);
    }

    public Optional<String> walletOverdrawnWarning(Transaction tx) {
        return buildWalletOverdrawnMessage(tx);
    }

    public Optional<String> walletOverdrawnWarningBeforeAdd(Transaction tx) {
        return buildWalletOverdrawnMessageBeforeAdd(tx);
    }

    public Optional<String> walletOverdrawnWarningBeforeUpdate(Transaction tx) {
        Optional<Transaction> existingOpt = transactionRepository.findById(tx.getId(), tx.getUserId());
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }
        return buildWalletOverdrawnMessageBeforeUpdate(tx, existingOpt.get());
    }

    private void enforceBudgetLimitBeforeAdd(Transaction tx) {
        if (tx.getType() != TransactionType.EXPENSE) {
            return;
        }
        Optional<Budget> budgetOpt = budgetFor(tx);
        if (budgetOpt.isEmpty()) {
            return;
        }

        Budget budget = budgetOpt.get();
        if (tx.getAmount().compareTo(budget.getAmountLimit()) > 0) {
            throw new IllegalArgumentException("Chi tiêu vượt giới hạn ngân sách ("
                    + formatVnd(tx.getAmount().abs()) + " / " + formatVnd(budget.getAmountLimit()) + ")");
        }
    }

    private void enforceBudgetLimitBeforeUpdate(Transaction tx, Transaction existing) {
        if (tx.getType() != TransactionType.EXPENSE) {
            return;
        }
        Optional<Budget> budgetOpt = budgetFor(tx);
        if (budgetOpt.isEmpty()) {
            return;
        }

        Budget budget = budgetOpt.get();
        if (tx.getAmount().compareTo(budget.getAmountLimit()) > 0) {
            throw new IllegalArgumentException("Chi tiêu vượt giới hạn ngân sách ("
                    + formatVnd(tx.getAmount().abs()) + " / " + formatVnd(budget.getAmountLimit()) + ")");
        }
    }

    private void checkBudgetAndNotify(Transaction tx) {
        buildBudgetExceededMessage(tx).ifPresent(message -> {
            LocalDate date = tx.getTransactionDate();
            String title = "Vượt ngân sách " + date.getMonthValue() + "/" + date.getYear();
            notificationService.push(tx.getUserId(), title, message);
        });
    }

    private Optional<String> buildBudgetExceededMessage(Transaction tx) {
        if (tx.getType() != TransactionType.EXPENSE) {
            return Optional.empty();
        }
        Optional<Budget> budgetOpt = budgetFor(tx);
        if (budgetOpt.isEmpty()) {
            return Optional.empty();
        }

        Budget budget = budgetOpt.get();
        if (tx.getAmount().compareTo(budget.getAmountLimit()) <= 0) {
            return Optional.empty();
        }

        String categoryName = categoryRepository.findById(tx.getCategoryId(), tx.getUserId())
                .map(c -> c.getName())
                .orElse("Danh mục #" + tx.getCategoryId());
        String message = String.format("%s chi %s / giới hạn %s",
                categoryName, formatVnd(tx.getAmount().abs()), formatVnd(budget.getAmountLimit()));
        return Optional.of(message);
    }

    private Optional<String> buildWalletOverdrawnMessage(Transaction tx) {
        if (tx.getType() != TransactionType.EXPENSE) {
            return Optional.empty();
        }
        Optional<Wallet> walletOpt = walletRepository.findById(tx.getWalletId(), tx.getUserId());
        if (walletOpt.isEmpty()) {
            return Optional.empty();
        }
        Wallet wallet = walletOpt.get();
        if (wallet.getBalance().compareTo(BigDecimal.ZERO) >= 0) {
            return Optional.empty();
        }
        return Optional.of("Ví không đủ tiền, tạo giao dịch. Số dư ví: " + formatSignedVnd(wallet.getBalance()));
    }

    private Optional<String> buildWalletOverdrawnMessageBeforeAdd(Transaction tx) {
        if (tx.getType() != TransactionType.EXPENSE) {
            return Optional.empty();
        }
        Optional<Wallet> walletOpt = walletRepository.findById(tx.getWalletId(), tx.getUserId());
        if (walletOpt.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal projected = walletOpt.get().getBalance().subtract(tx.getAmount());
        if (projected.compareTo(BigDecimal.ZERO) >= 0) {
            return Optional.empty();
        }
        return Optional.of("Ví không đủ tiền, tạo giao dịch. Số dư ví: " + formatSignedVnd(projected));
    }

    private Optional<String> buildWalletOverdrawnMessageBeforeUpdate(Transaction tx, Transaction existing) {
        if (tx.getType() != TransactionType.EXPENSE) {
            return Optional.empty();
        }
        Optional<Wallet> walletOpt = walletRepository.findById(tx.getWalletId(), tx.getUserId());
        if (walletOpt.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal base = walletOpt.get().getBalance();
        if (existing.getWalletId() == tx.getWalletId()) {
            if (existing.getType() == TransactionType.EXPENSE) {
                base = base.add(existing.getAmount());
            } else if (existing.getType() == TransactionType.INCOME) {
                base = base.subtract(existing.getAmount());
            }
        }

        BigDecimal projected = base.subtract(tx.getAmount());
        if (projected.compareTo(BigDecimal.ZERO) >= 0) {
            return Optional.empty();
        }
        return Optional.of("Ví không đủ tiền, tạo giao dịch. Số dư ví: " + formatSignedVnd(projected));
    }

    private Optional<Budget> budgetFor(Transaction tx) {
        LocalDate date = tx.getTransactionDate();
        return budgetService.find(tx.getUserId(), tx.getCategoryId(), date.getMonthValue(), date.getYear());
    }

    private boolean hasBudgetLimit(Transaction tx) {
        return budgetFor(tx).isPresent();
    }

    private void pushBalanceChangeNotification(Transaction tx, String action) {
        Optional<Wallet> walletOpt = walletRepository.findById(tx.getWalletId(), tx.getUserId());
        String walletName = walletOpt.map(Wallet::getName).orElse("Ví #" + tx.getWalletId());
        BigDecimal balance = walletOpt.map(Wallet::getBalance).orElse(BigDecimal.ZERO);
        String categoryName = categoryRepository.findById(tx.getCategoryId(), tx.getUserId())
                .map(c -> c.getName())
                .orElse("Danh mục #" + tx.getCategoryId());

        String typeLabel = tx.getType() == TransactionType.INCOME ? "Thu nhập" : "Chi tiêu";
        String amount = formatVnd(tx.getAmount().abs());
        String balanceStr = formatSignedVnd(balance);

        String message = String.format("%s %s - %s | Ví: %s | Số dư: %s",
                typeLabel, amount, categoryName, walletName, balanceStr);
        notificationService.push(tx.getUserId(), action, message);
    }

    private String formatVnd(BigDecimal value) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.setMinimumFractionDigits(0);
        return numberFormat.format(value) + "đ";
    }

    private String formatSignedVnd(BigDecimal value) {
        String sign = value.compareTo(BigDecimal.ZERO) < 0 ? "-" : "";
        return sign + formatVnd(value.abs());
    }
}

