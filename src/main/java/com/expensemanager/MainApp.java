package com.expensemanager;

import com.expensemanager.repository.*;
import com.expensemanager.service.*;
import com.expensemanager.view.DashboardView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class MainApp extends Application {

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
        transactionService = new TransactionService(transactionRepository, budgetService, notificationService,
                walletRepository, categoryRepository);
        reportService = new ReportService(transactionRepository, walletRepository);
    }

    /* ---------- Scenes ---------- */

    private void showAuthScene() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabs.getTabs().add(new Tab("Đăng nhập", buildLoginPane()));
        tabs.getTabs().add(new Tab("Đăng ký", buildRegisterPane()));

        Scene scene = new Scene(tabs, 1000, 650);
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
            String user = username.getText().trim();
            String pass = password.getText().trim();

            if (user.isBlank()) {
                alert(AlertType.WARNING, "Vui lòng nhập tài khoản!");
                username.requestFocus();
                return;
            }

            if (pass.isBlank()) {
                alert(AlertType.WARNING, "Vui lòng nhập mật khẩu!");
                password.requestFocus();
                return;
            }

            try {
                boolean ok = authService.login(user, pass);
                if (ok) {
                    showDashboard();
                } else {
                    alert(AlertType.WARNING, "Tài khoản hoặc mật khẩu không đúng!");
                }
            } catch (IllegalArgumentException e1) {
                alert(AlertType.WARNING, e1.getMessage());
            } catch (Exception e1) {
                alert(AlertType.ERROR, "Lỗi hệ thống");
            }
        });

        grid.addRow(0, new Label("Tài khoản"), username);
        grid.addRow(1, new Label("Mật khẩu"), password);
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
            String user = username.getText().trim();
            String mail = email.getText().trim();
            String name = fullName.getText().trim();
            String pass = password.getText();

            // validate fields
            if (user.isBlank()) {
                alert(AlertType.WARNING, "Vui lòng nhập tài khoản!");
                username.requestFocus();
                return;
            }

            if (user.length() < 3) {
                alert(AlertType.WARNING, "Tài khoản phải có ít nhất 3 kí tự!");
                username.requestFocus();
                return;
            }

            if (mail.isBlank()) {
                alert(AlertType.WARNING, "Vui lòng nhập email!");
                email.requestFocus();
                return;
            }

            if (!mail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                alert(AlertType.WARNING, "Email không hợp lệ!");
                email.requestFocus();
                return;
            }

            if (name.isBlank()) {
                alert(AlertType.WARNING, "Vui lòng nhập họ tên!");
                fullName.requestFocus();
                return;
            }

            if (pass.isBlank()) {
                alert(AlertType.WARNING, "Vui lòng nhập mật khẩu!");
                password.requestFocus();
                return;
            }

            if (pass.length() < 6) {
                alert(AlertType.WARNING, "Mật khẩu phải ít nhất 6 ký tự!");
                password.requestFocus();
                return;
            }

            try {
                boolean ok = authService.register(
                        username.getText().trim(),
                        password.getText(),
                        email.getText().trim(),
                        fullName.getText().trim());
                alert(AlertType.INFORMATION, ok ? "Đăng ký thành công, chuyển sang tab Đăng nhập để tiếp tục." : "Username hoặc email đã tồn tại");

                // clear fields sau khi đăng ký thành công
                username.clear();
                email.clear();
                fullName.clear();
                password.clear();
            } catch (IllegalArgumentException e1) {
                alert(AlertType.WARNING, e1.getMessage());
            } catch (Exception e1) {
                alert(AlertType.ERROR, "Lỗi hệ thống");
            }
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
        DashboardView view = new DashboardView(authService, walletService, categoryService,
                budgetService ,transactionService, reportService, notificationService, primaryStage,
                this::showAuthScene);
        Scene scene = new Scene(view.buildRoot(), 1000, 650);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(scene);
    }

    /* ---------- Helpers ---------- */
    private GridPane baseGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        return grid;
    }

    private void alert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.showAndWait();
    }
}
