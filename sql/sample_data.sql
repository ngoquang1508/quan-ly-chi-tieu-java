USE expense_manager;

-- Người dùng demo (password = 123456, SHA-256)
INSERT INTO users (id, username, password, full_name, email) VALUES
(1, 'demo', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Demo User', 'demo@example.com');

-- Ví
INSERT INTO wallets (id, user_id, name, balance, type) VALUES
(1, 1, N'Tiền mặt', 0.00, 'CASH'),
(2, 1, N'Ngân hàng', 0.00, 'BANK');

-- Danh mục
INSERT INTO categories (id, user_id, name, type, icon) VALUES
(1, 1, N'Ăn uống', 'EXPENSE', '🍜'),
(2, 1, N'Lương', 'INCOME', '💼'),
(3, 1, N'Giải trí', 'EXPENSE', '🎮');

-- Ngân sách: Ăn uống tháng 4/2026 giới hạn 2,000,000
INSERT INTO budgets (id, user_id, category_id, amount_limit, month, year) VALUES
(1, 1, 1, 2000000, 4, 2026);

-- Giao dịch mẫu (trigger sẽ tự cập nhật số dư ví)
INSERT INTO transactions (id, user_id, wallet_id, category_id, amount, type, title, note, transaction_date) VALUES
(1, 1, 2, 2, 15000000, 'INCOME', 'Lương tháng 4', '', '2026-04-01'),
(2, 1, 1, 1, 500000, 'EXPENSE', 'Bữa trưa', '', '2026-04-02'),
(3, 1, 1, 3, 700000, 'EXPENSE', 'Đi xem phim', '', '2026-04-03'),
(4, 1, 2, 1, 1200000, 'EXPENSE', 'Ăn cùng gia đình', '', '2026-04-04'),
(5, 1, 1, 1, 700000, 'EXPENSE', 'Tiệc cuối tuần', '', '2026-04-05');

-- Đánh dấu tất cả notification là chưa đọc (sẽ tạo khi chạy app nếu vượt ngân sách)
