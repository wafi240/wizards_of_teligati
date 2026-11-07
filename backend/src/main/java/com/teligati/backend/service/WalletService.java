package com.teligati.backend.service;

import com.teligati.backend.dto.TopUpRequest;
import com.teligati.backend.dto.TransactionResponse;
import com.teligati.backend.dto.WalletResponse;
import com.teligati.backend.dto.WithdrawRequest;
import com.teligati.backend.entity.Transaction;
import com.teligati.backend.entity.User;
import com.teligati.backend.entity.Wallet;
import com.teligati.backend.exception.BadRequestException;
import com.teligati.backend.exception.ResourceNotFoundException;
import com.teligati.backend.repository.TransactionRepository;
import com.teligati.backend.repository.UserRepository;
import com.teligati.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public static final String DEMO_NOTICE = "DEMO MODE — All balances and transactions are simulated. No real money is transferred.";
    public static final String DEMO_DISCLAIMER = "DEMO WALLET — NO REAL MONEY INVOLVED";

    @Transactional
    public Wallet getOrCreateWalletEntity(User user) {
        return walletRepository.findByUser(user)
                .orElseGet(() -> {
                    log.info("Initializing demo wallet for user id={}", user.getId());
                    Wallet newWallet = Wallet.builder()
                            .user(user)
                            .availableBalance(new BigDecimal("10000.00"))
                            .escrowBalance(BigDecimal.ZERO)
                            .currency("DEMO_CREDITS")
                            .version(0L)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return walletRepository.save(newWallet);
                });
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> getOrCreateWalletEntity(user));

        return toWalletResponse(wallet);
    }

    @Transactional
    public WalletResponse simulateTopUp(Long userId, TopUpRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Top-up amount must be greater than zero");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Wallet wallet = getOrCreateWalletEntity(user);

        wallet.setAvailableBalance(wallet.getAvailableBalance().add(request.getAmount()));
        wallet = walletRepository.save(wallet);

        String method = request.getPaymentMethod() != null ? request.getPaymentMethod() : "SIMULATED_DEPOSIT";
        Transaction tx = Transaction.builder()
                .transactionId("TX-TOPUP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .title("Simulated Demo Credits Top-up (" + method + ")")
                .category("DEPOSIT")
                .type("TOPUP")
                .status("COMPLETED")
                .amount(request.getAmount())
                .user(user)
                .timestamp(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(tx);

        log.info("Simulated top-up of {} demo credits completed for user id={}", request.getAmount(), userId);
        return toWalletResponse(wallet);
    }

    @Transactional
    public WalletResponse simulateWithdraw(Long userId, WithdrawRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Withdrawal amount must be greater than zero");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Wallet wallet = getOrCreateWalletEntity(user);

        if (wallet.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            throw new BadRequestException("Insufficient demo available balance for withdrawal. Current balance: " 
                    + wallet.getAvailableBalance());
        }

        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(request.getAmount()));
        wallet = walletRepository.save(wallet);

        Transaction tx = Transaction.builder()
                .transactionId("TX-WDR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .title("Simulated Demo Withdrawal to " + request.getDestinationAccount())
                .category("WITHDRAW")
                .type("WITHDRAWAL")
                .status("COMPLETED")
                .amount(request.getAmount())
                .user(user)
                .timestamp(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(tx);

        log.info("Simulated withdrawal of {} demo credits completed for user id={}", request.getAmount(), userId);
        return toWalletResponse(wallet);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByTimestampDesc(userId).stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());
    }

    public WalletResponse toWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUser().getId())
                .availableBalance(wallet.getAvailableBalance())
                .escrowBalance(wallet.getEscrowBalance())
                .currency(wallet.getCurrency())
                .updatedAt(wallet.getUpdatedAt())
                .disclaimer(DEMO_DISCLAIMER)
                .demoNotice(DEMO_NOTICE)
                .build();
    }

    public TransactionResponse toTransactionResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .transactionId(tx.getTransactionId())
                .title(tx.getTitle())
                .category(tx.getCategory())
                .amount(tx.getAmount())
                .type(tx.getType())
                .status(tx.getStatus())
                .timestamp(tx.getTimestamp())
                .completedAt(tx.getCompletedAt())
                .userId(tx.getUser() != null ? tx.getUser().getId() : null)
                .escrowId(tx.getEscrow() != null ? tx.getEscrow().getId() : null)
                .disclaimer(DEMO_DISCLAIMER)
                .demoNotice(DEMO_NOTICE)
                .build();
    }
}
