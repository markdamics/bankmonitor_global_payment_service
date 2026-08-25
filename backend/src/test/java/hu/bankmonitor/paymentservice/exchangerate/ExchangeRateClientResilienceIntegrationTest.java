package hu.bankmonitor.paymentservice.exchangerate;

import hu.bankmonitor.paymentservice.common.Currency;
import hu.bankmonitor.paymentservice.common.exception.ServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises {@link ExchangeRateClient}'s retry/backoff logic against a real, running instance of
 * the app's own mocked exchange rate provider (see {@link ExchangeRateMockController}), with the
 * provider forced to always fail (100% error rate) so exhausting all retries is deterministic.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {
        "server.port=18184",
        "exchangerate.base-url=http://localhost:18184",
        "exchangerate.max-attempts=3",
        "exchangerate.initial-backoff-ms=5",
        "mock.exchange-rate.error-rate=1.0",
        "mock.exchange-rate.min-delay-ms=0",
        "mock.exchange-rate.max-delay-ms=5"
})
class ExchangeRateClientResilienceIntegrationTest {

    @Autowired
    private ExchangeRateClient exchangeRateClient;

    @Test
    void getRate_throwsServiceUnavailable_afterExhaustingRetries() {
        assertThatThrownBy(() -> exchangeRateClient.getRate(Currency.EUR, Currency.USD))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("after 3 attempts");
    }
}
