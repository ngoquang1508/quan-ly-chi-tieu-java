package com.expensemanager.service;

import com.expensemanager.model.User;
import com.expensemanager.repository.UserRepository;
import com.expensemanager.util.PasswordUtil;

import java.util.Optional;

public class AuthService {

    private final UserRepository userRepository;
    private User currentUser;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean register(String username, String password, String email, String fullName) {
        // validate input
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Tài khoản không hợp lệ");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Email sai định dạng");
        }

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Họ tên không hợp lệ");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không hợp lệ");
        }

        if (password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải >= 6 ký tự");
        }

        //
        if (userRepository.existsByUsernameOrEmail(username, email)) {
            return false;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.hashPassword(password));
        user.setEmail(email);
        user.setFullName(fullName);
        userRepository.save(user);
        return true;
    }

    public boolean login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Tài khoản không hợp lệ");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không hợp lệ");
        }
        
        String hash = PasswordUtil.hashPassword(password);
        Optional<User> user = userRepository.findByUsernameAndPassword(username, hash);
        user.ifPresent(value -> currentUser = value);
        return user.isPresent();
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }
}
