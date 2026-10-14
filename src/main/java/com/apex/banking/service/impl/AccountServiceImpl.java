package com.apex.banking.service.impl;

import com.apex.banking.dto.request.AccountCreateRequest;
import com.apex.banking.dto.request.AccountStatusUpdateRequest;
import com.apex.banking.dto.response.AccountResponse;
import com.apex.banking.entity.*;
import com.apex.banking.exception.AccountFrozenException;
import com.apex.banking.exception.ResourceNotFoundException;
import com.apex.banking.exception.UnauthorizedAccessException;
import com.apex.banking.repository.BankAccountRepository;
import com.apex.banking.repository.TransactionRepository;
import com.apex.banking.repository.UserRepository;
import com.apex.banking.service.AccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountServiceImpl implements AccountService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountServiceImpl(BankAccountRepository bankAccountRepository,
                              UserRepository userRepository,
                              TransactionRepository transactionRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getUserAccounts(String username) {
        return bankAccountRepository.findByUserUsername(username).stream()
                .map(this::mapToAccountResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber, String currentUsername, boolean isAdmin) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found: " + accountNumber));

        if (!isAdmin && !account.getUser().getUsername().equals(currentUsername)) {
            throw new UnauthorizedAccessException("You do not have permission to view account: " + accountNumber);
        }

        return mapToAccountResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse createAccount(AccountCreateRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        String accountNumber = generateUniqueAccountNumber();
        BigDecimal initialDeposit = request.getInitialDeposit() != null ? request.getInitialDeposit() : BigDecimal.ZERO;

        BankAccount account = new BankAccount(
                accountNumber,
                request.getAccountType(),
                initialDeposit,
                user
        );
        account.setStatus(AccountStatus.ACTIVE);
        BankAccount savedAccount = bankAccountRepository.save(account);

        if (initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
            Transaction initialTxn = new Transaction(
                    "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                    TransactionType.DEPOSIT,
                    initialDeposit,
                    initialDeposit,
                    "Initial Opening Deposit (" + request.getAccountType() + ")",
                    TransactionStatus.COMPLETED,
                    savedAccount,
                    savedAccount
            );
            initialTxn.setTimestamp(LocalDateTime.now());
            transactionRepository.save(initialTxn);
        }

        return mapToAccountResponse(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse lookupRecipient(String accountNumber) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient account not found: " + accountNumber));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException("Recipient account " + accountNumber + " is " + account.getStatus() + " and cannot receive funds.");
        }

        return mapToAccountResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse updateAccountStatus(String accountNumber, AccountStatusUpdateRequest request) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        account.setStatus(request.getStatus());
        BankAccount updatedAccount = bankAccountRepository.save(account);

        return mapToAccountResponse(updatedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return bankAccountRepository.findAll().stream()
                .map(this::mapToAccountResponse)
                .collect(Collectors.toList());
    }

    private AccountResponse mapToAccountResponse(BankAccount account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getStatus(),
                account.getUser().getUsername(),
                account.getUser().getFullName(),
                account.getCreatedAt()
        );
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            int num = 10000000 + secureRandom.nextInt(90000000);
            accountNumber = "APEX-" + num;
        } while (bankAccountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}
