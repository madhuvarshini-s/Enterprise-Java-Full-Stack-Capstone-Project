package com.apex.banking.repository;

import com.apex.banking.entity.AccountStatus;
import com.apex.banking.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    List<BankAccount> findByUserUsername(String username);

    List<BankAccount> findByUserId(Long userId);

    Boolean existsByAccountNumber(String accountNumber);

    long countByStatus(AccountStatus status);

    @Query("SELECT COALESCE(SUM(b.balance), 0) FROM BankAccount b WHERE b.status = 'ACTIVE'")
    BigDecimal calculateTotalActiveBalance();
}
