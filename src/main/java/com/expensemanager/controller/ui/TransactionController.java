package com.expensemanager.controller.ui;

import com.expensemanager.model.Category;
import com.expensemanager.model.Transaction;
import com.expensemanager.model.TransactionType;
import com.expensemanager.model.Wallet;
import com.expensemanager.model.WalletType;
import com.expensemanager.service.CategoryService;
import com.expensemanager.service.TransactionService;
import com.expensemanager.service.WalletService;
import com.expensemanager.view.UiHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class TransactionController {

    private final TransactionService transactionService;
    private final WalletService walletService;
    private final CategoryService categoryService;
    private final UiHelper ui;
    private final IntSupplier currentUserId;
    private Runnable onTransactionChanged;

    private TableView<Transaction> transactionTable;
    private ObservableList<Transaction> transactionData;
    private List<Transaction> allTransactions = new ArrayList<>();
    private final Map<Transaction, BooleanProperty> transactionCheckedMap = new HashMap<>();
    private ComboBox<Wallet> txWalletBox;
    private ComboBox<Category> txCategoryBox;
    private ComboBox<String> txTypeFilterBox;
    private ComboBox<Wallet> txWalletFilterBox;
    private ComboBox<String> txSortFilterBox;
    private Button transactionDeleteAllBtn;
    private Map<Integer, String> walletNameMap = new HashMap<>();
    private Map<Integer, String> categoryNameMap = new HashMap<>();

    public TransactionController(TransactionService transactionService,
            WalletService walletService,
            CategoryService categoryService,
            UiHelper ui,
            IntSupplier currentUserId) {
        this.transactionService = transactionService;
        this.walletService = walletService;
        this.categoryService = categoryService;
        this.ui = ui;
        this.currentUserId = currentUserId;
    }

    public void setOnTransactionChanged(Runnable onTransactionChanged) {
        this.onTransactionChanged = onTransactionChanged;
    }

    public Tab buildTab() {
        transactionTable = new TableView<>();
        transactionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        transactionDeleteAllBtn = new Button("Xóa");
        transactionDeleteAllBtn.setVisible(false);
        transactionDeleteAllBtn.setManaged(false);
        transactionTable.getColumns().addAll(
                ui.selectColumn(transactionTable, transactionCheckedMap, this::updateBulkDeleteButton),
                ui.sttColumn(transactionTable),
                ui.column("Ngày", "transactionDate"),
                ui.txTypeColumn("Loại"),
                ui.txAmountColumn("Số tiền"),
                ui.mappingColumn("Ví", Transaction::getWalletId, () -> walletNameMap, "Ví #"),
                ui.mappingColumn("Danh mục", Transaction::getCategoryId, () -> categoryNameMap, "DM #"),
                ui.column("Tiêu đề", "title"));
        transactionData = FXCollections.observableArrayList();
        transactionTable.setItems(transactionData);

        txWalletBox = new ComboBox<>();
        txCategoryBox = new ComboBox<>();
        refreshWalletAndCategory();

        txTypeFilterBox = new ComboBox<>(FXCollections.observableArrayList("TẤT CẢ", "Thu nhập", "Chi tiêu"));
        txTypeFilterBox.getSelectionModel().selectFirst();
        txWalletFilterBox = new ComboBox<>();
        txSortFilterBox = new ComboBox<>(FXCollections.observableArrayList(
                "Ngày giảm dần", "Ngày tăng dần", "Số tiền giảm dần", "Số tiền tăng dần"));
        txSortFilterBox.getSelectionModel().selectFirst();
        refreshTransactionFilterCombos();

        txTypeFilterBox.valueProperty().addListener((obs, o, n) -> applyTransactionFilter());
        txWalletFilterBox.valueProperty().addListener((obs, o, n) -> applyTransactionFilter());
        txSortFilterBox.valueProperty().addListener((obs, o, n) -> applyTransactionFilter());

        ComboBox<TransactionType> type = new ComboBox<>(FXCollections.observableArrayList(TransactionType.values()));
        type.getSelectionModel().select(TransactionType.EXPENSE);
        TextField amount = new TextField();
        TextField title = new TextField();
        TextField note = new TextField();
        DatePicker datePicker = new DatePicker(LocalDate.now());

        Button add = new Button("Thêm");
        add.setOnAction(e -> addTransaction(type, amount, title, note, datePicker));

        Button update = new Button("Sửa");
        update.setOnAction(e -> updateTransaction(type, amount, title, note, datePicker));

        transactionDeleteAllBtn.setOnAction(e -> deleteSelectedTransactions(type, amount, title, note, datePicker));

        transactionTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                selectWallet(txWalletBox, n.getWalletId());
                selectCategory(txCategoryBox, n.getCategoryId());
                type.getSelectionModel().select(n.getType());
                amount.setText(n.getAmount().toPlainString());
                title.setText(n.getTitle());
                note.setText(n.getNote());
                datePicker.setValue(n.getTransactionDate());
            }
        });

        VBox form = new VBox(8,
                new HBox(6, new Label("Ví"), txWalletBox),
                new HBox(6, new Label("Danh mục"), txCategoryBox),
                new HBox(6, new Label("Loại"), type),
                new HBox(6, new Label("Số tiền"), amount),
                new HBox(6, new Label("Tiêu đề"), title),
                new HBox(6, new Label("Ghi chú"), note),
                new HBox(6, new Label("Ngày"), datePicker),
                new HBox(6, add, update, transactionDeleteAllBtn));
        form.setPadding(new Insets(8));

        HBox filterBar = new HBox(8,
                new Label("Lọc loại"), txTypeFilterBox,
                new Label("Ví"), txWalletFilterBox,
                new Label("Sắp xếp"), txSortFilterBox);
        filterBar.setPadding(new Insets(8));

        BorderPane pane = new BorderPane();
        pane.setTop(filterBar);
        pane.setCenter(transactionTable);
        pane.setBottom(form);

        reloadTransactions();
        applyTransactionFilter();

        return new Tab("Giao dịch", pane);
    }

    public void refresh() {
        reloadTransactions();
        applyTransactionFilter();
    }

    public void onWalletChanged() {
        refreshWalletAndCategory();
        applyTransactionFilter();
    }

    public void onCategoryChanged() {
        refreshWalletAndCategory();
        applyTransactionFilter();
    }

    private void addTransaction(ComboBox<TransactionType> type, TextField amount, TextField title, TextField note, DatePicker datePicker) {
        try {
            if (txWalletBox.getValue() == null) {
                throw new IllegalArgumentException("Vui lòng chọn ví!");
            }

            if (txCategoryBox.getValue() == null) {
                throw new IllegalArgumentException("Vui lòng chọn danh mục!");
            }

            if (type.getValue() == null) {
                throw new IllegalArgumentException("Vui lòng chọn loại giao dịch!");
            }

            String amountVal = amount.getText();
            if (amountVal == null || amountVal.isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập số tiền!");
            }

            BigDecimal amountValParsed;
            try {
                amountValParsed = new BigDecimal(amountVal.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Số tiền phải là số!");
            }

            if (amountValParsed.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Số tiền phải lớn hơn 0!");
            }

            String titleVal = title.getText();
            if (titleVal == null || titleVal.isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập tiêu đề!");
            }

            if (datePicker.getValue() == null) {
                throw new IllegalArgumentException("Vui lòng chọn ngày!");
            }

            Transaction tx = new Transaction();
            tx.setUserId(currentUserId.getAsInt());
            tx.setWalletId(txWalletBox.getValue().getId());
            tx.setCategoryId(txCategoryBox.getValue().getId());
            tx.setType(type.getValue());
            tx.setAmount(amountValParsed);
            tx.setTitle(titleVal.trim());
            tx.setNote(note.getText());
            tx.setTransactionDate(datePicker.getValue());

            Optional<String> walletWarning = transactionService.walletOverdrawnWarningBeforeAdd(tx);
            Transaction savedTx = transactionService.add(tx);

            walletWarning.ifPresent(ui::warning);

            reloadTransactions();
            applyTransactionFilter();
            clearForm(type, amount, title, note, datePicker);
            transactionTable.getSelectionModel().select(savedTx);
            notifyChanged();

        } catch (IllegalArgumentException ex) {
            ui.warning(ex.getMessage());
        } catch (Exception ex) {
            ui.error("Không thêm được giao dịch: " + ex.getMessage());
        }
    }

    private void updateTransaction(ComboBox<TransactionType> type, TextField amount, TextField title, TextField note,
            DatePicker datePicker) {
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            ui.info("Chọn giao dịch để sửa");
            return;
        }
        try {
            selected.setWalletId(txWalletBox.getValue().getId());
            selected.setCategoryId(txCategoryBox.getValue().getId());
            selected.setType(type.getValue());
            selected.setAmount(new BigDecimal(amount.getText()));
            selected.setTitle(title.getText());
            selected.setNote(note.getText());
            selected.setTransactionDate(datePicker.getValue());
            Optional<String> walletWarning = transactionService.walletOverdrawnWarningBeforeUpdate(selected);
            boolean updated = transactionService.update(selected);
            if (!updated) {
                ui.error("Không sửa được giao dịch");
                return;
            }
            walletWarning.ifPresent(ui::warning);
            reloadTransactions();
            applyTransactionFilter();
            clearForm(type, amount, title, note, datePicker);
            notifyChanged();
        } catch (IllegalArgumentException ex) {
            ui.info(ex.getMessage());
        } catch (Exception ex) {
            ui.error("Không sửa được: " + ex.getMessage());
        }
    }

    private void deleteSelectedTransactions(ComboBox<TransactionType> type, TextField amount, TextField title,
            TextField note, DatePicker datePicker) {
        List<Transaction> selected = checkedItems(transactionCheckedMap);
        if (selected.isEmpty()) {
            return;
        }
        if (!ui.confirmDelete("Bạn có chắc muốn xóa " + selected.size() + " giao dịch đã chọn?")) {
            return;
        }
        for (Transaction tx : selected) {
            transactionService.delete(tx.getId(), currentUserId.getAsInt());
        }
        reloadTransactions();
        applyTransactionFilter();
        clearForm(type, amount, title, note, datePicker);
        notifyChanged();
    }

    private void reloadTransactions() {
        allTransactions = transactionService.list(currentUserId.getAsInt());
        if (transactionData == null) {
            transactionData = FXCollections.observableArrayList(allTransactions);
            transactionTable.setItems(transactionData);
        } else {
            transactionData.setAll(allTransactions);
        }
        resetCheckedMap(transactionCheckedMap, allTransactions);
        updateBulkDeleteButton();
    }

    private void refreshTransactionFilterCombos() {
        if (txWalletFilterBox == null) {
            return;
        }
        Wallet allWallet = new Wallet();
        allWallet.setId(0);
        allWallet.setName("TẤT CẢ");
        allWallet.setType(WalletType.CASH);

        List<Wallet> wallets = new ArrayList<>();
        wallets.add(allWallet);
        wallets.addAll(walletService.listByUser(currentUserId.getAsInt()));
        txWalletFilterBox.setItems(FXCollections.observableArrayList(wallets));
        txWalletFilterBox.getSelectionModel().selectFirst();
    }

    private void applyTransactionFilter() {
        if (transactionData == null || txWalletFilterBox == null || txTypeFilterBox == null
                || txSortFilterBox == null) {
            return;
        }
        String typeFilter = txTypeFilterBox != null ? txTypeFilterBox.getValue() : "TẤT CẢ";
        Wallet walletFilter = txWalletFilterBox != null ? txWalletFilterBox.getValue() : null;
        String sortFilter = txSortFilterBox != null ? txSortFilterBox.getValue() : "Ngày giảm dần";

        Comparator<Transaction> comparator;
        if ("Ngày tăng dần".equals(sortFilter)) {
            comparator = Comparator.comparing(Transaction::getTransactionDate)
                    .thenComparing(Transaction::getAmount, Comparator.nullsLast(Comparator.naturalOrder()));
        } else if ("Số tiền giảm dần".equals(sortFilter)) {
            comparator = Comparator.comparing(Transaction::getAmount, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Transaction::getTransactionDate, Comparator.nullsLast(Comparator.reverseOrder()));
        } else if ("Số tiền tăng dần".equals(sortFilter)) {
            comparator = Comparator.comparing(Transaction::getAmount, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Transaction::getTransactionDate, Comparator.nullsLast(Comparator.naturalOrder()));
        } else {
            comparator = Comparator
                    .comparing(Transaction::getTransactionDate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Transaction::getAmount, Comparator.nullsLast(Comparator.reverseOrder()));
        }

        List<Transaction> filtered = allTransactions.stream()
                .filter(t -> {
                    return switch (typeFilter) {
                        case "Thu nhập" -> t.getType() == TransactionType.INCOME;
                        case "Chi tiêu" -> t.getType() == TransactionType.EXPENSE;
                        default -> true;
                    };
                })
                .filter(t -> walletFilter == null || walletFilter.getId() == 0
                        || t.getWalletId() == walletFilter.getId())
                .sorted(comparator)
                .toList();
        transactionData.setAll(filtered);
        resetCheckedMap(transactionCheckedMap, filtered);
        updateBulkDeleteButton();
    }

    private void refreshWalletAndCategory() {
        if (txWalletBox == null || txCategoryBox == null) {
            // UI chưa được build; bỏ qua
            return;
        }
        List<Wallet> wallets = walletService.listByUser(currentUserId.getAsInt());
        walletNameMap = wallets.stream().collect(Collectors.toMap(Wallet::getId, Wallet::getName));
        txWalletBox.setItems(FXCollections.observableArrayList(wallets));
        if (!wallets.isEmpty()) {
            txWalletBox.getSelectionModel().select(0);
        }

        List<Category> categories = categoryService.list(currentUserId.getAsInt());
        categoryNameMap = categories.stream().collect(Collectors.toMap(Category::getId, Category::getName));
        txCategoryBox.setItems(FXCollections.observableArrayList(categories));
        if (!categories.isEmpty()) {
            txCategoryBox.getSelectionModel().select(0);
        }

        refreshTransactionFilterCombos();
    }

    private void selectWallet(ComboBox<Wallet> walletBox, int walletId) {
        walletBox.getItems().stream().filter(w -> w.getId() == walletId)
                .findFirst()
                .ifPresent(w -> walletBox.getSelectionModel().select(w));
    }

    private void selectCategory(ComboBox<Category> categoryBox, int categoryId) {
        categoryBox.getItems().stream().filter(c -> c.getId() == categoryId)
                .findFirst()
                .ifPresent(c -> categoryBox.getSelectionModel().select(c));
    }

    private void clearForm(ComboBox<TransactionType> type, TextField amount, TextField title, TextField note,
            DatePicker datePicker) {
        amount.clear();
        title.clear();
        note.clear();
        datePicker.setValue(LocalDate.now());
        type.getSelectionModel().select(TransactionType.EXPENSE);
        if (!txWalletBox.getItems().isEmpty())
            txWalletBox.getSelectionModel().selectFirst();
        if (!txCategoryBox.getItems().isEmpty())
            txCategoryBox.getSelectionModel().selectFirst();
        transactionTable.getSelectionModel().clearSelection();
    }

    private <T> List<T> checkedItems(Map<T, BooleanProperty> checkedMap) {
        return checkedMap.entrySet().stream()
                .filter(e -> e.getValue().get())
                .map(Map.Entry::getKey)
                .toList();
    }

    private <T> void resetCheckedMap(Map<T, BooleanProperty> checkedMap, List<T> items) {
        checkedMap.clear();
        for (T item : items) {
            BooleanProperty p = new SimpleBooleanProperty(false);
            p.addListener((o, ov, nv) -> updateBulkDeleteButton());
            checkedMap.put(item, p);
        }
    }

    private void updateBulkDeleteButton() {
        boolean hasSelected = transactionCheckedMap.values().stream().anyMatch(BooleanProperty::get);
        transactionDeleteAllBtn.setManaged(hasSelected);
        transactionDeleteAllBtn.setVisible(hasSelected);
    }

    private void notifyChanged() {
        if (onTransactionChanged != null) {
            onTransactionChanged.run();
        }
    }
}
