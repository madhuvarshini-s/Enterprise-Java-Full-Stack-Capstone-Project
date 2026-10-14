package com.apex.banking.controller;

import com.apex.banking.dto.request.AccountCreateRequest;
import com.apex.banking.dto.response.AccountResponse;
import com.apex.banking.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Accounts", description = "Endpoints for customer bank account management and inquiries")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/my-accounts")
    @Operation(summary = "Retrieve all accounts belonging to the authenticated customer")
    public ResponseEntity<List<AccountResponse>> getMyAccounts(@AuthenticationPrincipal UserDetails userDetails) {
        List<AccountResponse> accounts = accountService.getUserAccounts(userDetails.getUsername());
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Retrieve detailed information for a specific account")
    public ResponseEntity<AccountResponse> getAccountByNumber(@PathVariable String accountNumber,
                                                              @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        AccountResponse account = accountService.getAccountByNumber(accountNumber, userDetails.getUsername(), isAdmin);
        return ResponseEntity.ok(account);
    }

    @PostMapping("/create")
    @Operation(summary = "Open an additional bank account (SAVINGS or BUSINESS)")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountCreateRequest request,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        AccountResponse account = accountService.createAccount(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @GetMapping("/recipient/lookup")
    @Operation(summary = "Lookup recipient account information prior to confirming a fund transfer")
    public ResponseEntity<AccountResponse> lookupRecipient(@RequestParam String accountNumber) {
        AccountResponse account = accountService.lookupRecipient(accountNumber);
        return ResponseEntity.ok(account);
    }
}
