package com.apex.banking.config;

import com.apex.banking.entity.*;
import com.apex.banking.repository.BankAccountRepository;
import com.apex.banking.repository.TransactionRepository;
import com.apex.banking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           BankAccountRepository bankAccountRepository,
                           TransactionRepository transactionRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping initialization.");
            return;
        }

        log.info("Seeding initial demo data for Apex Banking & Financial Services Portal...");

        // 1. Create System Admin
        User admin = new User(
                "admin",
                "admin@apexbank.com",
                passwordEncoder.encode("Admin@123"),
                "Apex System Administrator",
                "+1-800-555-0199"
        );
        admin.setRoles(Set.of(Role.ROLE_ADMIN, Role.ROLE_CUSTOMER));
        admin = userRepository.save(admin);

        BankAccount adminAccount = new BankAccount(
                "APEX-90001001",
                AccountType.CHECKING,
                new BigDecimal("100000.00"),
                admin
        );
        adminAccount.setStatus(AccountStatus.ACTIVE);
        adminAccount = bankAccountRepository.save(adminAccount);

        // 2. Create Customer: John Doe
        User john = new User(
                "john_doe",
                "john.doe@apexbank.com",
                passwordEncoder.encode("Password@123"),
                "Johnathan Doe",
                "+1-555-234-5678"
        );
        john.setRoles(Set.of(Role.ROLE_CUSTOMER));
        john = userRepository.save(john);

        BankAccount johnChecking = new BankAccount(
                "APEX-10023481",
                AccountType.CHECKING,
                new BigDecimal("12450.50"),
                john
        );
        johnChecking.setStatus(AccountStatus.ACTIVE);
        johnChecking = bankAccountRepository.save(johnChecking);

        BankAccount johnSavings = new BankAccount(
                "APEX-20045612",
                AccountType.SAVINGS,
                new BigDecimal("35800.00"),
                john
        );
        johnSavings.setStatus(AccountStatus.ACTIVE);
        johnSavings = bankAccountRepository.save(johnSavings);

        // 3. Create Customer: Jane Smith
        User jane = new User(
                "jane_smith",
                "jane.smith@apexbank.com",
                passwordEncoder.encode("Password@123"),
                "Jane Smith",
                "+1-555-876-5432"
        );
        jane.setRoles(Set.of(Role.ROLE_CUSTOMER));
        jane = userRepository.save(jane);

        BankAccount janeChecking = new BankAccount(
                "APEX-30078923",
                AccountType.CHECKING,
                new BigDecimal("8720.00"),
                jane
        );
        janeChecking.setStatus(AccountStatus.ACTIVE);
        janeChecking = bankAccountRepository.save(janeChecking);

        BankAccount janeBusiness = new BankAccount(
                "APEX-40089104",
                AccountType.BUSINESS,
                new BigDecimal("54300.00"),
                jane
        );
        janeBusiness.setStatus(AccountStatus.FROZEN); // For testing frozen constraints
        janeBusiness = bankAccountRepository.save(janeBusiness);

        // 4. Seed Transaction Records
        LocalDateTime now = LocalDateTime.now();

        // Admin treasury deposit
        Transaction txn1 = new Transaction(
                "TXN-A1B2C3D4",
                TransactionType.DEPOSIT,
                new BigDecimal("100000.00"),
                new BigDecimal("100000.00"),
                "Federal Reserve Liquidity Inflow",
                TransactionStatus.COMPLETED,
                adminAccount,
                adminAccount
        );
        txn1.setTimestamp(now.minusDays(7));
        transactionRepository.save(txn1);

        // John initial paycheck deposit
        Transaction txn2 = new Transaction(
                "TXN-E5F6G7H8",
                TransactionType.DEPOSIT,
                new BigDecimal("15000.00"),
                new BigDecimal("15000.00"),
                "Corporate Direct Deposit Payroll",
                TransactionStatus.COMPLETED,
                johnChecking,
                johnChecking
        );
        txn2.setTimestamp(now.minusDays(5));
        transactionRepository.save(txn2);

        // John transfer to Jane
        Transaction txn3 = new Transaction(
                "TXN-I9J0K1L2",
                TransactionType.TRANSFER,
                new BigDecimal("1500.00"),
                new BigDecimal("13500.00"),
                "Consulting Services Remittance",
                TransactionStatus.COMPLETED,
                johnChecking,
                janeChecking
        );
        txn3.setTimestamp(now.minusDays(3));
        transactionRepository.save(txn3);

        // John ATM withdrawal
        Transaction txn4 = new Transaction(
                "TXN-M3N4O5P6",
                TransactionType.WITHDRAWAL,
                new BigDecimal("1049.50"),
                new BigDecimal("12450.50"),
                "Downtown ATM Cash Withdrawal",
                TransactionStatus.COMPLETED,
                johnChecking,
                johnChecking
        );
        txn4.setTimestamp(now.minusDays(1));
        transactionRepository.save(txn4);

        // John savings funding deposit
        Transaction txn5 = new Transaction(
                "TXN-Q7R8S9T0",
                TransactionType.DEPOSIT,
                new BigDecimal("35800.00"),
                new BigDecimal("35800.00"),
                "High-Yield Savings Balance Allocation",
                TransactionStatus.COMPLETED,
                johnSavings,
                johnSavings
        );
        txn5.setTimestamp(now.minusDays(4));
        transactionRepository.save(txn5);

        // Jane checking opening deposit
        Transaction txn6 = new Transaction(
                "TXN-U1V2W3X4",
                TransactionType.DEPOSIT,
                new BigDecimal("7220.00"),
                new BigDecimal("7220.00"),
                "Initial Client Retainer Deposit",
                TransactionStatus.COMPLETED,
                janeChecking,
                janeChecking
        );
        txn6.setTimestamp(now.minusDays(6));
        transactionRepository.save(txn6);

        log.info("Demo data seeding completed successfully.");
        log.info("Admin Account: admin / Admin@123");
        log.info("Customer Accounts: john_doe / Password@123, jane_smith / Password@123");
    }
}
