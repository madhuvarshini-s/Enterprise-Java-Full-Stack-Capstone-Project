package com.apex.banking;

import com.apex.banking.dto.request.AccountStatusUpdateRequest;
import com.apex.banking.dto.request.DepositRequest;
import com.apex.banking.dto.request.LoginRequest;
import com.apex.banking.dto.request.RegisterRequest;
import com.apex.banking.dto.request.TransferRequest;
import com.apex.banking.dto.request.WithdrawRequest;
import com.apex.banking.dto.response.AccountResponse;
import com.apex.banking.dto.response.DashboardStatsResponse;
import com.apex.banking.dto.response.JwtResponse;
import com.apex.banking.dto.response.TransactionResponse;
import com.apex.banking.entity.AccountStatus;
import com.apex.banking.exception.AccountFrozenException;
import com.apex.banking.exception.InsufficientBalanceException;
import com.apex.banking.exception.InvalidTransactionException;
import com.apex.banking.service.AccountService;
import com.apex.banking.service.AdminService;
import com.apex.banking.service.AuthService;
import com.apex.banking.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApexBankingApplicationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AdminService adminService;

    @Test
    @Order(1)
    @DisplayName("1. Verify Spring Context Loads Successfully")
    void contextLoads() {
        assertNotNull(authService, "AuthService should be injected");
        assertNotNull(accountService, "AccountService should be injected");
        assertNotNull(transactionService, "TransactionService should be injected");
        assertNotNull(adminService, "AdminService should be injected");
    }

    @Test
    @Order(2)
    @DisplayName("2. Test User Authentication and JWT Generation")
    void testAuthenticationAndJwt() {
        // Authenticate as seeded demo customer
        LoginRequest loginRequest = new LoginRequest("john_doe", "Password@123");
        JwtResponse jwtResponse = authService.login(loginRequest);

        assertNotNull(jwtResponse);
        assertNotNull(jwtResponse.getToken(), "JWT token should not be null");
        assertEquals("john_doe", jwtResponse.getUsername());
        assertTrue(jwtResponse.getRoles().contains("ROLE_CUSTOMER"));

        // Verify invalid credentials rejection
        LoginRequest badRequest = new LoginRequest("john_doe", "WrongPass999");
        assertThrows(BadCredentialsException.class, () -> authService.login(badRequest));
    }

    @Test
    @Order(3)
    @DisplayName("3. Test Customer Registration with Automatic Checking Account")
    void testRegistration() {
        String testUser = "test_customer_" + System.currentTimeMillis();
        RegisterRequest registerRequest = new RegisterRequest(
                testUser,
                testUser + "@apexbank.com",
                "Password@123",
                "Automated Test User",
                "+1-555-999-1234"
        );

        var regResponse = authService.register(registerRequest);
        assertTrue(regResponse.isSuccess());

        // Verify user can now log in
        JwtResponse loginRes = authService.login(new LoginRequest(testUser, "Password@123"));
        assertNotNull(loginRes.getToken());

        // Verify default checking account provisioned with $1,000 welcome credit
        List<AccountResponse> accounts = accountService.getUserAccounts(testUser);
        assertFalse(accounts.isEmpty());
        assertEquals(new BigDecimal("1000.00"), accounts.get(0).getBalance());
    }

    @Test
    @Order(4)
    @DisplayName("4. Test Deposit and Withdrawal with Balance Validation")
    void testDepositAndWithdrawal() {
        // Find John's checking account (APEX-10023481)
        List<AccountResponse> johnAccounts = accountService.getUserAccounts("john_doe");
        AccountResponse checking = johnAccounts.stream()
                .filter(a -> a.getAccountNumber().equals("APEX-10023481"))
                .findFirst()
                .orElseThrow();

        BigDecimal initialBalance = checking.getBalance();

        // 1. Deposit $500.00
        DepositRequest depReq = new DepositRequest("APEX-10023481", new BigDecimal("500.00"), "Test Deposit");
        TransactionResponse depTxn = transactionService.deposit(depReq, "john_doe");
        assertEquals(initialBalance.add(new BigDecimal("500.00")), depTxn.getPostBalance());

        // 2. Withdraw $200.00
        WithdrawRequest withReq = new WithdrawRequest("APEX-10023481", new BigDecimal("200.00"), "Test Withdrawal");
        TransactionResponse withTxn = transactionService.withdraw(withReq, "john_doe");
        assertEquals(initialBalance.add(new BigDecimal("300.00")), withTxn.getPostBalance());

        // 3. Attempt Overdraft (should fail with InsufficientBalanceException)
        WithdrawRequest overdraftReq = new WithdrawRequest("APEX-10023481", new BigDecimal("999999.00"), "Overdraft");
        assertThrows(InsufficientBalanceException.class, () -> transactionService.withdraw(overdraftReq, "john_doe"));
    }

    @Test
    @Order(5)
    @DisplayName("5. Test ACID Transactional Fund Transfer Between Accounts")
    void testTransactionalFundTransfer() {
        AccountResponse johnAccBefore = accountService.getAccountByNumber("APEX-10023481", "john_doe", true);
        AccountResponse janeAccBefore = accountService.getAccountByNumber("APEX-30078923", "jane_smith", true);

        BigDecimal transferAmount = new BigDecimal("250.00");

        TransferRequest transferRequest = new TransferRequest(
                "APEX-10023481",
                "APEX-30078923",
                transferAmount,
                "Consulting Fee"
        );

        TransactionResponse transferTxn = transactionService.transfer(transferRequest, "john_doe");
        assertNotNull(transferTxn.getReferenceNumber());
        assertEquals("COMPLETED", transferTxn.getStatus().name());

        // Verify balance updates
        AccountResponse johnAccAfter = accountService.getAccountByNumber("APEX-10023481", "john_doe", true);
        AccountResponse janeAccAfter = accountService.getAccountByNumber("APEX-30078923", "jane_smith", true);

        assertEquals(johnAccBefore.getBalance().subtract(transferAmount), johnAccAfter.getBalance());
        assertEquals(janeAccBefore.getBalance().add(transferAmount), janeAccAfter.getBalance());

        // Verify transfer to self is rejected
        TransferRequest selfTransfer = new TransferRequest("APEX-10023481", "APEX-10023481", new BigDecimal("100.00"), "Self");
        assertThrows(InvalidTransactionException.class, () -> transactionService.transfer(selfTransfer, "john_doe"));

        // Verify transfer to frozen account is rejected (APEX-40089104 is FROZEN)
        TransferRequest frozenTransfer = new TransferRequest("APEX-10023481", "APEX-40089104", new BigDecimal("100.00"), "To Frozen");
        assertThrows(AccountFrozenException.class, () -> transactionService.transfer(frozenTransfer, "john_doe"));
    }

    @Test
    @Order(6)
    @DisplayName("6. Test Admin Governance, Metrics and Account Freeze Toggle")
    void testAdminGovernance() {
        DashboardStatsResponse stats = adminService.getDashboardStats();
        assertNotNull(stats);
        assertTrue(stats.getTotalAccounts() >= 4);
        assertTrue(stats.getTotalLiquidity().compareTo(BigDecimal.ZERO) > 0);

        // Toggle account status to FROZEN
        AccountStatusUpdateRequest freezeReq = new AccountStatusUpdateRequest(AccountStatus.FROZEN);
        AccountResponse updated = accountService.updateAccountStatus("APEX-10023481", freezeReq);
        assertEquals(AccountStatus.FROZEN, updated.getStatus());

        // Trying to withdraw from frozen account should now fail
        WithdrawRequest req = new WithdrawRequest("APEX-10023481", new BigDecimal("50.00"), "Withdraw from frozen");
        assertThrows(AccountFrozenException.class, () -> transactionService.withdraw(req, "john_doe"));

        // Re-activate account
        AccountStatusUpdateRequest activeReq = new AccountStatusUpdateRequest(AccountStatus.ACTIVE);
        AccountResponse unfreeze = accountService.updateAccountStatus("APEX-10023481", activeReq);
        assertEquals(AccountStatus.ACTIVE, unfreeze.getStatus());
    }
}
