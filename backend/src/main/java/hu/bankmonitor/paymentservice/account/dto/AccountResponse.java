package hu.bankmonitor.paymentservice.account.dto;

import hu.bankmonitor.paymentservice.account.Account;
import hu.bankmonitor.paymentservice.common.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String owner,
        Currency currency,
        BigDecimal balance,
        Instant createdAt
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getOwner(),
                account.getCurrency(),
                account.getBalance(),
                account.getCreatedAt()
        );
    }
}
