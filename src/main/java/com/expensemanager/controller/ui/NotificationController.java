package com.expensemanager.controller.ui;

import com.expensemanager.model.Notification;
import com.expensemanager.service.NotificationService;
import com.expensemanager.view.UiHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class NotificationController {

    private final NotificationService notificationService;
    private final UiHelper ui;
    private final IntSupplier currentUserId;

    private TableView<Notification> notificationTable;
    private ObservableList<Notification> notificationData;
    private final Map<Notification, BooleanProperty> notificationCheckedMap = new HashMap<>();
    private Button notificationDeleteAllBtn;

    public NotificationController(NotificationService notificationService, UiHelper ui, IntSupplier currentUserId) {
        this.notificationService = notificationService;
        this.ui = ui;
        this.currentUserId = currentUserId;
    }

    public Tab buildTab() {
        notificationTable = new TableView<>();
        notificationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        notificationDeleteAllBtn = new Button("Xóa");
        notificationDeleteAllBtn.setVisible(false);
        notificationDeleteAllBtn.setManaged(false);
        notificationTable.getColumns().addAll(
                ui.selectColumn(notificationTable, notificationCheckedMap, this::updateBulkDeleteButton),
                ui.sttColumn(notificationTable),
                ui.column("Tiêu đề", "title"),
                ui.column("Nội dung", "message"),
                ui.readStatusColumn("Đã đọc")
        );
        notificationData = FXCollections.observableArrayList();
        notificationTable.setItems(notificationData);

        Button mark = new Button("Đánh dấu Đã đọc");
        mark.setOnAction(e -> markAsRead());

        notificationDeleteAllBtn.setOnAction(e -> deleteSelected());

        BorderPane pane = new BorderPane();
        pane.setCenter(notificationTable);
        pane.setBottom(new HBox(8, mark, notificationDeleteAllBtn));
        BorderPane.setMargin(pane.getBottom(), new Insets(8));

        refresh();
        return new Tab("Thông báo", pane);
    }

    public void refresh() {
        List<Notification> notifications = notificationService.list(currentUserId.getAsInt());
        if (notificationData == null) {
            notificationData = FXCollections.observableArrayList(notifications);
            notificationTable.setItems(notificationData);
        } else {
            notificationData.setAll(notifications);
        }
        resetCheckedMap(notifications);
        updateBulkDeleteButton();
    }

    private void markAsRead() {
        Notification n = notificationTable.getSelectionModel().getSelectedItem();
        if (n == null) {
            ui.info("Chọn thông báo");
            return;
        }
        notificationService.markAsRead(n.getId(), currentUserId.getAsInt());
        refresh();
        notificationTable.getSelectionModel().clearSelection();
    }

    private void deleteSelected() {
        List<Notification> selected = checkedItems();
        if (selected.isEmpty()) {
            return;
        }
        if (!ui.confirmDelete("Bạn có chắc muốn xóa " + selected.size() + " thông báo đã chọn?")) {
            return;
        }
        for (Notification notification : selected) {
            notificationService.delete(notification.getId(), currentUserId.getAsInt());
        }
        refresh();
        notificationTable.getSelectionModel().clearSelection();
    }

    private void resetCheckedMap(List<Notification> items) {
        notificationCheckedMap.clear();
        for (Notification item : items) {
            BooleanProperty p = new SimpleBooleanProperty(false);
            p.addListener((o, ov, nv) -> updateBulkDeleteButton());
            notificationCheckedMap.put(item, p);
        }
    }

    private List<Notification> checkedItems() {
        return notificationCheckedMap.entrySet().stream()
                .filter(e -> e.getValue().get())
                .map(Map.Entry::getKey)
                .toList();
    }

    private void updateBulkDeleteButton() {
        boolean hasSelected = notificationCheckedMap.values().stream().anyMatch(BooleanProperty::get);
        notificationDeleteAllBtn.setManaged(hasSelected);
        notificationDeleteAllBtn.setVisible(hasSelected);
    }
}
