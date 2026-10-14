package com.apex.banking.dto.request;

import com.apex.banking.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public class AccountStatusUpdateRequest {

    @NotNull(message = "Account status is required (ACTIVE, FROZEN, CLOSED)")
    private AccountStatus status;

    public AccountStatusUpdateRequest() {
    }

    public AccountStatusUpdateRequest(AccountStatus status) {
        this.status = status;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
