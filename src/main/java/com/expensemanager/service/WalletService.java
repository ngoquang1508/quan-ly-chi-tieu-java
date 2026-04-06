package com.expensemanager.service;

import com.expensemanager.model.Wallet;
import com.expensemanager.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Wallet create(int userId, String name, String type) {
        return create(userId, name, type, BigDecimal.ZERO);
    }

    public Wallet create(int userId, String name, String type, BigDecimal initialBalance) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setName(name);
        wallet.setBalance(initialBalance == null ? BigDecimal.ZERO : initialBalance);
        wallet.setType(Enum.valueOf(com.expensemanager.model.WalletType.class, type));
        return walletRepository.create(wallet);
    }

    public List<Wallet> listByUser(int userId) {
        return walletRepository.findAllByUser(userId);
    }

    public boolean update(int userId, int id, String name, String type) {
        Optional<Wallet> opt = walletRepository.findById(id, userId);
        if (opt.isEmpty()) {
            return false;
        }
        Wallet wallet = opt.get();
        wallet.setName(name);
        wallet.setType(Enum.valueOf(com.expensemanager.model.WalletType.class, type));
        return walletRepository.update(wallet);
    }

    public boolean delete(int userId, int id) {
        return walletRepository.delete(id, userId);
    }

    public Optional<Wallet> get(int id, int userId) {
        return walletRepository.findById(id, userId);
    }
}
