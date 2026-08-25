package hu.bankmonitor.paymentservice.transfer;

import hu.bankmonitor.paymentservice.account.Account;
import hu.bankmonitor.paymentservice.account.AccountService;
import hu.bankmonitor.paymentservice.common.exception.BadRequestException;
import hu.bankmonitor.paymentservice.common.exception.ConflictException;
import hu.bankmonitor.paymentservice.common.exception.NotFoundException;
import hu.bankmonitor.paymentservice.exchangerate.ExchangeRateClient;
import hu.bankmonitor.paymentservice.transfer.dto.CreateTransferRequest;
import hu.bankmonitor.paymentservice.transfer.dto.TransferResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferRepository transferRepository;
    private final AccountService accountService;
    private final ExchangeRateClient exchangeRateClient;

    public TransferService(TransferRepository transferRepository, AccountService accountService,
                            ExchangeRateClient exchangeRateClient) {
        this.transferRepository = transferRepository;
        this.accountService = accountService;
        this.exchangeRateClient = exchangeRateClient;
    }

    @Transactional
    public TransferCreationResult createTransfer(CreateTransferRequest request) {
        log.info("Transfer requested: {} -> {}, amount={}, idempotencyKey='{}'",
                request.sourceAccountId(), request.targetAccountId(), request.amount(), request.idempotencyKey());

        Optional<Transfer> existing = transferRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return replay(existing.get(), request);
        }

        if (request.sourceAccountId().equals(request.targetAccountId())) {
            log.warn("Rejected transfer: source and target account are the same ({})", request.sourceAccountId());
            throw new BadRequestException("Source and target account must differ");
        }

        Account first;
        Account second;
        if (request.sourceAccountId().compareTo(request.targetAccountId()) < 0) {
            first = accountService.findAccountForUpdateOrThrow(request.sourceAccountId());
            second = accountService.findAccountForUpdateOrThrow(request.targetAccountId());
        } else {
            first = accountService.findAccountForUpdateOrThrow(request.targetAccountId());
            second = accountService.findAccountForUpdateOrThrow(request.sourceAccountId());
        }
        Account source = first.getId().equals(request.sourceAccountId()) ? first : second;
        Account target = first.getId().equals(request.sourceAccountId()) ? second : first;

        BigDecimal amount = request.amount();

        if (source.getBalance().compareTo(amount) < 0) {
            Transfer failed = new Transfer(source, target, source.getCurrency(), target.getCurrency(),
                    amount, amount, null, TransferStatus.FAILED, request.idempotencyKey());
            failed = saveOrThrowOnConcurrentDuplicate(failed);
            log.warn("Transfer {} FAILED: insufficient funds on account {} (balance={}, requested={})",
                    failed.getId(), source.getId(), source.getBalance(), amount);
            return new TransferCreationResult(TransferResponse.from(failed), true);
        }

        BigDecimal targetAmount;
        BigDecimal exchangeRate;
        if (source.getCurrency() == target.getCurrency()) {
            targetAmount = amount;
            exchangeRate = null;
        } else {
            exchangeRate = exchangeRateClient.getRate(source.getCurrency(), target.getCurrency());
            targetAmount = amount.multiply(exchangeRate).setScale(4, RoundingMode.HALF_UP);
        }

        source.setBalance(source.getBalance().subtract(amount));
        target.setBalance(target.getBalance().add(targetAmount));

        Transfer transfer = new Transfer(source, target, source.getCurrency(), target.getCurrency(),
                amount, targetAmount, exchangeRate, TransferStatus.COMPLETED, request.idempotencyKey());
        transfer = saveOrThrowOnConcurrentDuplicate(transfer);

        log.info("Transfer {} COMPLETED: {} {} -> {} {} (rate={})",
                transfer.getId(), amount, source.getCurrency(), targetAmount, target.getCurrency(), exchangeRate);

        return new TransferCreationResult(TransferResponse.from(transfer), true);
    }

    private Transfer saveOrThrowOnConcurrentDuplicate(Transfer transfer) {
        try {
            return transferRepository.saveAndFlush(transfer);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Idempotency key '{}' was concurrently claimed by another in-flight request",
                    transfer.getIdempotencyKey());
            throw new ConflictException("A request with idempotency key '" + transfer.getIdempotencyKey()
                    + "' is already being processed");
        }
    }

    private TransferCreationResult replay(Transfer existing, CreateTransferRequest request) {
        boolean samePayload = existing.getSourceAccount().getId().equals(request.sourceAccountId())
                && existing.getTargetAccount().getId().equals(request.targetAccountId())
                && existing.getSourceAmount().compareTo(request.amount()) == 0;

        if (!samePayload) {
            log.warn("Idempotency key '{}' reused with a different payload than transfer {}",
                    request.idempotencyKey(), existing.getId());
            throw new ConflictException("Idempotency key '" + request.idempotencyKey()
                    + "' was already used for a different transfer request");
        }

        log.info("Idempotent replay for key '{}': returning existing transfer {} (status={})",
                request.idempotencyKey(), existing.getId(), existing.getStatus());
        return new TransferCreationResult(TransferResponse.from(existing), false);
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> listTransfers() {
        log.debug("Listing all transfers");
        return transferRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(TransferResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> listTransfersForAccount(UUID accountId) {
        accountService.findAccountOrThrow(accountId);
        log.debug("Listing transfers for account {}", accountId);
        return transferRepository.findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(accountId, accountId).stream()
                .map(TransferResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransfer(UUID id) {
        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Transfer not found: {}", id);
                    return new NotFoundException("Transfer not found: " + id);
                });
        return TransferResponse.from(transfer);
    }
}
