package hu.bankmonitor.paymentservice.transfer.dto;

import hu.bankmonitor.paymentservice.common.Currency;
import hu.bankmonitor.paymentservice.transfer.Transfer;
import hu.bankmonitor.paymentservice.transfer.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID sourceAccountId,
        UUID targetAccountId,
        Currency sourceCurrency,
        Currency targetCurrency,
        BigDecimal sourceAmount,
        BigDecimal targetAmount,
        BigDecimal exchangeRate,
        TransferStatus status,
        String idempotencyKey,
        Instant createdAt
) {

    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceAccount().getId(),
                transfer.getTargetAccount().getId(),
                transfer.getSourceCurrency(),
                transfer.getTargetCurrency(),
                transfer.getSourceAmount(),
                transfer.getTargetAmount(),
                transfer.getExchangeRate(),
                transfer.getStatus(),
                transfer.getIdempotencyKey(),
                transfer.getCreatedAt()
        );
    }
}
