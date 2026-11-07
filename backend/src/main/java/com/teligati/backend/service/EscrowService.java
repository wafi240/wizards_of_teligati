package com.teligati.backend.service;

import com.teligati.backend.dto.EscrowCreateRequest;
import com.teligati.backend.dto.EscrowResponse;
import com.teligati.backend.entity.Escrow;
import com.teligati.backend.entity.Job;
import com.teligati.backend.entity.Transaction;
import com.teligati.backend.entity.User;
import com.teligati.backend.entity.Wallet;
import com.teligati.backend.exception.BadRequestException;
import com.teligati.backend.exception.ResourceNotFoundException;
import com.teligati.backend.repository.EscrowRepository;
import com.teligati.backend.repository.JobRepository;
import com.teligati.backend.repository.TransactionRepository;
import com.teligati.backend.repository.UserRepository;
import com.teligati.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
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
public class EscrowService {

    private final EscrowRepository escrowRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final WalletService walletService;

    public static final String DEMO_NOTICE = "DEMO MODE — All balances and transactions are simulated. No real money is transferred.";
    public static final String DEMO_DISCLAIMER = "DEMO WALLET — NO REAL MONEY INVOLVED";

    @Transactional
    public EscrowResponse createAndFundEscrow(Long payerId, EscrowCreateRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Escrow amount must be greater than zero");
        }
        if (payerId.equals(request.getPayeeId())) {
            throw new BadRequestException("Payer and payee cannot be the same user");
        }

        User payer = userRepository.findById(payerId)
                .orElseThrow(() -> new ResourceNotFoundException("Payer user not found with id: " + payerId));
        User payee = userRepository.findById(request.getPayeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Payee user not found with id: " + request.getPayeeId()));

        Wallet payerWallet = walletService.getOrCreateWalletEntity(payer);
        if (payerWallet.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            throw new BadRequestException("Insufficient demo available balance to fund escrow. Available: "
                    + payerWallet.getAvailableBalance() + ", required: " + request.getAmount());
        }

        Job job = null;
        if (request.getJobId() != null) {
            job = jobRepository.findById(request.getJobId()).orElse(null);
        }

        // Lock demo funds atomically
        payerWallet.setAvailableBalance(payerWallet.getAvailableBalance().subtract(request.getAmount()));
        payerWallet.setEscrowBalance(payerWallet.getEscrowBalance().add(request.getAmount()));
        walletRepository.save(payerWallet);

        // Ensure payee has wallet created
        walletService.getOrCreateWalletEntity(payee);

        Escrow escrow = Escrow.builder()
                .escrowReference("ESC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .title(request.getTitle())
                .description(request.getDescription())
                .amount(request.getAmount())
                .status("FUNDED")
                .payer(payer)
                .payee(payee)
                .job(job)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        escrow = escrowRepository.save(escrow);

        Transaction lockTx = Transaction.builder()
                .transactionId("TX-LOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .title("Demo Escrow Locked: " + request.getTitle())
                .category("ESCROW_LOCK")
                .type("ESCROW_LOCK")
                .status("COMPLETED")
                .amount(request.getAmount())
                .user(payer)
                .escrow(escrow)
                .timestamp(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(lockTx);

        log.info("Simulated escrow created & funded: id={}, ref={}, payer={}, payee={}, amount={}",
                escrow.getId(), escrow.getEscrowReference(), payerId, request.getPayeeId(), request.getAmount());
        return toEscrowResponse(escrow);
    }

    @Transactional
    public EscrowResponse startWork(Long escrowId, Long callerId) {
        Escrow escrow = escrowRepository.findById(escrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow contract not found with id: " + escrowId));

        if (!escrow.getPayer().getId().equals(callerId) && !escrow.getPayee().getId().equals(callerId)) {
            throw new AccessDeniedException("Unauthorized to modify this escrow contract");
        }

        if (!"FUNDED".equalsIgnoreCase(escrow.getStatus())) {
            throw new BadRequestException("Escrow must be in FUNDED status to start work. Current status: " + escrow.getStatus());
        }

        escrow.setStatus("IN_PROGRESS");
        escrow = escrowRepository.save(escrow);
        log.info("Escrow id={} transitioned to IN_PROGRESS by user id={}", escrowId, callerId);
        return toEscrowResponse(escrow);
    }

    @Transactional
    public EscrowResponse releaseEscrow(Long escrowId, Long callerId) {
        Escrow escrow = escrowRepository.findById(escrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow contract not found with id: " + escrowId));

        if (!escrow.getPayer().getId().equals(callerId)) {
            throw new AccessDeniedException("Only the payer who funded this escrow can approve payment release");
        }

        if (!"FUNDED".equalsIgnoreCase(escrow.getStatus()) && !"IN_PROGRESS".equalsIgnoreCase(escrow.getStatus())) {
            throw new BadRequestException("Cannot release escrow with status: " + escrow.getStatus());
        }

        Wallet payerWallet = walletService.getOrCreateWalletEntity(escrow.getPayer());
        Wallet payeeWallet = walletService.getOrCreateWalletEntity(escrow.getPayee());

        // Deduct from payer's escrow balance
        if (payerWallet.getEscrowBalance().compareTo(escrow.getAmount()) < 0) {
            throw new BadRequestException("Integrity error: Payer escrow balance is lower than escrow amount");
        }
        payerWallet.setEscrowBalance(payerWallet.getEscrowBalance().subtract(escrow.getAmount()));
        walletRepository.save(payerWallet);

        // Credit to payee's available balance
        payeeWallet.setAvailableBalance(payeeWallet.getAvailableBalance().add(escrow.getAmount()));
        walletRepository.save(payeeWallet);

        escrow.setStatus("RELEASED");
        escrow = escrowRepository.save(escrow);

        // Payee transaction
        Transaction payeeTx = Transaction.builder()
                .transactionId("TX-REL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .title("Demo Escrow Released: " + escrow.getTitle())
                .category("EARNING")
                .type("ESCROW_RELEASE")
                .status("COMPLETED")
                .amount(escrow.getAmount())
                .user(escrow.getPayee())
                .escrow(escrow)
                .timestamp(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(payeeTx);

        // Payer transaction
        Transaction payerTx = Transaction.builder()
                .transactionId("TX-DISB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .title("Demo Escrow Disbursed: " + escrow.getTitle())
                .category("ESCROW")
                .type("ESCROW_RELEASE")
                .status("COMPLETED")
                .amount(escrow.getAmount())
                .user(escrow.getPayer())
                .escrow(escrow)
                .timestamp(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(payerTx);

        log.info("Demo escrow id={} successfully RELEASED to payee id={}", escrowId, escrow.getPayee().getId());
        return toEscrowResponse(escrow);
    }

    @Transactional
    public EscrowResponse refundEscrow(Long escrowId, Long callerId) {
        Escrow escrow = escrowRepository.findById(escrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow contract not found with id: " + escrowId));

        if (!escrow.getPayer().getId().equals(callerId) && !escrow.getPayee().getId().equals(callerId)) {
            throw new AccessDeniedException("Unauthorized to refund this escrow contract");
        }

        if (!"FUNDED".equalsIgnoreCase(escrow.getStatus()) && !"IN_PROGRESS".equalsIgnoreCase(escrow.getStatus()) && !"DISPUTED".equalsIgnoreCase(escrow.getStatus())) {
            throw new BadRequestException("Cannot refund escrow with status: " + escrow.getStatus());
        }

        Wallet payerWallet = walletService.getOrCreateWalletEntity(escrow.getPayer());

        // Reverse escrow hold back to available balance
        if (payerWallet.getEscrowBalance().compareTo(escrow.getAmount()) < 0) {
            throw new BadRequestException("Integrity error: Payer escrow balance is lower than escrow amount");
        }
        payerWallet.setEscrowBalance(payerWallet.getEscrowBalance().subtract(escrow.getAmount()));
        payerWallet.setAvailableBalance(payerWallet.getAvailableBalance().add(escrow.getAmount()));
        walletRepository.save(payerWallet);

        escrow.setStatus("REFUNDED");
        escrow = escrowRepository.save(escrow);

        Transaction refundTx = Transaction.builder()
                .transactionId("TX-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .title("Demo Escrow Refunded: " + escrow.getTitle())
                .category("REFUND")
                .type("ESCROW_REFUND")
                .status("COMPLETED")
                .amount(escrow.getAmount())
                .user(escrow.getPayer())
                .escrow(escrow)
                .timestamp(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(refundTx);

        log.info("Demo escrow id={} successfully REFUNDED to payer id={}", escrowId, escrow.getPayer().getId());
        return toEscrowResponse(escrow);
    }

    @Transactional
    public EscrowResponse disputeEscrow(Long escrowId, Long callerId, String reason) {
        Escrow escrow = escrowRepository.findById(escrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow contract not found with id: " + escrowId));

        if (!escrow.getPayer().getId().equals(callerId) && !escrow.getPayee().getId().equals(callerId)) {
            throw new AccessDeniedException("Unauthorized to dispute this escrow contract");
        }

        if (!"FUNDED".equalsIgnoreCase(escrow.getStatus()) && !"IN_PROGRESS".equalsIgnoreCase(escrow.getStatus())) {
            throw new BadRequestException("Cannot dispute escrow with status: " + escrow.getStatus());
        }

        escrow.setStatus("DISPUTED");
        escrow = escrowRepository.save(escrow);
        log.info("Demo escrow id={} DISPUTED by user id={}, reason: {}", escrowId, callerId, reason);
        return toEscrowResponse(escrow);
    }

    @Transactional(readOnly = true)
    public EscrowResponse getEscrow(Long escrowId, Long callerId) {
        Escrow escrow = escrowRepository.findById(escrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Escrow contract not found with id: " + escrowId));

        if (!escrow.getPayer().getId().equals(callerId) && !escrow.getPayee().getId().equals(callerId)) {
            throw new AccessDeniedException("Unauthorized to view this escrow contract");
        }

        return toEscrowResponse(escrow);
    }

    @Transactional(readOnly = true)
    public List<EscrowResponse> getUserEscrows(Long userId) {
        return escrowRepository.findAllByUserId(userId).stream()
                .map(this::toEscrowResponse)
                .collect(Collectors.toList());
    }

    public EscrowResponse toEscrowResponse(Escrow escrow) {
        return EscrowResponse.builder()
                .id(escrow.getId())
                .escrowReference(escrow.getEscrowReference())
                .title(escrow.getTitle())
                .description(escrow.getDescription())
                .amount(escrow.getAmount())
                .status(escrow.getStatus())
                .payerId(escrow.getPayer().getId())
                .payerName(escrow.getPayer().getName())
                .payeeId(escrow.getPayee().getId())
                .payeeName(escrow.getPayee().getName())
                .jobId(escrow.getJob() != null ? escrow.getJob().getId() : null)
                .createdAt(escrow.getCreatedAt())
                .updatedAt(escrow.getUpdatedAt())
                .disclaimer(DEMO_DISCLAIMER)
                .demoNotice(DEMO_NOTICE)
                .build();
    }
}
