package hu.bankmonitor.paymentservice.transfer;

import hu.bankmonitor.paymentservice.account.Account;
import hu.bankmonitor.paymentservice.account.AccountService;
import hu.bankmonitor.paymentservice.common.exception.BadRequestException;
import hu.bankmonitor.paymentservice.common.exception.NotFoundException;
import hu.bankmonitor.paymentservice.transfer.dto.CreateTransferRequest;
import hu.bankmonitor.paymentservice.transfer.dto.TransferResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountService accountService;

    public TransferService(TransferRepository transferRepository, AccountService accountService) {
        this.transferRepository = transferRepository;
        this.accountService = accountService;
    }

    @Transactional
    public TransferResponse createTransfer(CreateTransferRequest request) {
        if (request.sourceAccountId().equals(request.targetAccountId())) {
            throw new BadRequestException("Source and target account must differ");
        }

        Account source = accountService.findAccountOrThrow(request.sourceAccountId());
        Account target = accountService.findAccountOrThrow(request.targetAccountId());

        if (source.getCurrency() != target.getCurrency()) {
            // TODO: cross-currency conversion via the exchange rate client (see TODO-lista #4)
            throw new BadRequestException("Cross-currency transfers are not yet supported");
        }

        BigDecimal amount = request.amount();

        if (source.getBalance().compareTo(amount) < 0) {
            Transfer failed = new Transfer(source, target, source.getCurrency(), target.getCurrency(),
                    amount, amount, null, TransferStatus.FAILED, request.idempotencyKey());
            return TransferResponse.from(transferRepository.save(failed));
        }

        source.setBalance(source.getBalance().subtract(amount));
        target.setBalance(target.getBalance().add(amount));

        Transfer transfer = new Transfer(source, target, source.getCurrency(), target.getCurrency(),
                amount, amount, null, TransferStatus.COMPLETED, request.idempotencyKey());

        return TransferResponse.from(transferRepository.save(transfer));
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> listTransfers() {
        return transferRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(TransferResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> listTransfersForAccount(UUID accountId) {
        accountService.findAccountOrThrow(accountId);
        return transferRepository.findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(accountId, accountId).stream()
                .map(TransferResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransfer(UUID id) {
        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transfer not found: " + id));
        return TransferResponse.from(transfer);
    }
}
