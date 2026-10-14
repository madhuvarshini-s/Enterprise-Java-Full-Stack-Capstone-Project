package com.apex.banking.dto.request;

import com.apex.banking.entity.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AccountCreateRequest {

    @NotNull(message = "Account type is required (CHECKING, SAVINGS, BUSINESS)")
    private AccountType accountType;

    @DecimalMin(value = "0.00", message = "Initial deposit cannot be negative")
    private BigDecimal initialDeposit = BigDecimal.ZERO;

    public AccountCreateRequest() {
    }

    public AccountCreateRequest(AccountType accountType, BigDecimal initialDeposit) {
        this.accountType = accountType;
        this.initialDeposit = initialDeposit;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getInitialDeposit() {
        return initialDeposit;
    }

    public void setInitialDeposit(BigDecimal initialDeposit) {
        this.initialDeposit = initialDeposit;
    }
}
