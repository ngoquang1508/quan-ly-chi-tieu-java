package com.expensemanager.controller.ui;

import com.expensemanager.model.Category;
import com.expensemanager.model.CategoryType;
import com.expensemanager.service.CategoryService;
import com.expensemanager.view.UiHelper;
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

public class CategoryController {

    private final CategoryService categoryService;
    private final UiHelper ui;
    private final IntSupplier currentUserId;
    private Runnable onCategoryChanged;

    private TableView<Category> categoryTable;
    private ObservableList<Category> categoryData;
    private final Map<Category, BooleanProperty> categoryCheckedMap = new HashMap<>();
    private Button categoryDeleteAllBtn;
    private Map<Integer, String> categoryNameMap = new HashMap<>();

    public CategoryController(CategoryService categoryService, UiHelper ui, IntSupplier currentUserId,
            Runnable onCategoryChanged) {
        this.categoryService = categoryService;
        this.ui = ui;
        this.currentUserId = currentUserId;
        this.onCategoryChanged = onCategoryChanged;
    }

    public Tab buildTab() {
        categoryTable = new TableView<>();
        categoryTable.setEditable(true);
        categoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        categoryDeleteAllBtn = new Button("Xóa");
        categoryDeleteAllBtn.setVisible(false);
        categoryDeleteAllBtn.setManaged(false);
        categoryTable.getColumns().addAll(
                ui.selectColumn(categoryTable, categoryCheckedMap, this::updateBulkDeleteButton),
                ui.sttColumn(categoryTable),
                ui.column("Tên", "name"),
                ui.column("Loại", "type"),
                ui.column("Icon", "icon"));

        refresh();

        TextField name = new TextField();
        ComboBox<CategoryType> type = new ComboBox<>(FXCollections.observableArrayList(CategoryType.values()));
        type.getSelectionModel().select(CategoryType.EXPENSE);
        TextField icon = new TextField();

        Button add = new Button("Thêm");
        add.setOnAction(e -> {
            try {
                String nameVal = name.getText();

                if (nameVal.isBlank()) {
                    throw new IllegalArgumentException("Vui lòng nhập tên danh mục!");
                }

                if (type.getValue() == null) {
                    throw new IllegalArgumentException("Vui lòng chọn loại danh mục!");
                }

                categoryService.create(
                        currentUserId.getAsInt(),
                        nameVal,
                        type.getValue(),
                        icon.getText());

                refresh();
                clearForm(name, icon, type);
                notifyChanged();

            } catch (IllegalArgumentException ex) {
                ui.warning(ex.getMessage());
            } catch (Exception ex) {
                ui.error("Không thêm được danh mục: " + ex.getMessage());
            }
        });

        Button update = new Button("Sửa");
        update.setOnAction(e -> {
            Category selected = categoryTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ui.info("Chọn danh mục để sửa");
                return;
            }
            boolean ok = categoryService.update(currentUserId.getAsInt(), selected.getId(), name.getText(),
                    type.getValue(), icon.getText());
            if (!ok)
                ui.error("Cập nhật thất bại");
            refresh();
            clearForm(name, icon, type);
            notifyChanged();
        });

        categoryDeleteAllBtn.setOnAction(e -> {
            List<Category> selected = categoryCheckedMap.entrySet().stream()
                    .filter(entry -> entry.getValue().get())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            if (selected.isEmpty())
                return;
            if (!ui.confirmDelete("Bạn có chắc muốn xóa " + selected.size() + " danh mục đã chọn?"))
                return;
            for (Category category : selected) {
                categoryService.delete(currentUserId.getAsInt(), category.getId());
            }
            refresh();
            clearForm(name, icon, type);
            notifyChanged();
        });

        categoryTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                name.setText(n.getName());
                type.getSelectionModel().select(n.getType());
                icon.setText(n.getIcon());
            }
        });

        VBox form = new VBox(8,
                new HBox(6, new Label("Tên"), name),
                new HBox(6, new Label("Loại"), type),
                new HBox(6, new Label("Icon"), icon),
                new HBox(6, add, update, categoryDeleteAllBtn));
        form.setPadding(new Insets(8));

        BorderPane pane = new BorderPane(categoryTable, null, null, form, null);
        return new Tab("Danh mục", pane);
    }

    public void refresh() {
        List<Category> categories = categoryService.list(currentUserId.getAsInt());
        categoryNameMap = categories.stream().collect(Collectors.toMap(Category::getId, Category::getName));
        if (categoryData == null) {
            categoryData = FXCollections.observableArrayList(categories);
            categoryTable.setItems(categoryData);
        } else {
            categoryData.setAll(categories);
        }
        categoryCheckedMap.clear();
        categories.forEach(c -> {
            BooleanProperty p = new SimpleBooleanProperty(false);
            p.addListener((o, ov, nv) -> updateBulkDeleteButton());
            categoryCheckedMap.put(c, p);
        });
        updateBulkDeleteButton();
    }

    public Map<Integer, String> categoryNameMap() {
        return categoryNameMap;
    }

    private void updateBulkDeleteButton() {
        boolean hasSelected = categoryCheckedMap.values().stream().anyMatch(BooleanProperty::get);
        categoryDeleteAllBtn.setManaged(hasSelected);
        categoryDeleteAllBtn.setVisible(hasSelected);
    }

    private void clearForm(TextField name, TextField icon, ComboBox<CategoryType> type) {
        name.clear();
        icon.clear();
        type.getSelectionModel().select(CategoryType.EXPENSE);
        categoryTable.getSelectionModel().clearSelection();
    }

    private void notifyChanged() {
        if (onCategoryChanged != null)
            onCategoryChanged.run();
    }

    public void setOnCategoryChanged(Runnable onCategoryChanged) {
        this.onCategoryChanged = onCategoryChanged;
    }
}
