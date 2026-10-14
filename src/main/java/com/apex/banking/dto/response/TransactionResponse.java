package com.apex.banking.dto.response;

import com.apex.banking.entity.TransactionStatus;
import com.apex.banking.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {

    private Long id;
    private String referenceNumber;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal postBalance;
    private String description;
    private TransactionStatus status;
    private String sourceAccountNumber;
    private String targetAccountNumber;
    private LocalDateTime timestamp;

    public TransactionResponse() {
    }

    public TransactionResponse(Long id, String referenceNumber, TransactionType transactionType,
                               BigDecimal amount, BigDecimal postBalance, String description,
                               TransactionStatus status, String sourceAccountNumber,
                               String targetAccountNumber, LocalDateTime timestamp) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.postBalance = postBalance;
        this.description = description;
        this.status = status;
        this.sourceAccountNumber = sourceAccountNumber;
        this.targetAccountNumber = targetAccountNumber;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPostBalance() {
        return postBalance;
    }

    public void setPostBalance(BigDecimal postBalance) {
        this.postBalance = postBalance;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public void setSourceAccountNumber(String sourceAccountNumber) {
        this.sourceAccountNumber = sourceAccountNumber;
    }

    public String getTargetAccountNumber() {
        return targetAccountNumber;
    }

    public void setTargetAccountNumber(String targetAccountNumber) {
        this.targetAccountNumber = targetAccountNumber;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
