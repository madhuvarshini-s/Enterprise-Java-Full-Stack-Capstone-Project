package com.apex.banking.service;

import com.apex.banking.dto.request.AccountCreateRequest;
import com.apex.banking.dto.request.AccountStatusUpdateRequest;
import com.apex.banking.dto.response.AccountResponse;

import java.util.List;

public interface AccountService {

    List<AccountResponse> getUserAccounts(String username);

    AccountResponse getAccountByNumber(String accountNumber, String currentUsername, boolean isAdmin);

    AccountResponse createAccount(AccountCreateRequest request, String username);

    AccountResponse lookupRecipient(String accountNumber);

    AccountResponse updateAccountStatus(String accountNumber, AccountStatusUpdateRequest request);

    List<AccountResponse> getAllAccounts();
}
