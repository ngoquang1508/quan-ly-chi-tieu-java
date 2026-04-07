package com.expensemanager.controller.ui;

import com.expensemanager.model.Transaction;
import com.expensemanager.model.Wallet;
import com.expensemanager.repository.TransactionRepository.TopCategory;
import com.expensemanager.service.CategoryService;
import com.expensemanager.service.ReportService;
import com.expensemanager.service.TransactionService;
import com.expensemanager.service.WalletService;
import com.expensemanager.view.UiHelper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class ReportController {

    private final ReportService reportService;
    private final TransactionService transactionService;
    private final WalletService walletService;
    private final CategoryService categoryService;
    private final UiHelper ui;
    private final Stage primaryStage;
    private final IntSupplier currentUserId;

    private TableView<Transaction> reportTxTable;
    private ObservableList<Transaction> reportTxData;
    private Label incomeLabel;
    private Label expenseLabel;
    private Label netLabel;
    private Label topCatLabel;
    private Label totalWalletLabel;
    private ComboBox<Integer> reportMonthBox;
    private ComboBox<Integer> reportYearBox;
    private Map<Integer, String> walletNameMap = new HashMap<>();
    private Map<Integer, String> categoryNameMap = new HashMap<>();

    public ReportController(ReportService reportService,
                            TransactionService transactionService,
                            WalletService walletService,
                            CategoryService categoryService,
                            UiHelper ui,
                            Stage primaryStage,
                            IntSupplier currentUserId) {
        this.reportService = reportService;
        this.transactionService = transactionService;
        this.walletService = walletService;
        this.categoryService = categoryService;
        this.ui = ui;
        this.primaryStage = primaryStage;
        this.currentUserId = currentUserId;
    }

    public Tab buildTab() {
        List<Integer> monthOptions = new ArrayList<>();
        monthOptions.add(null);
        monthOptions.addAll(java.util.stream.IntStream.rangeClosed(1, 12).boxed().toList());

        reportMonthBox = new ComboBox<>(FXCollections.observableArrayList(monthOptions));
        reportMonthBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer object) {
                return object == null ? "" : String.valueOf(object);
            }

            @Override
            public Integer fromString(String string) {
                if (string == null || string.isBlank()) {
                    return null;
                }
                return Integer.parseInt(string.trim());
            }
        });
        reportMonthBox.getSelectionModel().select(Integer.valueOf(LocalDate.now().getMonthValue()));

        reportYearBox = new ComboBox<>(FXCollections.observableArrayList(availableReportYears()));
        if (reportYearBox.getItems().isEmpty()) {
            reportYearBox.getItems().add(LocalDate.now().getYear());
        }
        reportYearBox.getSelectionModel().selectFirst();

        incomeLabel = new Label();
        expenseLabel = new Label();
        netLabel = new Label();
        topCatLabel = new Label();
        totalWalletLabel = new Label();
        incomeLabel.getStyleClass().add("label-card");
        expenseLabel.getStyleClass().add("label-card");
        netLabel.getStyleClass().add("label-card");
        topCatLabel.getStyleClass().add("label-card");
        totalWalletLabel.getStyleClass().add("label-card");

        reportTxTable = new TableView<>();
        reportTxTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        reportTxTable.getColumns().addAll(
                ui.column("Ngày", "transactionDate"),
                ui.txTypeColumn("Loại"),
                ui.txAmountColumn("Số tiền"),
                ui.mappingColumn("Ví", Transaction::getWalletId, () -> walletNameMap, "Ví #"),
                ui.mappingColumn("Danh mục", Transaction::getCategoryId, () -> categoryNameMap, "DM #"),
                ui.column("Tiêu đề", "title")
        );
        reportTxData = FXCollections.observableArrayList();
        reportTxTable.setItems(reportTxData);

        incomeLabel.setStyle("-fx-font-weight: 700;");
        expenseLabel.setStyle("-fx-font-weight: 700;");
        netLabel.setStyle("-fx-font-weight: 700;");

        reportMonthBox.valueProperty().addListener((obs, oldV, newV) -> refresh());
        reportYearBox.valueProperty().addListener((obs, oldV, newV) -> refresh());

        VBox incomeCard = new VBox(4, new Label("Tổng Thu nhập"), incomeLabel);
        incomeCard.setStyle("-fx-background-color: #E9FBEF; -fx-border-color: #4ADE80; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8;");

        VBox expenseCard = new VBox(4, new Label("Tổng Chi tiêu"), expenseLabel);
        expenseCard.setStyle("-fx-background-color: #FEECEC; -fx-border-color: #F87171; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8;");

        VBox netCard = new VBox(4, new Label("Số dư tháng"), netLabel);
        netCard.setStyle("-fx-background-color: #EEF5FF; -fx-border-color: #60A5FA; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8;");

        Label topCatTitle = new Label("DANH MỤC chi nhiều nhất");
        topCatTitle.setMinWidth(140);
        Label walletTotalTitle = new Label("Tổng số dư VÍ");
        walletTotalTitle.setMinWidth(140);
        Button exportMonthBtn = new Button("Xuất CSV tháng");
        exportMonthBtn.setOnAction(e -> exportReportCsv(true));
        Button exportYearBtn = new Button("Xuất CSV Năm");
        exportYearBtn.setOnAction(e -> exportReportCsv(false));

        VBox box = new VBox(10,
                new HBox(8, new Label("Tháng"), reportMonthBox, new Label("Năm"), reportYearBox),
                new HBox(10, incomeCard, expenseCard, netCard),
                new HBox(10, topCatTitle, topCatLabel),
                new HBox(10, walletTotalTitle, totalWalletLabel),
                new HBox(8, exportMonthBtn, exportYearBtn),
                new Label("Giao dịch của khoảng đã chọn:"),
                reportTxTable
        );
        box.setPadding(new Insets(10));

        refresh();
        return new Tab("Thống kê", new BorderPane(box, null, null, null, null));
    }

    public void refresh() {
        walletNameMap = walletService.listByUser(currentUserId.getAsInt()).stream()
                .collect(Collectors.toMap(Wallet::getId, Wallet::getName));
        categoryNameMap = categoryService.list(currentUserId.getAsInt()).stream()
                .collect(Collectors.toMap(cat -> cat.getId(), cat -> cat.getName()));

        Integer month = reportMonthBox.getValue();
        int year = reportYearBox.getValue() != null ? reportYearBox.getValue() : LocalDate.now().getYear();

        BigDecimal income = month == null
                ? reportService.totalIncomeByYear(currentUserId.getAsInt(), year)
                : reportService.totalIncomeByMonth(currentUserId.getAsInt(), month, year);
        BigDecimal expense = month == null
                ? reportService.totalExpenseByYear(currentUserId.getAsInt(), year)
                : reportService.totalExpenseByMonth(currentUserId.getAsInt(), month, year);
        BigDecimal net = income.subtract(expense);

        incomeLabel.setText(ui.fmt(income));
        expenseLabel.setText(ui.fmt(expense));
        netLabel.setText(ui.fmt(net));
        netLabel.setStyle(net.compareTo(BigDecimal.ZERO) < 0
                ? "-fx-font-weight: 700; -fx-text-fill: #dc2626;"
                : "-fx-font-weight: 700; -fx-text-fill: #16a34a;");

        Optional<TopCategory> top = reportService.topExpenseCategory(currentUserId.getAsInt(), month, year);
        topCatLabel.setText(top.map(t -> t.name() + " (" + ui.fmt(t.total()) + ")").orElse("Chưa có dữ liệu"));
        totalWalletLabel.setText(ui.fmt(reportService.totalWalletBalance(currentUserId.getAsInt())));

        List<Transaction> filtered = transactionService.list(currentUserId.getAsInt()).stream()
                .filter(t -> t.getTransactionDate().getYear() == year)
                .filter(t -> month == null || t.getTransactionDate().getMonthValue() == month)
                .sorted(Comparator.comparing(Transaction::getTransactionDate).reversed())
                .toList();
        reportTxData.setAll(filtered);

        refreshYearOptions();
    }

    public void onWalletChanged() {
        refresh();
    }

    public void onCategoryChanged() {
        refresh();
    }

    public void onTransactionChanged() {
        refresh();
    }

    private List<Integer> availableReportYears() {
        List<Integer> years = transactionService.list(currentUserId.getAsInt()).stream()
                .map(t -> t.getTransactionDate().getYear())
                .distinct()
                .sorted((a, b) -> Integer.compare(b, a))
                .toList();
        if (years.isEmpty()) {
            years = List.of(LocalDate.now().getYear());
        }
        return years;
    }

    private void refreshYearOptions() {
        List<Integer> years = availableReportYears();
        Integer current = reportYearBox.getValue();
        reportYearBox.setItems(FXCollections.observableArrayList(years));
        if (current != null && years.contains(current)) {
            reportYearBox.getSelectionModel().select(current);
        } else {
            reportYearBox.getSelectionModel().selectFirst();
        }
    }

    private void exportReportCsv(boolean byMonth) {
        try {
            Integer month = reportMonthBox.getValue();
            if (byMonth && month == null) {
                ui.info("Vui lòng chọn tháng để xuất báo cáo tháng.");
                return;
            }
            int year = reportYearBox.getValue() != null ? reportYearBox.getValue() : LocalDate.now().getYear();
            List<Transaction> source = transactionService.list(currentUserId.getAsInt()).stream()
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
            chooser.setTitle(byMonth ? "Xuất báo cáo tháng" : "Xuất báo cáo Năm");
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
                ui.info("Xuất file thành công: " + file.getName());
            }
        } catch (IOException ex) {
            ui.error("Không xuất được file: " + ex.getMessage());
        }
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
