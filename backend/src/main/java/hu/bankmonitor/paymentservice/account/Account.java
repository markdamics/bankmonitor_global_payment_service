package hu.bankmonitor.paymentservice.account;

import hu.bankmonitor.paymentservice.common.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String owner; //User

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Account() {
    }

    public Account(String owner, Currency currency, BigDecimal balance) {
        this.owner = owner;
        this.currency = currency;
        this.balance = balance;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
