package com.apex.banking.repository;

import com.apex.banking.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE (t.sourceAccount.accountNumber = :accNumber OR t.targetAccount.accountNumber = :accNumber) ORDER BY t.timestamp DESC")
    List<Transaction> findByAccountNumberOrderByTimestampDesc(@Param("accNumber") String accNumber);

    @Query("SELECT t FROM Transaction t WHERE (t.sourceAccount.user.username = :username OR t.targetAccount.user.username = :username) ORDER BY t.timestamp DESC")
    List<Transaction> findByUserUsernameOrderByTimestampDesc(@Param("username") String username, Pageable pageable);

    List<Transaction> findAllByOrderByTimestampDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = 'COMPLETED'")
    BigDecimal calculateTotalVolume();
}
