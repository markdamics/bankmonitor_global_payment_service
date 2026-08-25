package hu.bankmonitor.paymentservice.exchangerate;

import hu.bankmonitor.paymentservice.common.Currency;
import hu.bankmonitor.paymentservice.exchangerate.dto.ExchangeRateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stands in for the "flaky" third-party exchange rate provider described in the assignment
 * (random 503s, random latency). It is a separate simulated boundary that {@link ExchangeRateClient}
 * calls over HTTP, not part of this service's own public API.
 */
@RestController
public class ExchangeRateMockController {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateMockController.class);

    private static final Map<String, BigDecimal> BASE_RATES = Map.of(
            "EUR-USD", new BigDecimal("1.08"),
            "USD-EUR", new BigDecimal("0.93"),
            "EUR-HUF", new BigDecimal("395"),
            "HUF-EUR", new BigDecimal("0.00253"),
            "USD-HUF", new BigDecimal("365"),
            "HUF-USD", new BigDecimal("0.00274")
    );

    private final double errorRate;
    private final long minDelayMs;
    private final long maxDelayMs;

    public ExchangeRateMockController(
            @Value("${mock.exchange-rate.error-rate}") double errorRate,
            @Value("${mock.exchange-rate.min-delay-ms}") long minDelayMs,
            @Value("${mock.exchange-rate.max-delay-ms}") long maxDelayMs) {
        this.errorRate = errorRate;
        this.minDelayMs = minDelayMs;
        this.maxDelayMs = maxDelayMs;
    }

    @GetMapping("/mock/exchange-rate")
    public ResponseEntity<ExchangeRateResponse> getRate(@RequestParam Currency from, @RequestParam Currency to) {
        simulateLatency();

        if (ThreadLocalRandom.current().nextDouble() < errorRate) {
            log.info("Mock exchange rate provider: simulated 503 for {} -> {}", from, to);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        BigDecimal rate = resolveRate(from, to);
        return ResponseEntity.ok(new ExchangeRateResponse(from, to, rate));
    }

    private BigDecimal resolveRate(Currency from, Currency to) {
        if (from == to) {
            return BigDecimal.ONE;
        }
        BigDecimal base = BASE_RATES.get(from + "-" + to);
        if (base == null) {
            throw new IllegalArgumentException("No mock rate configured for " + from + " -> " + to);
        }
        double jitter = 1 + (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.02; // +-1%
        return base.multiply(BigDecimal.valueOf(jitter)).setScale(6, RoundingMode.HALF_UP);
    }

    private void simulateLatency() {
        long delay = ThreadLocalRandom.current().nextLong(minDelayMs, maxDelayMs + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
