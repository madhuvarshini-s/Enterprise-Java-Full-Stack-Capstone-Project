package com.apex.banking.service.impl;

import com.apex.banking.dto.request.DepositRequest;
import com.apex.banking.dto.request.TransferRequest;
import com.apex.banking.dto.request.WithdrawRequest;
import com.apex.banking.dto.response.TransactionResponse;
import com.apex.banking.entity.*;
import com.apex.banking.exception.*;
import com.apex.banking.repository.BankAccountRepository;
import com.apex.banking.repository.TransactionRepository;
import com.apex.banking.service.TransactionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(BankAccountRepository bankAccountRepository,
                                  TransactionRepository transactionRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public TransactionResponse deposit(DepositRequest request, String username) {
        BankAccount account = bankAccountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountNumber()));

        if (!account.getUser().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You are not authorized to deposit into account: " + request.getAccountNumber());
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException("Cannot deposit. Account " + request.getAccountNumber() + " is " + account.getStatus());
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Deposit amount must be greater than zero.");
        }

        BigDecimal updatedBalance = account.getBalance().add(request.getAmount());
        account.setBalance(updatedBalance);
        bankAccountRepository.save(account);

        String description = (request.getDescription() != null && !request.getDescription().trim().isEmpty())
                ? request.getDescription().trim()
                : "Funds Deposit";

        Transaction txn = new Transaction(
                generateReferenceNumber(),
                TransactionType.DEPOSIT,
                request.getAmount(),
                updatedBalance,
                description,
                TransactionStatus.COMPLETED,
                account,
                account
        );
        txn.setTimestamp(LocalDateTime.now());
        Transaction savedTxn = transactionRepository.save(txn);

        return mapToResponse(savedTxn);
    }

    @Override
    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request, String username) {
        BankAccount account = bankAccountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountNumber()));

        if (!account.getUser().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You are not authorized to withdraw from account: " + request.getAccountNumber());
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException("Cannot withdraw. Account " + request.getAccountNumber() + " is " + account.getStatus());
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Withdrawal amount must be greater than zero.");
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient funds in account " + request.getAccountNumber()
                    + ". Available: $" + account.getBalance() + ", Requested: $" + request.getAmount());
        }

        BigDecimal updatedBalance = account.getBalance().subtract(request.getAmount());
        account.setBalance(updatedBalance);
        bankAccountRepository.save(account);

        String description = (request.getDescription() != null && !request.getDescription().trim().isEmpty())
                ? request.getDescription().trim()
                : "Funds Withdrawal";

        Transaction txn = new Transaction(
                generateReferenceNumber(),
                TransactionType.WITHDRAWAL,
                request.getAmount(),
                updatedBalance,
                description,
                TransactionStatus.COMPLETED,
                account,
                account
        );
        txn.setTimestamp(LocalDateTime.now());
        Transaction savedTxn = transactionRepository.save(txn);

        return mapToResponse(savedTxn);
    }

    @Override
    @Transactional
    public TransactionResponse transfer(TransferRequest request, String username) {
        if (request.getSourceAccountNumber().trim().equalsIgnoreCase(request.getTargetAccountNumber().trim())) {
            throw new InvalidTransactionException("Source and destination accounts cannot be identical.");
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be greater than zero.");
        }

        BankAccount sourceAccount = bankAccountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found: " + request.getSourceAccountNumber()));

        if (!sourceAccount.getUser().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You are not authorized to transfer from account: " + request.getSourceAccountNumber());
        }

        if (sourceAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException("Source account " + request.getSourceAccountNumber() + " is " + sourceAccount.getStatus());
        }

        BankAccount targetAccount = bankAccountRepository.findByAccountNumber(request.getTargetAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Target recipient account not found: " + request.getTargetAccountNumber()));

        if (targetAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountFrozenException("Target recipient account " + request.getTargetAccountNumber() + " is " + targetAccount.getStatus());
        }

        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + sourceAccount.getAccountNumber()
                    + ". Available: $" + sourceAccount.getBalance() + ", Transfer: $" + request.getAmount());
        }

        // ACID Debit & Credit
        BigDecimal newSourceBalance = sourceAccount.getBalance().subtract(request.getAmount());
        sourceAccount.setBalance(newSourceBalance);

        BigDecimal newTargetBalance = targetAccount.getBalance().add(request.getAmount());
        targetAccount.setBalance(newTargetBalance);

        bankAccountRepository.save(sourceAccount);
        bankAccountRepository.save(targetAccount);

        String description = (request.getDescription() != null && !request.getDescription().trim().isEmpty())
                ? request.getDescription().trim()
                : "Transfer to " + targetAccount.getUser().getFullName() + " (" + targetAccount.getAccountNumber() + ")";

        Transaction txn = new Transaction(
                generateReferenceNumber(),
                TransactionType.TRANSFER,
                request.getAmount(),
                newSourceBalance,
                description,
                TransactionStatus.COMPLETED,
                sourceAccount,
                targetAccount
        );
        txn.setTimestamp(LocalDateTime.now());
        Transaction savedTxn = transactionRepository.save(txn);

        return mapToResponse(savedTxn);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getAccountTransactionHistory(String accountNumber, String currentUsername, boolean isAdmin) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        if (!isAdmin && !account.getUser().getUsername().equals(currentUsername)) {
            throw new UnauthorizedAccessException("You are not authorized to access transactions for this account.");
        }

        return transactionRepository.findByAccountNumberOrderByTimestampDesc(accountNumber).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecentTransactions(String username, int limit) {
        Pageable pageable = PageRequest.of(0, limit <= 0 ? 10 : limit);
        return transactionRepository.findByUserUsernameOrderByTimestampDesc(username, pageable).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllTransactions(int limit) {
        Pageable pageable = PageRequest.of(0, limit <= 0 ? 25 : limit);
        return transactionRepository.findAllByOrderByTimestampDesc(pageable).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TransactionResponse mapToResponse(Transaction txn) {
        String sourceAcc = txn.getSourceAccount() != null ? txn.getSourceAccount().getAccountNumber() : "EXTERNAL";
        String targetAcc = txn.getTargetAccount() != null ? txn.getTargetAccount().getAccountNumber() : "EXTERNAL";

        return new TransactionResponse(
                txn.getId(),
                txn.getReferenceNumber(),
                txn.getTransactionType(),
                txn.getAmount(),
                txn.getPostBalance(),
                txn.getDescription(),
                txn.getStatus(),
                sourceAcc,
                targetAcc,
                txn.getTimestamp()
        );
    }

    private String generateReferenceNumber() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
