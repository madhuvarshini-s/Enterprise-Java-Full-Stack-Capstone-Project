package com.apex.banking.service;

import com.apex.banking.dto.request.DepositRequest;
import com.apex.banking.dto.request.TransferRequest;
import com.apex.banking.dto.request.WithdrawRequest;
import com.apex.banking.dto.response.TransactionResponse;

import java.util.List;

public interface TransactionService {

    TransactionResponse deposit(DepositRequest request, String username);

    TransactionResponse withdraw(WithdrawRequest request, String username);

    TransactionResponse transfer(TransferRequest request, String username);

    List<TransactionResponse> getAccountTransactionHistory(String accountNumber, String currentUsername, boolean isAdmin);

    List<TransactionResponse> getRecentTransactions(String username, int limit);

    List<TransactionResponse> getAllTransactions(int limit);
}
