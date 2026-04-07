package com.expensemanager.view;

import com.expensemanager.controller.ui.BudgetController;
import com.expensemanager.controller.ui.CategoryController;
import com.expensemanager.controller.ui.NotificationController;
import com.expensemanager.controller.ui.ReportController;
import com.expensemanager.controller.ui.TransactionController;
import com.expensemanager.controller.ui.WalletController;
import com.expensemanager.service.AuthService;
import com.expensemanager.service.BudgetService;
import com.expensemanager.service.CategoryService;
import com.expensemanager.service.NotificationService;
import com.expensemanager.service.ReportService;
import com.expensemanager.service.TransactionService;
import com.expensemanager.service.WalletService;
import java.util.function.IntSupplier;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class DashboardView {

    private final AuthService authService;
    private final WalletService walletService;
    private final CategoryService categoryService;
    private final TransactionService transactionService;
    private final BudgetService budgetService;
    private final ReportService reportService;
    private final NotificationService notificationService;
    private final javafx.stage.Stage primaryStage;
    private final Runnable onLogout;

    private final UiHelper ui;
    private final WalletController walletController;
    private final CategoryController categoryController;
    private final TransactionController transactionController;
    private final BudgetController budgetController;
    private final ReportController reportController;
    private final NotificationController notificationController;

    public DashboardView(AuthService authService,
                         WalletService walletService,
                         CategoryService categoryService,
                         BudgetService budgetService,
                         TransactionService transactionService,
                         ReportService reportService,
                         NotificationService notificationService,
                         javafx.stage.Stage primaryStage,
                         Runnable onLogout) {
        this.authService = authService;
        this.walletService = walletService;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
        this.budgetService = budgetService;
        this.reportService = reportService;
        this.notificationService = notificationService;
        this.primaryStage = primaryStage;
        this.onLogout = onLogout;

        this.ui = new UiHelper();
        IntSupplier currentUserId = () -> authService.getCurrentUser().getId();

        this.transactionController = new TransactionController(transactionService, walletService, categoryService, ui, currentUserId);
        this.budgetController = new BudgetController(budgetService, categoryService, ui, currentUserId);
        this.reportController = new ReportController(reportService, transactionService, walletService, categoryService, ui, primaryStage, currentUserId);
        this.notificationController = new NotificationController(notificationService, ui, currentUserId);
        this.walletController = new WalletController(walletService, ui, currentUserId, null);
        this.categoryController = new CategoryController(categoryService, ui, currentUserId, null);

        wireControllers();
    }

    private void wireControllers() {
        walletController.setOnWalletChanged(() -> {
            transactionController.onWalletChanged();
            reportController.onWalletChanged();
        });

        categoryController.setOnCategoryChanged(() -> {
            transactionController.onCategoryChanged();
            budgetController.onCategoryChanged();
            reportController.onCategoryChanged();
        });

        transactionController.setOnTransactionChanged(() -> {
            walletController.refresh();
            reportController.onTransactionChanged();
            notificationController.refresh();
        });
    }

    public Parent buildRoot() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("main-nav-tabs");

        Tab walletTab = walletController.buildTab();
        Tab categoryTab = categoryController.buildTab();
        Tab budgetTab = budgetController.buildTab();
        Tab transactionTab = transactionController.buildTab();
        Tab reportTab = reportController.buildTab();
        Tab notificationTab = notificationController.buildTab();

        tabs.getTabs().addAll(walletTab, categoryTab, budgetTab, transactionTab, reportTab, notificationTab);

        Button logoutBtn = new Button("Đăng xuất");
        logoutBtn.setOnAction(e -> {
            authService.logout();
            if (onLogout != null) onLogout.run();
        });

        BorderPane root = new BorderPane();
        Label title = new Label("Expense Manager");
        title.getStyleClass().add("title");
        Label greeting = new Label("Xin chào " + authService.getCurrentUser().getUsername());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox rightBox = new HBox(10, greeting, logoutBtn);
        rightBox.setStyle("-fx-alignment: CENTER-RIGHT;");
        HBox header = new HBox(12, title, spacer, rightBox);
        header.getStyleClass().add("header");
        root.setCenter(tabs);
        root.setTop(header);
        BorderPane.setMargin(header, new Insets(8));

        walletTab.setOnSelectionChanged(e -> { if (walletTab.isSelected()) walletController.refresh(); });
        categoryTab.setOnSelectionChanged(e -> { if (categoryTab.isSelected()) categoryController.refresh(); });
        transactionTab.setOnSelectionChanged(e -> { if (transactionTab.isSelected()) transactionController.refresh(); });
        budgetTab.setOnSelectionChanged(e -> { if (budgetTab.isSelected()) budgetController.refresh(); });
        reportTab.setOnSelectionChanged(e -> { if (reportTab.isSelected()) reportController.refresh(); });
        notificationTab.setOnSelectionChanged(e -> { if (notificationTab.isSelected()) notificationController.refresh(); });

        return root;
    }
}
