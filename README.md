# Expense Manager GUI (Java 17 + JavaFX + MySQL)

Ứng dụng quản lý chi tiêu với giao diện JavaFX, kết nối MySQL, gồm: đăng nhập/đăng ký, ví, danh mục, giao dịch, ngân sách, thống kê, thông báo (cảnh báo vượt ngân sách).

## 1. Chuẩn bị database
```sql
SOURCE sql/expense_manager.sql;   -- tạo bảng + trigger
SOURCE sql/sample_data.sql;       -- dữ liệu mẫu (user demo/123456)
```

## 2. Cấu hình kết nối
Chỉnh `src/main/resources/db.properties` hoặc đặt biến môi trường:
```
DB_URL=jdbc:mysql://localhost:3306/expense_manager?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USER=root
DB_PASSWORD=yourpassword
```

## 3. Build & chạy (GUI)
```bash
mvn -DskipTests package
java -jar target/expense-manager-gui-1.0-SNAPSHOT-jar-with-dependencies.jar
# hoặc chạy nhanh bằng plugin
mvn javafx:run
```

## 4. Tài khoản mẫu
- Username: `demo`
- Password: `123456`

## 5. Giao diện chính
- Tab Ví: thêm/sửa/xóa ví, xem số dư (trigger DB cập nhật). Tự refresh khi chuyển tab, hiển thị số tiền định dạng VND.
- Tab Danh mục: EXPENSE/INCOME.
- Tab Giao dịch: chọn ví + danh mục + loại, nhập số tiền, ngày; trigger DB tự cập nhật số dư, kiểm tra ngân sách. Combo ví/danh mục tự cập nhật khi quay lại tab. Mỗi giao dịch tạo thông báo kèm biến động số dư.
- Tab Ngân sách: đặt/sửa hạn mức theo danh mục / tháng / năm (chọn dòng để sửa), hiển thị tên danh mục.
- Tab Thống kê: lọc theo tháng/năm, hiển thị thu/chi/cân đối (VND), top danh mục chi, tổng số dư ví, bảng giao dịch của tháng đang chọn với tên ví/danh mục.
- Tab Thông báo: xem và đánh dấu đã đọc; danh sách tự cập nhật khi vào tab, nội dung gồm biến động số dư và cảnh báo vượt ngân sách.

## 6. Lưu ý
- Mọi truy vấn dùng PreparedStatement; password lưu SHA-256.
- Giao diện dùng JavaFX, style ở `src/main/resources/style.css` (có gradient, nút xanh, bo góc). Bạn có thể tùy chỉnh màu dễ dàng.
- Nếu gặp lỗi thiếu native JavaFX, dùng `mvn javafx:run` để plugin tải đúng JavaFX runtime.
