package com.apex.banking.controller;

import com.apex.banking.dto.request.AccountStatusUpdateRequest;
import com.apex.banking.dto.response.AccountResponse;
import com.apex.banking.dto.response.DashboardStatsResponse;
import com.apex.banking.dto.response.TransactionResponse;
import com.apex.banking.service.AccountService;
import com.apex.banking.service.AdminService;
import com.apex.banking.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration", description = "Endpoints for platform administrators: metrics, account freezes, and audit logs")
public class AdminController {

    private final AdminService adminService;
    private final AccountService accountService;
    private final TransactionService transactionService;

    public AdminController(AdminService adminService,
                           AccountService accountService,
                           TransactionService transactionService) {
        this.adminService = adminService;
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Retrieve real-time platform metrics and liquidity summary")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        DashboardStatsResponse stats = adminService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/accounts")
    @Operation(summary = "Retrieve all bank accounts across all system users")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        List<AccountResponse> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }

    @PutMapping("/accounts/{accountNumber}/status")
    @Operation(summary = "Update an account status (ACTIVE, FROZEN, CLOSED)")
    public ResponseEntity<AccountResponse> updateAccountStatus(@PathVariable String accountNumber,
                                                               @Valid @RequestBody AccountStatusUpdateRequest request) {
        AccountResponse response = accountService.updateAccountStatus(accountNumber, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    @Operation(summary = "Retrieve global transaction audit stream across the entire bank")
    public ResponseEntity<List<TransactionResponse>> getAllTransactions(@RequestParam(defaultValue = "50") int limit) {
        List<TransactionResponse> transactions = transactionService.getAllTransactions(limit);
        return ResponseEntity.ok(transactions);
    }
}
