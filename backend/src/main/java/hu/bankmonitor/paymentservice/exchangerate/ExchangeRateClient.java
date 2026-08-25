package hu.bankmonitor.paymentservice.exchangerate;

import hu.bankmonitor.paymentservice.common.Currency;
import hu.bankmonitor.paymentservice.common.exception.ServiceUnavailableException;
import hu.bankmonitor.paymentservice.exchangerate.dto.ExchangeRateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Resilient client for the (mocked) external exchange rate provider. The provider is "flaky"
 * (random 503s, random latency), so calls are retried with exponential backoff up to a fixed
 * number of attempts, each bounded by a connect/read timeout, before giving up.
 */
@Component
public class ExchangeRateClient {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateClient.class);

    private final RestClient restClient;
    private final int maxAttempts;
    private final long initialBackoffMs;

    public ExchangeRateClient(
            @Value("${exchangerate.base-url}") String baseUrl,
            @Value("${exchangerate.max-attempts}") int maxAttempts,
            @Value("${exchangerate.initial-backoff-ms}") long initialBackoffMs,
            @Value("${exchangerate.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${exchangerate.read-timeout-ms}") long readTimeoutMs) {
        this.maxAttempts = maxAttempts;
        this.initialBackoffMs = initialBackoffMs;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public BigDecimal getRate(Currency from, Currency to) {
        RestClientException lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ExchangeRateResponse response = restClient.get()
                        .uri(uriBuilder -> uriBuilder.path("/mock/exchange-rate")
                                .queryParam("from", from)
                                .queryParam("to", to)
                                .build())
                        .retrieve()
                        .body(ExchangeRateResponse.class);

                if (response == null || response.rate() == null) {
                    throw new RestClientException("Empty response from exchange rate provider");
                }

                log.info("Fetched exchange rate {} -> {}: {} (attempt {}/{})",
                        from, to, response.rate(), attempt, maxAttempts);
                return response.rate();
            } catch (RestClientException ex) {
                lastError = ex;
                log.warn("Exchange rate lookup {} -> {} failed on attempt {}/{}: {}",
                        from, to, attempt, maxAttempts, ex.getMessage());
                if (attempt < maxAttempts) {
                    backoff(attempt);
                }
            }
        }

        throw new ServiceUnavailableException(
                "Exchange rate provider unavailable for " + from + " -> " + to
                        + " after " + maxAttempts + " attempts", lastError);
    }

    private void backoff(int attempt) {
        long delay = initialBackoffMs * (1L << (attempt - 1));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("Interrupted while waiting to retry exchange rate lookup", e);
        }
    }
}
