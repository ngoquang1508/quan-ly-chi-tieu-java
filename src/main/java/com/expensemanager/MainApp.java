package com.expensemanager;

import com.expensemanager.model.*;
import com.expensemanager.repository.*;
import com.expensemanager.service.*;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.Optional;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JavaFX entry point: màn hình login/đăng ký đơn giản, sau đó TabPane cho từng chức năng.
 */
public class MainApp extends Application {

    private final NumberFormat vndFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    private AuthService authService;
    private WalletService walletService;
    private CategoryService categoryService;
    private TransactionService transactionService;
    private BudgetService budgetService;
    private ReportService reportService;
    private NotificationService notificationService;

    private Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        initServices();
        this.primaryStage = stage;
        stage.setTitle("Expense Manager");
        showAuthScene();
    }

    private void initServices() {
        UserRepository userRepository = new UserRepository();
        WalletRepository walletRepository = new WalletRepository();
        CategoryRepository categoryRepository = new CategoryRepository();
        TransactionRepository transactionRepository = new TransactionRepository();
        BudgetRepository budgetRepository = new BudgetRepository();
        NotificationRepository notificationRepository = new NotificationRepository();

        authService = new AuthService(userRepository);
        walletService = new WalletService(walletRepository);
        categoryService = new CategoryService(categoryRepository);
        notificationService = new NotificationService(notificationRepository);
        budgetService = new BudgetService(budgetRepository);
        transactionService = new TransactionService(transactionRepository, budgetService, notificationService, walletRepository, categoryRepository);
        reportService = new ReportService(transactionRepository, walletRepository);
    }

    /* ---------- Scenes ---------- */

    private void showAuthScene() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabs.getTabs().add(new Tab("Đăng nhập", buildLoginPane()));
        tabs.getTabs().add(new Tab("Đăng ký", buildRegisterPane()));

        Scene scene = new Scene(tabs, 420, 320);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private GridPane buildLoginPane() {
        GridPane grid = baseGrid();
        TextField username = new TextField();
        PasswordField password = new PasswordField();
        Label message = new Label();

        Button loginBtn = new Button("Đăng nhập");
        loginBtn.setOnAction(e -> {
            boolean ok = authService.login(username.getText().trim(), password.getText());
            if (ok) {
                showDashboard();
            } else {
                message.setText("Sai username/password");
            }
        });

        grid.addRow(0, new Label("Username"), username);
        grid.addRow(1, new Label("Password"), password);
        grid.add(loginBtn, 1, 2);
        grid.add(message, 1, 3);
        return grid;
    }

    private GridPane buildRegisterPane() {
        GridPane grid = baseGrid();
        TextField username = new TextField();
        TextField email = new TextField();
        TextField fullName = new TextField();
        PasswordField password = new PasswordField();
        Label message = new Label();

        Button registerBtn = new Button("Đăng ký");
        registerBtn.setOnAction(e -> {
            boolean ok = authService.register(
                    username.getText().trim(),
                    password.getText(),
                    email.getText().trim(),
                    fullName.getText().trim());
            message.setText(ok ? "Đăng ký thành công, chuyển sang tab đăng nhập."
                    : "Username hoặc email đã tồn tại");
        });

        grid.addRow(0, new Label("Username"), username);
        grid.addRow(1, new Label("Email"), email);
        grid.addRow(2, new Label("Họ tên"), fullName);
        grid.addRow(3, new Label("Password"), password);
        grid.add(registerBtn, 1, 4);
        grid.add(message, 1, 5);
        return grid;
    }

    private void showDashboard() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab walletTab = new Tab("Ví", buildWalletTab());
        Tab categoryTab = new Tab("Danh mục", buildCategoryTab());
        Tab transactionTab = new Tab("Giao dịch", buildTransactionTab());
        Tab budgetTab = new Tab("Ngân sách", buildBudgetTab());
        Tab reportTab = new Tab("Thống kê", buildReportTab());
        Tab notificationTab = new Tab("Thông báo", buildNotificationTab());

        tabs.getTabs().addAll(walletTab, categoryTab, transactionTab, budgetTab, reportTab, notificationTab);

        Button logoutBtn = new Button("Đăng xuất");
        logoutBtn.setOnAction(e -> {
            authService.logout();
            showAuthScene();
        });

        BorderPane root = new BorderPane();
        Label title = new Label("Expense Manager");
        title.getStyleClass().add("title");
        HBox header = new HBox(12, title, new Label("Xin chào " + authService.getCurrentUser().getUsername()), logoutBtn);
        header.getStyleClass().add("header");
        root.setCenter(tabs);
        root.setTop(header);
        BorderPane.setMargin(header, new Insets(8));

        Scene scene = new Scene(root, 1000, 650);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(scene);

        // Auto refresh when switch tab to keep data mới nhất
        walletTab.setOnSelectionChanged(e -> {
            if (walletTab.isSelected()) refreshWalletTable();
        });
        categoryTab.setOnSelectionChanged(e -> {
            if (categoryTab.isSelected()) refreshCategoryTable();
        });
        transactionTab.setOnSelectionChanged(e -> {
            if (transactionTab.isSelected()) refreshTransactionTableAndCombos();
        });
        budgetTab.setOnSelectionChanged(e -> {
            if (budgetTab.isSelected()) refreshBudgetTable();
        });
        reportTab.setOnSelectionChanged(e -> {
            if (reportTab.isSelected()) refreshReportDefault();
        });
        notificationTab.setOnSelectionChanged(e -> {
            if (notificationTab.isSelected()) refreshNotificationTable();
        });
    }

    /* ---------- Wallet UI ---------- */

    private TableView<Wallet> walletTable;
    private ObservableList<Wallet> walletData;

    private BorderPane buildWalletTab() {
        walletTable = new TableView<>();
        walletTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        walletTable.getColumns().addAll(
                sttColumn(walletTable),
                column("Tên", "name"),
                column("Loại", "type"),
                moneyColumn("Số dư", "balance")
        );

        List<Wallet> initialWallets = walletService.listByUser(currentUserId());
        walletData = FXCollections.observableArrayList(initialWallets);
        walletNameMap = initialWallets.stream().collect(Collectors.toMap(Wallet::getId, Wallet::getName));
        walletTable.setItems(walletData);

        TextField name = new TextField();
        ComboBox<WalletType> type = new ComboBox<>(FXCollections.observableArrayList(WalletType.values()));
        type.getSelectionModel().select(WalletType.CASH);

        Button add = new Button("Thêm");
        add.setOnAction(e -> {
            try {
                Wallet w = walletService.create(currentUserId(), name.getText(), type.getValue().name());
                refreshWalletTable();
                walletTable.getSelectionModel().select(w);
            } catch (Exception ex) {
                alert("Không thêm được ví: " + ex.getMessage());
            }
        });

        Button update = new Button("Sửa");
        update.setOnAction(e -> {
            Wallet selected = walletTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                alert("Chọn ví để sửa");
                return;
            }
            boolean ok = walletService.update(currentUserId(), selected.getId(), name.getText(), type.getValue().name());
            if (!ok) {
                alert("Không thể cập nhật");
            }
            refreshWalletTable();
        });

        Button delete = new Button("Xóa");
        delete.setOnAction(e -> {
            Wallet selected = walletTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                alert("Chọn ví để xóa");
                return;
            }
            walletService.delete(currentUserId(), selected.getId());
            refreshWalletTable();
        });

        walletTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                name.setText(newV.getName());
                type.getSelectionModel().select(newV.getType());
            }
        });

        VBox form = new VBox(8,
                new HBox(6, new Label("Tên"), name),
                new HBox(6, new Label("Loại"), type),
                new HBox(6, add, update, delete)
        );
        form.setPadding(new Insets(8));

        BorderPane pane = new BorderPane();
        pane.setCenter(walletTable);
        pane.setBottom(form);
        return pane;
    }

    /* ---------- Category UI ---------- */

    private TableView<Category> categoryTable;
    private ObservableList<Category> categoryData;

    private BorderPane buildCategoryTab() {
        categoryTable = new TableView<>();
        categoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        categoryTable.getColumns().addAll(
                sttColumn(categoryTable),
                column("Tên", "name"),
                column("Loại", "type"),
                column("Icon", "icon")
        );
        List<Category> initialCategories = categoryService.list(currentUserId());
        categoryData = FXCollections.observableArrayList(initialCategories);
        categoryNameMap = initialCategories.stream().collect(Collectors.toMap(Category::getId, Category::getName));
        categoryTable.setItems(categoryData);

        TextField name = new TextField();
        ComboBox<CategoryType> type = new ComboBox<>(FXCollections.observableArrayList(CategoryType.values()));
        type.getSelectionModel().select(CategoryType.EXPENSE);
        TextField icon = new TextField();

        Button add = new Button("Thêm");
        add.setOnAction(e -> {
            try {
                categoryService.create(currentUserId(), name.getText(), type.getValue(), icon.getText());
                refreshCategoryTable();
            } catch (Exception ex) {
                alert("Không thêm được danh mục: " + ex.getMessage());
            }
        });

        Button update = new Button("Sửa");
        update.setOnAction(e -> {
            Category selected = categoryTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                alert("Chọn danh mục để sửa");
                return;
            }
            boolean ok = categoryService.update(currentUserId(), selected.getId(), name.getText(), type.getValue(), icon.getText());
            if (!ok) alert("Cập nhật thất bại");
            refreshCategoryTable();
        });

        Button delete = new Button("Xóa");
        delete.setOnAction(e -> {
            Category selected = categoryTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                alert("Chọn danh mục để xóa");
                return;
            }
            categoryService.delete(currentUserId(), selected.getId());
            refreshCategoryTable();
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
                new HBox(6, add, update, delete)
        );
        form.setPadding(new Insets(8));

        BorderPane pane = new BorderPane(categoryTable, null, null, form, null);
        return pane;
    }

    /* ---------- Transaction UI ---------- */

    private TableView<Transaction> transactionTable;
    private ObservableList<Transaction> transactionData;
    private List<Transaction> allTransactions = new ArrayList<>();
    private ComboBox<Wallet> txWalletBox;
    private ComboBox<Category> txCategoryBox;
    private ComboBox<String> txTypeFilterBox;
    private ComboBox<Wallet> txWalletFilterBox;
    private ComboBox<String> txSortFilterBox;

    private BorderPane buildTransactionTab() {
        transactionTable = new TableView<>();
        transactionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        transactionTable.getColumns().addAll(
                sttColumn(transactionTable),
                column("Ngày", "transactionDate"),
                column("Loại", "type"),
                moneyColumn("Số tiền", "amount"),
                mappingColumn("Ví", Transaction::getWalletId, () -> walletNameMap, "Ví #"),
                mappingColumn("Danh mục", Transaction::getCategoryId, () -> categoryNameMap, "DM #"),
                column("Tiêu đề", "title")
        );
        allTransactions = transactionService.list(currentUserId());
        transactionData = FXCollections.observableArrayList(allTransactions);
        transactionTable.setItems(transactionData);

        txWalletBox = new ComboBox<>();
        txCategoryBox = new ComboBox<>();
        refreshWalletAndCategory(txWalletBox, txCategoryBox);

        txTypeFilterBox = new ComboBox<>(FXCollections.observableArrayList("TẤT CẢ", "EXPENSE", "INCOME"));
        txTypeFilterBox.getSelectionModel().selectFirst();
        txWalletFilterBox = new ComboBox<>();
        txSortFilterBox = new ComboBox<>(FXCollections.observableArrayList(
                "Ngày giảm dần", "Ngày tăng dần", "Số tiền giảm dần", "Số tiền tăng dần"
        ));
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
        add.setOnAction(e -> {
            try {
                Transaction tx = new Transaction();
                tx.setUserId(currentUserId());
                tx.setWalletId(txWalletBox.getValue().getId());
                tx.setCategoryId(txCategoryBox.getValue().getId());
                tx.setType(type.getValue());
                tx.setAmount(new BigDecimal(amount.getText()));
                tx.setTitle(title.getText());
                tx.setNote(note.getText());
                tx.setTransactionDate(datePicker.getValue());
                transactionService.add(tx);
                refreshTransactionTableAndCombos();
                applyTransactionFilter();
            } catch (Exception ex) {
                alert("Không thêm được giao dịch: " + ex.getMessage());
            }
        });

        Button update = new Button("Sửa");
        update.setOnAction(e -> {
            Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                alert("Chọn giao dịch để sửa");
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
                transactionService.update(selected);
                refreshTransactionTableAndCombos();
                applyTransactionFilter();
            } catch (Exception ex) {
                alert("Không sửa được: " + ex.getMessage());
            }
        });

        Button delete = new Button("Xóa");
        delete.setOnAction(e -> {
            Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                alert("Chọn giao dịch để xóa");
                return;
            }
            transactionService.delete(selected.getId(), currentUserId());
            refreshTransactionTableAndCombos();
            applyTransactionFilter();
        });

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
                new HBox(6, add, update, delete)
        );
        form.setPadding(new Insets(8));

        HBox filterBar = new HBox(8,
                new Label("Lọc loại"), txTypeFilterBox,
                new Label("Ví"), txWalletFilterBox,
                new Label("Sắp xếp"), txSortFilterBox
        );
        filterBar.setPadding(new Insets(8));

        BorderPane pane = new BorderPane();
        pane.setTop(filterBar);
        pane.setCenter(transactionTable);
        pane.setBottom(form);
        applyTransactionFilter();
        return pane;
    }

    /* ---------- Budget UI ---------- */

    private TableView<Budget> budgetTable;
    private ObservableList<Budget> budgetData;

    private BorderPane buildBudgetTab() {
        budgetTable = new TableView<>();
        budgetTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Budget, String> catCol = new TableColumn<>("Danh mục");
        catCol.setCellValueFactory(c -> javafx.beans.binding.Bindings.createObjectBinding(() ->
                categoryNameMap != null ? categoryNameMap.getOrDefault(c.getValue().getCategoryId(), "#" + c.getValue().getCategoryId())
                        : String.valueOf(c.getValue().getCategoryId())));
        budgetTable.getColumns().addAll(
                sttColumn(budgetTable),
                catCol,
                column("Tháng", "month"),
                column("Năm", "year"),
                moneyColumn("Giới hạn", "amountLimit")
        );
        budgetData = FXCollections.observableArrayList(budgetService.list(currentUserId()));
        budgetTable.setItems(budgetData);

        ComboBox<Category> categoryBox = new ComboBox<>(FXCollections.observableArrayList(categoryService.list(currentUserId())));
        TextField month = new TextField("4");
        TextField year = new TextField(String.valueOf(LocalDate.now().getYear()));
        TextField amount = new TextField();

        Button addBudget = new Button("Thêm");
        addBudget.setOnAction(e -> {
            try {
                Category cat = categoryBox.getValue();
                Budget b = budgetService.create(currentUserId(), cat.getId(),
                        new BigDecimal(amount.getText()),
                        Integer.parseInt(month.getText()),
                        Integer.parseInt(year.getText()));
                refreshBudgetTable();
                budgetTable.getSelectionModel().select(b);
            } catch (Exception ex) {
                alert("Không thêm được ngân sách: " + ex.getMessage());
            }
        });

        Button updateBudget = new Button("Sửa");
        updateBudget.setOnAction(e -> {
            Budget selected = budgetTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                alert("Chọn ngân sách để sửa");
                return;
            }
            try {
                Category cat = categoryBox.getValue();
                boolean ok = budgetService.update(currentUserId(), selected.getId(), cat.getId(),
                        new BigDecimal(amount.getText()),
                        Integer.parseInt(month.getText()),
                        Integer.parseInt(year.getText()));
                if (!ok) {
                    alert("Cập nhật thất bại");
                }
                refreshBudgetTable();
            } catch (Exception ex) {
                alert("Không sửa được ngân sách: " + ex.getMessage());
            }
        });

        Button deleteBudget = new Button("Xóa");
        deleteBudget.setOnAction(e -> {
            Budget selected = budgetTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                alert("Chọn ngân sách để xóa");
                return;
            }
            boolean ok = budgetService.delete(currentUserId(), selected.getId());
            if (!ok) {
                alert("Xóa thất bại");
            }
            refreshBudgetTable();
        });

        budgetTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                categoryBox.getSelectionModel().select(categoryBox.getItems().stream()
                        .filter(c -> c.getId() == n.getCategoryId()).findFirst().orElse(null));
                month.setText(String.valueOf(n.getMonth()));
                year.setText(String.valueOf(n.getYear()));
                amount.setText(n.getAmountLimit().toPlainString());
            }
        });

        VBox form = new VBox(8,
                new HBox(6, new Label("Danh mục"), categoryBox),
                new HBox(6, new Label("Tháng"), month),
                new HBox(6, new Label("Năm"), year),
                new HBox(6, new Label("Giới hạn"), amount),
                new HBox(6, addBudget, updateBudget, deleteBudget)
        );
        form.setPadding(new Insets(8));

        BorderPane pane = new BorderPane();
        pane.setCenter(budgetTable);
        pane.setBottom(form);
        return pane;
    }

    /* ---------- Report UI ---------- */

    private BorderPane buildReportTab() {
        reportMonthBox = new ComboBox<>(FXCollections.observableArrayList(
                java.util.stream.IntStream.rangeClosed(1, 12).boxed().toList()));
        reportMonthBox.getSelectionModel().select(Integer.valueOf(LocalDate.now().getMonthValue()));

        reportYearBox = new ComboBox<>(FXCollections.observableArrayList(availableReportYears()));
        if (!reportYearBox.getItems().isEmpty()) {
            reportYearBox.getSelectionModel().selectFirst();
        } else {
            reportYearBox.getItems().add(LocalDate.now().getYear());
            reportYearBox.getSelectionModel().selectFirst();
        }

        monthlyLabel = new Label();
        yearlyLabel = new Label();
        dailyLabel = new Label();
        topCatLabel = new Label();
        totalWalletLabel = new Label();
        monthlyLabel.getStyleClass().add("label-card");
        yearlyLabel.getStyleClass().add("label-card");
        dailyLabel.getStyleClass().add("label-card");
        topCatLabel.getStyleClass().add("label-card");
        totalWalletLabel.getStyleClass().add("label-card");

        reportTxTable = new TableView<>();
        reportTxTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        reportTxTable.getColumns().addAll(
                column("Ngày", "transactionDate"),
                column("Loại", "type"),
                moneyColumn("Số tiền", "amount"),
                mappingColumn("Ví", Transaction::getWalletId, () -> walletNameMap, "Ví #"),
                mappingColumn("Danh mục", Transaction::getCategoryId, () -> categoryNameMap, "DM #"),
                column("Tiêu đề", "title")
        );
        reportTxData = FXCollections.observableArrayList();
        reportTxTable.setItems(reportTxData);

        monthlyLabel.setStyle("-fx-font-weight: 700;");
        yearlyLabel.setStyle("-fx-font-weight: 700;");
        dailyLabel.setStyle("-fx-font-weight: 700;");

        reportMonthBox.valueProperty().addListener((obs, oldV, newV) -> refreshReportByFilter());
        reportYearBox.valueProperty().addListener((obs, oldV, newV) -> refreshReportByFilter());

        VBox incomeCard = new VBox(4, new Label("Tổng Thu nhập"), monthlyLabel);
        incomeCard.setStyle("-fx-background-color: #E9FBEF; -fx-border-color: #4ADE80; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8;");

        VBox expenseCard = new VBox(4, new Label("Tổng Chi tiêu"), yearlyLabel);
        expenseCard.setStyle("-fx-background-color: #FEECEC; -fx-border-color: #F87171; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8;");

        VBox netCard = new VBox(4, new Label("Số dư thuần"), dailyLabel);
        netCard.setStyle("-fx-background-color: #EEF5FF; -fx-border-color: #60A5FA; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8;");

        Label topCatTitle = new Label("DM chi nhiều nhất");
        topCatTitle.setMinWidth(140);
        Label walletTotalTitle = new Label("Tổng số dư ví");
        walletTotalTitle.setMinWidth(140);
        Button exportMonthBtn = new Button("Xuất CSV tháng");
        exportMonthBtn.setOnAction(e -> exportReportCsv(true));
        Button exportYearBtn = new Button("Xuất CSV năm");
        exportYearBtn.setOnAction(e -> exportReportCsv(false));

        VBox box = new VBox(10,
                new HBox(8, new Label("Tháng"), reportMonthBox, new Label("Năm"), reportYearBox),
                new HBox(10, incomeCard, expenseCard, netCard),
                new HBox(10, topCatTitle, topCatLabel),
                new HBox(10, walletTotalTitle, totalWalletLabel),
                new HBox(8, exportMonthBtn, exportYearBtn),
                new Label("Giao dịch của tháng đã chọn:"),
                reportTxTable
        );
        box.setPadding(new Insets(10));

        refreshReportByFilter();
        return new BorderPane(box, null, null, null, null);
    }

    /* ---------- Notification UI ---------- */

    private TableView<Notification> notificationTable;
    private ObservableList<Notification> notificationData;

    private TableView<Transaction> reportTxTable;
    private ObservableList<Transaction> reportTxData;
    private Label monthlyLabel;
    private Label yearlyLabel;
    private Label dailyLabel;
    private Label topCatLabel;
    private Label totalWalletLabel;
    private ComboBox<Integer> reportMonthBox;
    private ComboBox<Integer> reportYearBox;

    private Map<Integer, String> walletNameMap;
    private Map<Integer, String> categoryNameMap;

    private BorderPane buildNotificationTab() {
        notificationTable = new TableView<>();
        notificationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        notificationTable.getColumns().addAll(
                sttColumn(notificationTable),
                column("Tiêu đề", "title"),
                column("Nội dung", "message"),
                column("Đã đọc", "read")
        );
        notificationData = FXCollections.observableArrayList(notificationService.list(currentUserId()));
        notificationTable.setItems(notificationData);

        Button mark = new Button("Đánh dấu đã đọc");
        mark.setOnAction(e -> {
            Notification n = notificationTable.getSelectionModel().getSelectedItem();
            if (n == null) {
                alert("Chọn thông báo");
                return;
            }
            notificationService.markAsRead(n.getId(), currentUserId());
            refreshNotificationTable();
        });

        BorderPane pane = new BorderPane();
        pane.setCenter(notificationTable);
        pane.setBottom(new HBox(8, mark));
        BorderPane.setMargin(pane.getBottom(), new Insets(8));
        return pane;
    }

    /* ---------- Helpers ---------- */

    private int currentUserId() {
        return authService.getCurrentUser().getId();
    }

    private <T> TableColumn<T, Object> column(String title, String property) {
        TableColumn<T, Object> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        return col;
    }

    private <T> TableColumn<T, Number> sttColumn(TableView<T> table) {
        TableColumn<T, Number> col = new TableColumn<>("STT");
        col.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(table.getItems().indexOf(cell.getValue()) + 1));
        col.setSortable(false);
        col.setMaxWidth(70);
        return col;
    }

    private <T> TableColumn<T, BigDecimal> moneyColumn(String title, String property) {
        TableColumn<T, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setCellFactory(moneyCellFactory());
        return col;
    }

    private <T> TableColumn<T, String> mappingColumn(String title,
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

    private <S> Callback<TableColumn<S, BigDecimal>, TableCell<S, BigDecimal>> moneyCellFactory() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : fmt(item));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        };
    }

    private GridPane baseGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        return grid;
    }

    private void alert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.showAndWait();
    }

    private String fmt(BigDecimal value) {
        return vndFormat.format(value);
    }

    private void refreshTransactionFilterCombos() {
        Wallet allWallet = new Wallet();
        allWallet.setId(0);
        allWallet.setName("Tất cả");
        allWallet.setType(WalletType.CASH);

        List<Wallet> wallets = new ArrayList<>();
        wallets.add(allWallet);
        wallets.addAll(walletService.listByUser(currentUserId()));
        txWalletFilterBox.setItems(FXCollections.observableArrayList(wallets));
        txWalletFilterBox.getSelectionModel().selectFirst();
    }

    private void applyTransactionFilter() {
        if (transactionData == null) {
            return;
        }
        String typeFilter = txTypeFilterBox != null ? txTypeFilterBox.getValue() : "TẤT CẢ";
        Wallet walletFilter = txWalletFilterBox != null ? txWalletFilterBox.getValue() : null;
        String sortFilter = txSortFilterBox != null ? txSortFilterBox.getValue() : "Ngày giảm dần";

        Comparator<Transaction> comparator = Comparator.comparing(Transaction::getTransactionDate).reversed();
        if ("Ngày tăng dần".equals(sortFilter)) {
            comparator = Comparator.comparing(Transaction::getTransactionDate);
        } else if ("Số tiền giảm dần".equals(sortFilter)) {
            comparator = Comparator.comparing(Transaction::getAmount).reversed();
        } else if ("Số tiền tăng dần".equals(sortFilter)) {
            comparator = Comparator.comparing(Transaction::getAmount);
        }

        List<Transaction> filtered = allTransactions.stream()
                .filter(t -> "TẤT CẢ".equals(typeFilter) || t.getType().name().equals(typeFilter))
                .filter(t -> walletFilter == null || walletFilter.getId() == 0 || t.getWalletId() == walletFilter.getId())
                .sorted(comparator.thenComparing(Transaction::getTransactionDate).reversed())
                .toList();
        transactionData.setAll(filtered);
    }

    private void exportReportCsv(boolean byMonth) {
        try {
            int month = reportMonthBox.getValue() != null ? reportMonthBox.getValue() : LocalDate.now().getMonthValue();
            int year = reportYearBox.getValue() != null ? reportYearBox.getValue() : LocalDate.now().getYear();
            List<Transaction> source = transactionService.list(currentUserId()).stream()
                    .filter(t -> byMonth ? (t.getTransactionDate().getMonthValue() == month && t.getTransactionDate().getYear() == year)
                            : (t.getTransactionDate().getYear() == year))
                    .sorted(Comparator.comparing(Transaction::getTransactionDate).reversed())
                    .toList();

            String period = byMonth ? String.format("%02d-%d", month, year) : String.valueOf(year);
            StringBuilder sb = new StringBuilder();
            sb.append("Period,").append(period).append("\n");
            sb.append("Date,Type,Amount,Wallet,Category,Title,Note\n");
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (Transaction t : source) {
                String walletName = walletNameMap.getOrDefault(t.getWalletId(), "Ví #" + t.getWalletId());
                String catName = categoryNameMap.getOrDefault(t.getCategoryId(), "DM #" + t.getCategoryId());
                sb.append(df.format(t.getTransactionDate())).append(",")
                        .append(t.getType()).append(",")
                        .append(t.getAmount()).append(",")
                        .append(csvEscape(walletName)).append(",")
                        .append(csvEscape(catName)).append(",")
                        .append(csvEscape(t.getTitle())).append(",")
                        .append(csvEscape(t.getNote()))
                        .append("\n");
            }

            FileChooser chooser = new FileChooser();
            chooser.setTitle(byMonth ? "Xuất báo cáo tháng" : "Xuất báo cáo năm");
            chooser.setInitialFileName(byMonth ? "bao_cao_thang_" + period + ".csv" : "bao_cao_nam_" + period + ".csv");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
            var file = chooser.showSaveDialog(primaryStage);
            if (file != null) {
                byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
                byte[] content = sb.toString().getBytes(StandardCharsets.UTF_8);
                byte[] output = new byte[bom.length + content.length];
                System.arraycopy(bom, 0, output, 0, bom.length);
                System.arraycopy(content, 0, output, bom.length, content.length);
                Files.write(Path.of(file.getAbsolutePath()), output);
                alert("Xuất file thành công: " + file.getName());
            }
        } catch (IOException ex) {
            alert("Không xuất được file: " + ex.getMessage());
        }
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private void refreshWalletAndCategory(ComboBox<Wallet> walletBox, ComboBox<Category> categoryBox) {
        List<Wallet> wallets = walletService.listByUser(currentUserId());
        walletBox.setItems(FXCollections.observableArrayList(wallets));
        if (!wallets.isEmpty()) walletBox.getSelectionModel().select(0);
        walletNameMap = wallets.stream().collect(Collectors.toMap(Wallet::getId, Wallet::getName));

        List<Category> categories = categoryService.list(currentUserId());
        categoryBox.setItems(FXCollections.observableArrayList(categories));
        if (!categories.isEmpty()) categoryBox.getSelectionModel().select(0);
        categoryNameMap = categories.stream().collect(Collectors.toMap(Category::getId, Category::getName));
    }

    private void selectWallet(ComboBox<Wallet> walletBox, int walletId) {
        Optional<Wallet> found = walletBox.getItems().stream().filter(w -> w.getId() == walletId).findFirst();
        found.ifPresent(w -> walletBox.getSelectionModel().select(w));
    }

    private void selectCategory(ComboBox<Category> categoryBox, int categoryId) {
        Optional<Category> found = categoryBox.getItems().stream().filter(c -> c.getId() == categoryId).findFirst();
        found.ifPresent(c -> categoryBox.getSelectionModel().select(c));
    }

    // ---------- Refresh helpers ----------
    private void refreshWalletTable() {
        if (walletData != null) {
            List<Wallet> wallets = walletService.listByUser(currentUserId());
            walletData.setAll(wallets);
            walletNameMap = wallets.stream().collect(Collectors.toMap(Wallet::getId, Wallet::getName));
        }
    }

    private void refreshCategoryTable() {
        if (categoryData != null) {
            List<Category> categories = categoryService.list(currentUserId());
            categoryData.setAll(categories);
            categoryNameMap = categories.stream().collect(Collectors.toMap(Category::getId, Category::getName));
        }
    }

    private void refreshTransactionTableAndCombos() {
        allTransactions = transactionService.list(currentUserId());
        if (transactionData != null) {
            transactionData.setAll(allTransactions);
        }
        if (txWalletBox != null && txCategoryBox != null) {
            refreshWalletAndCategory(txWalletBox, txCategoryBox);
        }
        if (txWalletFilterBox != null) {
            refreshTransactionFilterCombos();
        }
    }

    private void refreshBudgetTable() {
        if (budgetData != null) {
            budgetData.setAll(budgetService.list(currentUserId()));
        }
    }

    private void refreshNotificationTable() {
        if (notificationData != null) {
            notificationData.setAll(notificationService.list(currentUserId()));
        }
    }

    private void refreshReportDefault() {
        refreshReportByFilter();
    }

    private List<Integer> availableReportYears() {
        return transactionService.list(currentUserId()).stream()
                .map(t -> t.getTransactionDate().getYear())
                .distinct()
                .sorted((a, b) -> Integer.compare(b, a))
                .toList();
    }

    private void refreshReportByFilter() {
        if (reportMonthBox == null || reportYearBox == null) {
            return;
        }
        walletNameMap = walletService.listByUser(currentUserId()).stream()
                .collect(Collectors.toMap(Wallet::getId, Wallet::getName));
        categoryNameMap = categoryService.list(currentUserId()).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        int month = reportMonthBox.getValue() != null ? reportMonthBox.getValue() : LocalDate.now().getMonthValue();
        int year = reportYearBox.getValue() != null ? reportYearBox.getValue() : LocalDate.now().getYear();

        var incomeMonth = reportService.totalIncomeByMonth(currentUserId(), month, year);
        var expenseMonth = reportService.totalExpenseByMonth(currentUserId(), month, year);
        var netMonth = incomeMonth.subtract(expenseMonth);

        if (monthlyLabel != null) monthlyLabel.setText(fmt(incomeMonth));
        if (yearlyLabel != null) yearlyLabel.setText(fmt(expenseMonth));
        if (dailyLabel != null) {
            dailyLabel.setText(fmt(netMonth));
            dailyLabel.setStyle(netMonth.compareTo(BigDecimal.ZERO) < 0
                    ? "-fx-font-weight: 700; -fx-text-fill: #dc2626;"
                    : "-fx-font-weight: 700; -fx-text-fill: #16a34a;");
        }
        if (topCatLabel != null) {
            topCatLabel.setText(reportService.topExpenseCategory(currentUserId(), month, year)
                    .orElse("Chưa có dữ liệu"));
        }
        if (totalWalletLabel != null) {
            totalWalletLabel.setText(fmt(reportService.totalWalletBalance(currentUserId())));
        }

        if (reportTxData != null) {
            reportTxData.setAll(transactionService.list(currentUserId()).stream()
                    .filter(t -> t.getTransactionDate().getMonthValue() == month && t.getTransactionDate().getYear() == year)
                    .toList());
        }
    }

}
