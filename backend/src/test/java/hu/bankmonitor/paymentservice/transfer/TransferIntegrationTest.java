package hu.bankmonitor.paymentservice.transfer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises idempotency and concurrency behavior against the real REST layer, service layer and
 * H2 database (MockMvc dispatches in-process, but every request runs through a real, independent
 * transaction and database connection, so the pessimistic account locking and the idempotencyKey
 * unique constraint are genuinely exercised under real concurrent threads).
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransferIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String createAccount(String owner, String currency, String balance) throws Exception {
        String body = """
                {"owner":"%s","currency":"%s","initialBalance":%s}
                """.formatted(owner, currency, balance);
        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private MvcResult submitTransfer(String sourceId, String targetId, String amount, String idempotencyKey) throws Exception {
        String body = """
                {"sourceAccountId":"%s","targetAccountId":"%s","amount":%s,"idempotencyKey":"%s"}
                """.formatted(sourceId, targetId, amount, idempotencyKey);
        return mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    @Test
    void sequentialRetryWithSamePayload_returnsOriginalResultWithoutDoubleDebit() throws Exception {
        String source = createAccount("Alice-" + UUID.randomUUID(), "EUR", "100.00");
        String target = createAccount("Bob-" + UUID.randomUUID(), "EUR", "0.00");
        String idempotencyKey = "retry-" + UUID.randomUUID();

        MvcResult first = submitTransfer(source, target, "30.00", idempotencyKey);
        assertThat(first.getResponse().getStatus()).isEqualTo(201);
        String firstId = objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asText();

        MvcResult retry = submitTransfer(source, target, "30.00", idempotencyKey);
        assertThat(retry.getResponse().getStatus()).isEqualTo(200);
        String retryId = objectMapper.readTree(retry.getResponse().getContentAsString()).get("id").asText();

        assertThat(retryId).isEqualTo(firstId);

        MvcResult sourceAccount = mockMvc.perform(get("/api/accounts/" + source)).andReturn();
        JsonNode sourceJson = objectMapper.readTree(sourceAccount.getResponse().getContentAsString());
        assertThat(sourceJson.get("balance").decimalValue()).isEqualByComparingTo("70.00");
    }

    @Test
    void retryWithSameKeyButDifferentPayload_returnsConflict() throws Exception {
        String source = createAccount("Alice-" + UUID.randomUUID(), "EUR", "100.00");
        String target = createAccount("Bob-" + UUID.randomUUID(), "EUR", "0.00");
        String idempotencyKey = "retry-" + UUID.randomUUID();

        submitTransfer(source, target, "30.00", idempotencyKey);
        MvcResult conflicting = submitTransfer(source, target, "99.00", idempotencyKey);

        assertThat(conflicting.getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    void concurrentTransfersFromSameAccount_neverOverdraw() throws Exception {
        String source = createAccount("Alice-" + UUID.randomUUID(), "EUR", "100.00");
        String targetB = createAccount("Bob-" + UUID.randomUUID(), "EUR", "0.00");
        String targetC = createAccount("Carol-" + UUID.randomUUID(), "EUR", "0.00");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<MvcResult> first = () -> submitTransfer(source, targetB, "80.00", "race-" + UUID.randomUUID());
            Callable<MvcResult> second = () -> submitTransfer(source, targetC, "80.00", "race-" + UUID.randomUUID());

            List<Future<MvcResult>> futures = executor.invokeAll(List.of(first, second));

            int completedCount = 0;
            for (Future<MvcResult> future : futures) {
                MvcResult result = future.get(10, TimeUnit.SECONDS);
                assertThat(result.getResponse().getStatus()).isEqualTo(201);
                String status = objectMapper.readTree(result.getResponse().getContentAsString()).get("status").asText();
                if ("COMPLETED".equals(status)) {
                    completedCount++;
                }
            }

            // Both requests individually fit the starting balance (100), but combined (160) would
            // overdraw it: the pessimistic account lock must serialize them so only one succeeds.
            assertThat(completedCount).isEqualTo(1);

            MvcResult sourceAccount = mockMvc.perform(get("/api/accounts/" + source)).andReturn();
            JsonNode sourceJson = objectMapper.readTree(sourceAccount.getResponse().getContentAsString());
            assertThat(sourceJson.get("balance").decimalValue()).isEqualByComparingTo("20.00");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentRequestsWithSameNewIdempotencyKey_onlyOnePersists() throws Exception {
        String source = createAccount("Dave-" + UUID.randomUUID(), "EUR", "100.00");
        String target = createAccount("Eve-" + UUID.randomUUID(), "EUR", "0.00");
        String idempotencyKey = "shared-" + UUID.randomUUID();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<MvcResult> first = () -> submitTransfer(source, target, "10.00", idempotencyKey);
            Callable<MvcResult> second = () -> submitTransfer(source, target, "10.00", idempotencyKey);

            List<Future<MvcResult>> futures = executor.invokeAll(List.of(first, second));

            List<Integer> statusCodes = new ArrayList<>();
            for (Future<MvcResult> future : futures) {
                statusCodes.add(future.get(10, TimeUnit.SECONDS).getResponse().getStatus());
            }
            assertThat(statusCodes).containsExactlyInAnyOrder(201, 409);

            MvcResult listResult = mockMvc.perform(get("/api/transfers").param("accountId", source)).andReturn();
            JsonNode transfers = objectMapper.readTree(listResult.getResponse().getContentAsString());
            long matching = 0;
            for (JsonNode transfer : transfers) {
                if (idempotencyKey.equals(transfer.get("idempotencyKey").asText())) {
                    matching++;
                }
            }
            assertThat(matching).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
