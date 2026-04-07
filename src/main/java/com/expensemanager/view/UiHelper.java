package com.expensemanager.view;

import com.expensemanager.model.Notification;
import com.expensemanager.model.Transaction;
import com.expensemanager.model.TransactionType;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

public class UiHelper {

    private final NumberFormat vndFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public void info(String msg) {
        alert(AlertType.INFORMATION, msg);
    }

    public void warning(String msg) {
        alert(AlertType.WARNING, msg);
    }

    public void error(String msg) {
        alert(AlertType.ERROR, msg);
    }

    public void alert(AlertType type, String msg) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.showAndWait();
    }

    public boolean confirmDelete(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText("Xác nhận xóa");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    public String fmt(BigDecimal value) {
        return vndFormat.format(value);
    }

    public <T> TableColumn<T, Object> column(String title, String property) {
        TableColumn<T, Object> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        return col;
    }

    public <T> TableColumn<T, Number> sttColumn(TableView<T> table) {
        TableColumn<T, Number> col = new TableColumn<>("STT");
        col.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(table.getItems().indexOf(cell.getValue()) + 1));
        col.setSortable(false);
        col.setMaxWidth(70);
        return col;
    }

    public <T> TableColumn<T, Boolean> selectColumn(TableView<T> table, Map<T, BooleanProperty> checkedMap, Runnable onChanged) {
        TableColumn<T, Boolean> col = new TableColumn<>();
        // allow per-row checkbox click
        table.setEditable(true);
        CheckBox checkAll = new CheckBox();
        checkAll.selectedProperty().addListener((obs, oldV, newV) -> {
            for (T item : table.getItems()) {
                checkedMap.computeIfAbsent(item, k -> new SimpleBooleanProperty(false)).set(newV);
            }
            onChanged.run();
            table.refresh();
        });
        col.setGraphic(checkAll);
        col.setCellValueFactory(param ->
                checkedMap.computeIfAbsent(param.getValue(), k -> {
                    BooleanProperty p = new SimpleBooleanProperty(false);
                    p.addListener((o, ov, nv) -> onChanged.run());
                    return p;
                }));
        col.setCellFactory(CheckBoxTableCell.forTableColumn(col));
        col.setEditable(true);
        col.setSortable(false);
        col.setReorderable(false);
        col.setPrefWidth(45);
        col.setMinWidth(35);
        col.setMaxWidth(55);
        return col;
    }

    public <T> TableColumn<T, BigDecimal> moneyColumn(String title, String property) {
        TableColumn<T, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setCellFactory(moneyCellFactory());
        return col;
    }

    public <T> TableColumn<T, String> mappingColumn(String title,
                                                    Function<T, Integer> idGetter,
                                                    java.util.function.Supplier<Map<Integer, String>> mapSupplier,
                                                    String fallbackPrefix) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createObjectBinding(() -> {
            Integer id = idGetter.apply(cell.getValue());
            Map<Integer, String> map = mapSupplier.get();
            if (map != null && id != null) {
                return map.getOrDefault(id, fallbackPrefix + id);
            }
            return id == null ? "" : fallbackPrefix + id;
        }));
        return col;
    }

    public <S> Callback<TableColumn<S, BigDecimal>, TableCell<S, BigDecimal>> moneyCellFactory() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : fmt(item));
                setStyle("-fx-alignment: CENTER-LEFT;");
            }
        };
    }

    public TableColumn<Transaction, TransactionType> txTypeColumn(String title) {
        TableColumn<Transaction, TransactionType> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>("type"));
        col.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(TransactionType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-alignment: CENTER-LEFT;");
                    return;
                }
                boolean income = item == TransactionType.INCOME;
                setText(income ? "Thu nhập" : "Chi tiêu");
                setStyle("-fx-alignment: CENTER-LEFT; -fx-font-weight: 700; -fx-text-fill: "
                        + (income ? "#16a34a;" : "#dc2626;"));
            }
        });
        return col;
    }

    public TableColumn<Transaction, BigDecimal> txAmountColumn(String title) {
        TableColumn<Transaction, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>("amount"));
        col.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                Transaction tx = getTableRow() == null ? null : (Transaction) getTableRow().getItem();
                if (empty || item == null || tx == null || tx.getType() == null) {
                    setText(null);
                    setStyle("-fx-alignment: CENTER-LEFT;");
                    return;
                }
                boolean income = tx.getType() == TransactionType.INCOME;
                String sign = income ? "+" : "-";
                setText(sign + fmtCompactVnd(item.abs()));
                setStyle("-fx-alignment: CENTER-LEFT; -fx-font-weight: 700; -fx-text-fill: "
                        + (income ? "#16a34a;" : "#dc2626;"));
            }
        });
        return col;
    }

    public TableColumn<Notification, Boolean> readStatusColumn(String title) {
        TableColumn<Notification, Boolean> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>("read"));
        col.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-alignment: CENTER;");
                    return;
                }
                if (item) {
                    setText("✓");
                    setStyle("-fx-alignment: CENTER; -fx-text-fill: #16a34a; -fx-font-weight: 700;");
                } else {
                    setText("•");
                    setStyle("-fx-alignment: CENTER; -fx-text-fill: #dc2626; -fx-font-weight: 900;");
                }
            }
        });
        col.setPrefWidth(60);
        col.setMinWidth(50);
        col.setMaxWidth(80);
        return col;
    }

    public String fmtCompactVnd(BigDecimal value) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.setMinimumFractionDigits(0);
        return numberFormat.format(value) + "\u0111";
    }
}
