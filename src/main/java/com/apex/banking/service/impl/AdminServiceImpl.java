package com.apex.banking.service.impl;

import com.apex.banking.dto.response.DashboardStatsResponse;
import com.apex.banking.entity.AccountStatus;
import com.apex.banking.repository.BankAccountRepository;
import com.apex.banking.repository.TransactionRepository;
import com.apex.banking.repository.UserRepository;
import com.apex.banking.service.AdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AdminServiceImpl implements AdminService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public AdminServiceImpl(BankAccountRepository bankAccountRepository,
                            UserRepository userRepository,
                            TransactionRepository transactionRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        BigDecimal totalLiquidity = bankAccountRepository.calculateTotalActiveBalance();
        if (totalLiquidity == null) {
            totalLiquidity = BigDecimal.ZERO;
        }

        long totalAccounts = bankAccountRepository.count();
        long activeAccounts = bankAccountRepository.countByStatus(AccountStatus.ACTIVE);
        long frozenAccounts = bankAccountRepository.countByStatus(AccountStatus.FROZEN);
        long totalUsers = userRepository.count();

        BigDecimal totalVolume = transactionRepository.calculateTotalVolume();
        if (totalVolume == null) {
            totalVolume = BigDecimal.ZERO;
        }

        return new DashboardStatsResponse(
                totalLiquidity,
                totalAccounts,
                activeAccounts,
                frozenAccounts,
                totalUsers,
                totalVolume
        );
    }
}
