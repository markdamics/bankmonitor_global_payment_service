package hu.bankmonitor.paymentservice.exchangerate.dto;

import hu.bankmonitor.paymentservice.common.Currency;

import java.math.BigDecimal;

public record ExchangeRateResponse(Currency from, Currency to, BigDecimal rate) {
}
