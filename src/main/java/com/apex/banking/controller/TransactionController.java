package com.apex.banking.controller;

import com.apex.banking.dto.request.DepositRequest;
import com.apex.banking.dto.request.TransferRequest;
import com.apex.banking.dto.request.WithdrawRequest;
import com.apex.banking.dto.response.TransactionResponse;
import com.apex.banking.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Transactions", description = "Endpoints for funds deposit, withdrawal, transfers, and transaction history")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    @Operation(summary = "Deposit funds into a bank account")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        TransactionResponse response = transactionService.deposit(request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw funds from an account")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody WithdrawRequest request,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        TransactionResponse response = transactionService.withdraw(request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    @Operation(summary = "Execute atomic fund transfer between accounts with rollback protection")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        TransactionResponse response = transactionService.transfer(request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{accountNumber}")
    @Operation(summary = "Retrieve chronological transaction history for an account")
    public ResponseEntity<List<TransactionResponse>> getAccountHistory(@PathVariable String accountNumber,
                                                                       @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        List<TransactionResponse> history = transactionService.getAccountTransactionHistory(
                accountNumber, userDetails.getUsername(), isAdmin
        );
        return ResponseEntity.ok(history);
    }

    @GetMapping("/recent")
    @Operation(summary = "Retrieve recent transactions across all accounts of the logged-in user")
    public ResponseEntity<List<TransactionResponse>> getRecentTransactions(@AuthenticationPrincipal UserDetails userDetails,
                                                                           @RequestParam(defaultValue = "10") int limit) {
        List<TransactionResponse> transactions = transactionService.getRecentTransactions(userDetails.getUsername(), limit);
        return ResponseEntity.ok(transactions);
    }
}
