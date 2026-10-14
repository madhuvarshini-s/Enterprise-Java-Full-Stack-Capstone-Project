package com.apex.banking.service.impl;

import com.apex.banking.dto.request.LoginRequest;
import com.apex.banking.dto.request.RegisterRequest;
import com.apex.banking.dto.response.ApiResponse;
import com.apex.banking.dto.response.JwtResponse;
import com.apex.banking.dto.response.UserProfileResponse;
import com.apex.banking.entity.*;
import com.apex.banking.exception.DuplicateResourceException;
import com.apex.banking.exception.ResourceNotFoundException;
import com.apex.banking.repository.BankAccountRepository;
import com.apex.banking.repository.TransactionRepository;
import com.apex.banking.repository.UserRepository;
import com.apex.banking.security.CustomUserDetails;
import com.apex.banking.security.JwtUtils;
import com.apex.banking.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           BankAccountRepository bankAccountRepository,
                           TransactionRepository transactionRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public JwtResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Set<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getFullName(),
                roles
        );
    }

    @Override
    @Transactional
    public ApiResponse<String> register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already in use.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered.");
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getPhoneNumber()
        );
        user.setRoles(Collections.singleton(Role.ROLE_CUSTOMER));
        User savedUser = userRepository.save(user);

        // Auto-provision primary checking account with $1,000 welcome credit
        String accountNumber = generateUniqueAccountNumber();
        BigDecimal welcomeBonus = new BigDecimal("1000.00");
        BankAccount account = new BankAccount(
                accountNumber,
                AccountType.CHECKING,
                welcomeBonus,
                savedUser
        );
        account.setStatus(AccountStatus.ACTIVE);
        BankAccount savedAccount = bankAccountRepository.save(account);

        // Record welcome credit transaction
        Transaction welcomeTxn = new Transaction(
                "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                TransactionType.DEPOSIT,
                welcomeBonus,
                welcomeBonus,
                "Welcome Bonus Credit on Registration",
                TransactionStatus.COMPLETED,
                savedAccount,
                savedAccount
        );
        welcomeTxn.setTimestamp(LocalDateTime.now());
        transactionRepository.save(welcomeTxn);

        return ApiResponse.success("Customer registered successfully with account " + accountNumber + " and $1,000.00 welcome credit.");
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Set<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                roles,
                user.getCreatedAt()
        );
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            int num = 10000000 + secureRandom.nextInt(90000000);
            accountNumber = "APEX-" + num;
        } while (bankAccountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}
