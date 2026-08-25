package hu.bankmonitor.paymentservice.transfer;

import hu.bankmonitor.paymentservice.account.Account;
import hu.bankmonitor.paymentservice.common.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_account_id", nullable = false)
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_account_id", nullable = false)
    private Account targetAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency sourceCurrency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency targetCurrency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal sourceAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal targetAmount;

    @Column(precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransferStatus status;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Transfer() {
    }

    public Transfer(Account sourceAccount, Account targetAccount, Currency sourceCurrency, Currency targetCurrency,
                     BigDecimal sourceAmount, BigDecimal targetAmount, BigDecimal exchangeRate,
                     TransferStatus status, String idempotencyKey) {
        this.sourceAccount = sourceAccount;
        this.targetAccount = targetAccount;
        this.sourceCurrency = sourceCurrency;
        this.targetCurrency = targetCurrency;
        this.sourceAmount = sourceAmount;
        this.targetAmount = targetAmount;
        this.exchangeRate = exchangeRate;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Account getSourceAccount() {
        return sourceAccount;
    }

    public Account getTargetAccount() {
        return targetAccount;
    }

    public Currency getSourceCurrency() {
        return sourceCurrency;
    }

    public Currency getTargetCurrency() {
        return targetCurrency;
    }

    public BigDecimal getSourceAmount() {
        return sourceAmount;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
