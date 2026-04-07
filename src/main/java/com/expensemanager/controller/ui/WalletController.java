package com.expensemanager.controller.ui;

import com.expensemanager.model.Wallet;
import com.expensemanager.model.WalletType;
import com.expensemanager.service.WalletService;
import com.expensemanager.view.UiHelper;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Tab;

public class WalletController {

    private final WalletService walletService;
    private final UiHelper ui;
    private final IntSupplier currentUserId;
    private Runnable onWalletChanged;

    private TableView<Wallet> walletTable;
    private ObservableList<Wallet> walletData;
    private final Map<Wallet, BooleanProperty> walletCheckedMap = new HashMap<>();
    private Button walletDeleteAllBtn;
    private Map<Integer, String> walletNameMap = new HashMap<>();

    public WalletController(WalletService walletService, UiHelper ui, IntSupplier currentUserId, Runnable onWalletChanged) {
        this.walletService = walletService;
        this.ui = ui;
        this.currentUserId = currentUserId;
        this.onWalletChanged = onWalletChanged;
    }

    public Tab buildTab() {
        walletTable = new TableView<>();
        walletTable.setEditable(true);
        walletTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        walletDeleteAllBtn = new Button("Xóa");
        walletDeleteAllBtn.setVisible(false);
        walletDeleteAllBtn.setManaged(false);
        walletTable.getColumns().addAll(
                ui.selectColumn(walletTable, walletCheckedMap, () -> updateBulkDeleteButton()),
                ui.sttColumn(walletTable),
                ui.column("Tên", "name"),
                ui.column("Loại", "type"),
                ui.moneyColumn("Số dư", "balance")
        );

        refreshTable();

        TextField name = new TextField();
        TextField initialBalance = new TextField();
        ComboBox<WalletType> type = new ComboBox<>(FXCollections.observableArrayList(WalletType.values()));
        type.getSelectionModel().select(WalletType.CASH);

        Button add = new Button("Thêm");
        add.setOnAction(e -> {
            try {
                BigDecimal balance = initialBalance.getText().isBlank() ? BigDecimal.ZERO : new BigDecimal(initialBalance.getText().trim());
                Wallet w = walletService.create(currentUserId.getAsInt(), name.getText(), type.getValue().name(), balance);
                refreshTable();
                walletTable.getSelectionModel().select(w);
                clearForm(name, initialBalance, type);
                notifyChanged();
            } catch (Exception ex) {
                ui.error("Không thêm được ví: " + ex.getMessage());
            }
        });

        Button update = new Button("Sửa");
        update.setOnAction(e -> {
            Wallet selected = walletTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ui.info("Chọn ví để sửa");
                return;
            }
            boolean ok = walletService.update(currentUserId.getAsInt(), selected.getId(), name.getText(), type.getValue().name());
            if (!ok) {
                ui.error("Không thể cập nhật");
            }
            refreshTable();
            clearForm(name, initialBalance, type);
            notifyChanged();
        });


        walletDeleteAllBtn.setOnAction(e -> {
            List<Wallet> selected = walletCheckedMap.entrySet().stream()
                    .filter(entry -> entry.getValue().get())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            if (selected.isEmpty()) {
                return;
            }
            if (!ui.confirmDelete("Bạn có chắc muốn xóa " + selected.size() + " ví đã chọn?")) {
                return;
            }
            for (Wallet wallet : selected) {
                walletService.delete(currentUserId.getAsInt(), wallet.getId());
            }
            refreshTable();
            clearForm(name, initialBalance, type);
            notifyChanged();
        });

        walletTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                name.setText(newV.getName());
                type.getSelectionModel().select(newV.getType());
            }
        });

        VBox form = new VBox(8,
                new HBox(6, new Label("Tên"), name),
                new HBox(6, new Label("Số dư ban đầu"), initialBalance),
                new HBox(6, new Label("Loại"), type),
                new HBox(6, add, update, walletDeleteAllBtn)
        );
        form.setPadding(new Insets(8));

        BorderPane pane = new BorderPane();
        pane.setCenter(walletTable);
        pane.setBottom(form);

        return new Tab("Ví", pane);
    }

    public void refresh() {
        refreshTable();
    }

    public Map<Integer, String> walletNameMap() {
        return walletNameMap;
    }

    private void refreshTable() {
        List<Wallet> wallets = walletService.listByUser(currentUserId.getAsInt());
        walletNameMap = wallets.stream().collect(Collectors.toMap(Wallet::getId, Wallet::getName));
        if (walletData == null) {
            walletData = FXCollections.observableArrayList(wallets);
            walletTable.setItems(walletData);
        } else {
            walletData.setAll(wallets);
        }
        walletCheckedMap.clear();
        wallets.forEach(w -> {
            BooleanProperty p = new SimpleBooleanProperty(false);
            p.addListener((o, ov, nv) -> updateBulkDeleteButton());
            walletCheckedMap.put(w, p);
        });
        updateBulkDeleteButton();
    }

    private void updateBulkDeleteButton() {
        boolean hasSelected = walletCheckedMap.values().stream().anyMatch(BooleanProperty::get);
        walletDeleteAllBtn.setManaged(hasSelected);
        walletDeleteAllBtn.setVisible(hasSelected);
    }

    private void clearForm(TextField name, TextField initialBalance, ComboBox<WalletType> type) {
        name.clear();
        initialBalance.clear();
        type.getSelectionModel().select(WalletType.CASH);
        walletTable.getSelectionModel().clearSelection();
    }

    private void notifyChanged() {
        if (onWalletChanged != null) {
            onWalletChanged.run();
        }
    }

    public void setOnWalletChanged(Runnable onWalletChanged) {
        this.onWalletChanged = onWalletChanged;
    }
}
