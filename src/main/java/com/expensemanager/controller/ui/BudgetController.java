package com.expensemanager.controller.ui;

import com.expensemanager.model.Budget;
import com.expensemanager.model.Category;
import com.expensemanager.service.BudgetService;
import com.expensemanager.service.CategoryService;
import com.expensemanager.view.UiHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class BudgetController {

    private final BudgetService budgetService;
    private final CategoryService categoryService;
    private final UiHelper ui;
    private final IntSupplier currentUserId;

    private TableView<Budget> budgetTable;
    private ObservableList<Budget> budgetData;
    private final Map<Budget, BooleanProperty> budgetCheckedMap = new HashMap<>();
    private Button budgetDeleteAllBtn;
    private ComboBox<Category> budgetCategoryBox;
    private Map<Integer, String> categoryNameMap = new HashMap<>();

    public BudgetController(BudgetService budgetService, CategoryService categoryService, UiHelper ui, IntSupplier currentUserId) {
        this.budgetService = budgetService;
        this.categoryService = categoryService;
        this.ui = ui;
        this.currentUserId = currentUserId;
    }

    public Tab buildTab() {
        budgetTable = new TableView<>();
        budgetTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        budgetDeleteAllBtn = new Button("Xóa");
        budgetDeleteAllBtn.setVisible(false);
        budgetDeleteAllBtn.setManaged(false);

        TableColumn<Budget, String> catCol = new TableColumn<>("Danh mục");
        catCol.setCellValueFactory(c -> javafx.beans.binding.Bindings.createObjectBinding(() ->
                categoryNameMap.getOrDefault(c.getValue().getCategoryId(), "#" + c.getValue().getCategoryId())));

        budgetTable.getColumns().addAll(
                ui.selectColumn(budgetTable, budgetCheckedMap, this::updateBulkDeleteButton),
                ui.sttColumn(budgetTable),
                catCol,
                ui.column("Tháng", "month"),
                ui.column("Năm", "year"),
                ui.moneyColumn("Giới hạn", "amountLimit")
        );

        budgetData = FXCollections.observableArrayList();
        budgetTable.setItems(budgetData);

        budgetCategoryBox = new ComboBox<>();
        refreshCategories();

        TextField month = new TextField(String.valueOf(LocalDate.now().getMonthValue()));
        TextField year = new TextField(String.valueOf(LocalDate.now().getYear()));
        TextField amount = new TextField();

        Button addBudget = new Button("Thêm");
        addBudget.setOnAction(e -> addBudget(month, year, amount));

        Button updateBudget = new Button("Sửa");
        updateBudget.setOnAction(e -> updateBudget(month, year, amount));

        budgetDeleteAllBtn.setOnAction(e -> deleteBudgets(month, year, amount));

        budgetTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                selectCategory(n.getCategoryId());
                month.setText(String.valueOf(n.getMonth()));
                year.setText(String.valueOf(n.getYear()));
                amount.setText(n.getAmountLimit().toPlainString());
            }
        });

        VBox form = new VBox(8,
                new HBox(6, new Label("Danh mục"), budgetCategoryBox),
                new HBox(6, new Label("Tháng"), month),
                new HBox(6, new Label("Năm"), year),
                new HBox(6, new Label("Giới hạn"), amount),
                new HBox(6, addBudget, updateBudget, budgetDeleteAllBtn)
        );
        form.setPadding(new Insets(8));

        BorderPane pane = new BorderPane();
        pane.setCenter(budgetTable);
        pane.setBottom(form);

        refresh();

        return new Tab("Ngân sách", pane);
    }

    public void refresh() {
        List<Budget> budgets = budgetService.list(currentUserId.getAsInt());
        if (budgetData == null) {
            budgetData = FXCollections.observableArrayList(budgets);
            budgetTable.setItems(budgetData);
        } else {
            budgetData.setAll(budgets);
        }
        resetCheckedMap(budgetCheckedMap, budgets);
        updateBulkDeleteButton();
        refreshCategories();
    }

    public void onCategoryChanged() {
        refreshCategories();
        refresh();
    }

    private void addBudget(TextField month, TextField year, TextField amount) {
        try {
            Category cat = budgetCategoryBox.getValue();
            if (cat == null) {
                ui.info("Chọn danh mục trước khi thêm ngân sách");
                return;
            }
            Budget b = budgetService.create(currentUserId.getAsInt(), cat.getId(),
                    new BigDecimal(amount.getText()),
                    Integer.parseInt(month.getText()),
                    Integer.parseInt(year.getText()));
            refresh();
            budgetTable.getSelectionModel().select(b);
            clearForm(month, year, amount);
        } catch (Exception ex) {
            ui.error("Không thêm được ngân sách: " + ex.getMessage());
        }
    }

    private void updateBudget(TextField month, TextField year, TextField amount) {
        Budget selected = budgetTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            ui.info("Chọn ngân sách để sửa");
            return;
        }
        try {
            Category cat = budgetCategoryBox.getValue();
            if (cat == null) {
                ui.info("Chọn danh mục trước khi sửa ngân sách");
                return;
            }
            boolean ok = budgetService.update(currentUserId.getAsInt(), selected.getId(), cat.getId(),
                    new BigDecimal(amount.getText()),
                    Integer.parseInt(month.getText()),
                    Integer.parseInt(year.getText()));
            if (!ok) {
                ui.error("Cập nhật thất bại");
            }
            refresh();
            clearForm(month, year, amount);
        } catch (Exception ex) {
            ui.error("Không sửa được ngân sách: " + ex.getMessage());
        }
    }

    private void deleteBudgets(TextField month, TextField year, TextField amount) {
        List<Budget> selected = checkedItems(budgetCheckedMap);
        if (selected.isEmpty()) {
            return;
        }
        if (!ui.confirmDelete("Bạn có chắc muốn xóa " + selected.size() + " ngân sách đã chọn?")) {
            return;
        }
        for (Budget budget : selected) {
            budgetService.delete(currentUserId.getAsInt(), budget.getId());
        }
        refresh();
        clearForm(month, year, amount);
    }

    private void refreshCategories() {
        List<Category> categories = categoryService.list(currentUserId.getAsInt());
        categoryNameMap = categories.stream().collect(Collectors.toMap(Category::getId, Category::getName));
        budgetCategoryBox.setItems(FXCollections.observableArrayList(categories));
    }

    private void selectCategory(int categoryId) {
        budgetCategoryBox.getItems().stream()
                .filter(c -> c.getId() == categoryId)
                .findFirst()
                .ifPresent(c -> budgetCategoryBox.getSelectionModel().select(c));
    }

    private void clearForm(TextField month, TextField year, TextField amount) {
        budgetCategoryBox.getSelectionModel().clearSelection();
        month.clear();
        year.clear();
        amount.clear();
        budgetTable.getSelectionModel().clearSelection();
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
        boolean hasSelected = budgetCheckedMap.values().stream().anyMatch(BooleanProperty::get);
        budgetDeleteAllBtn.setManaged(hasSelected);
        budgetDeleteAllBtn.setVisible(hasSelected);
    }
}
