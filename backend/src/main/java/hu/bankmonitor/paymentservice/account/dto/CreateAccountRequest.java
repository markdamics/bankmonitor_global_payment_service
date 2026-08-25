package hu.bankmonitor.paymentservice.account.dto;

import hu.bankmonitor.paymentservice.common.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank
        String owner,

        @NotNull
        Currency currency,

        @NotNull @DecimalMin(value = "0.00")
        BigDecimal initialBalance
) {
}
