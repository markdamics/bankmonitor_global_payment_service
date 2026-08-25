package hu.bankmonitor.paymentservice.exchangerate;

import hu.bankmonitor.paymentservice.common.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link ExchangeRateClient} against a real, running instance of the app's own
 * mocked exchange rate provider (see {@link ExchangeRateMockController}) over an actual HTTP
 * connection, with the provider forced to never fail so the happy path is deterministic.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {
        "server.port=18183",
        "exchangerate.base-url=http://localhost:18183",
        "mock.exchange-rate.error-rate=0.0",
        "mock.exchange-rate.min-delay-ms=0",
        "mock.exchange-rate.max-delay-ms=5"
})
class ExchangeRateClientSuccessIntegrationTest {

    @Autowired
    private ExchangeRateClient exchangeRateClient;

    @Test
    void getRate_returnsPositiveRate_whenProviderIsHealthy() {
        BigDecimal rate = exchangeRateClient.getRate(Currency.EUR, Currency.USD);

        assertThat(rate).isPositive();
    }
}
