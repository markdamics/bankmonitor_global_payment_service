package hu.bankmonitor.paymentservice.transfer;

import hu.bankmonitor.paymentservice.account.Account;
import hu.bankmonitor.paymentservice.account.AccountService;
import hu.bankmonitor.paymentservice.common.exception.BadRequestException;
import hu.bankmonitor.paymentservice.common.exception.NotFoundException;
import hu.bankmonitor.paymentservice.transfer.dto.CreateTransferRequest;
import hu.bankmonitor.paymentservice.transfer.dto.TransferResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferRepository transferRepository;
    private final AccountService accountService;

    public TransferService(TransferRepository transferRepository, AccountService accountService) {
        this.transferRepository = transferRepository;
        this.accountService = accountService;
    }

    @Transactional
    public TransferResponse createTransfer(CreateTransferRequest request) {
        log.info("Transfer requested: {} -> {}, amount={}, idempotencyKey='{}'",
                request.sourceAccountId(), request.targetAccountId(), request.amount(), request.idempotencyKey());

        if (request.sourceAccountId().equals(request.targetAccountId())) {
            log.warn("Rejected transfer: source and target account are the same ({})", request.sourceAccountId());
            throw new BadRequestException("Source and target account must differ");
        }

        Account source = accountService.findAccountOrThrow(request.sourceAccountId());
        Account target = accountService.findAccountOrThrow(request.targetAccountId());

        if (source.getCurrency() != target.getCurrency()) {
            // TODO: cross-currency conversion via the exchange rate client (see TODO-lista #4)
            log.warn("Rejected transfer {} -> {}: currency mismatch ({} vs {})",
                    source.getId(), target.getId(), source.getCurrency(), target.getCurrency());
            throw new BadRequestException("Cross-currency transfers are not yet supported");
        }

        BigDecimal amount = request.amount();

        if (source.getBalance().compareTo(amount) < 0) {
            Transfer failed = new Transfer(source, target, source.getCurrency(), target.getCurrency(),
                    amount, amount, null, TransferStatus.FAILED, request.idempotencyKey());
            failed = transferRepository.save(failed);
            log.warn("Transfer {} FAILED: insufficient funds on account {} (balance={}, requested={})",
                    failed.getId(), source.getId(), source.getBalance(), amount);
            return TransferResponse.from(failed);
        }

        source.setBalance(source.getBalance().subtract(amount));
        target.setBalance(target.getBalance().add(amount));

        Transfer transfer = new Transfer(source, target, source.getCurrency(), target.getCurrency(),
                amount, amount, null, TransferStatus.COMPLETED, request.idempotencyKey());
        transfer = transferRepository.save(transfer);

        log.info("Transfer {} COMPLETED: {} -> {}, amount={}", transfer.getId(), source.getId(), target.getId(), amount);

        return TransferResponse.from(transfer);
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
