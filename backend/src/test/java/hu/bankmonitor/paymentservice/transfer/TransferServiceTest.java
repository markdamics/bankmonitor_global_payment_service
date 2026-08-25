package hu.bankmonitor.paymentservice.transfer;

import hu.bankmonitor.paymentservice.account.Account;
import hu.bankmonitor.paymentservice.account.AccountService;
import hu.bankmonitor.paymentservice.common.Currency;
import hu.bankmonitor.paymentservice.common.exception.BadRequestException;
import hu.bankmonitor.paymentservice.common.exception.ConflictException;
import hu.bankmonitor.paymentservice.common.exception.NotFoundException;
import hu.bankmonitor.paymentservice.exchangerate.ExchangeRateClient;
import hu.bankmonitor.paymentservice.transfer.dto.CreateTransferRequest;
import hu.bankmonitor.paymentservice.transfer.dto.TransferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private ExchangeRateClient exchangeRateClient;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(transferRepository, accountService, exchangeRateClient);
    }

    private void stubSaveAssigningId() {
        when(transferRepository.saveAndFlush(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);
            ReflectionTestUtils.setField(transfer, "id", UUID.randomUUID());
            return transfer;
        });
    }

    private Account account(Currency currency, String balance) {
        Account account = new Account("Owner", currency, new BigDecimal(balance));
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        return account;
    }

    @Test
    void createTransfer_rejectsSelfTransfer() {
        UUID accountId = UUID.randomUUID();
        when(transferRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        CreateTransferRequest request = new CreateTransferRequest(accountId, accountId, new BigDecimal("10"), "key-1");

        assertThatThrownBy(() -> transferService.createTransfer(request))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(accountService, exchangeRateClient);
    }

    @Test
    void createTransfer_completesSameCurrencyTransfer() {
        stubSaveAssigningId();
        Account source = account(Currency.EUR, "100.00");
        Account target = account(Currency.EUR, "10.00");
        when(transferRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(accountService.findAccountForUpdateOrThrow(source.getId())).thenReturn(source);
        when(accountService.findAccountForUpdateOrThrow(target.getId())).thenReturn(target);

        CreateTransferRequest request = new CreateTransferRequest(
                source.getId(), target.getId(), new BigDecimal("30.00"), "key-1");

        TransferCreationResult result = transferService.createTransfer(request);

        assertThat(result.newlyCreated()).isTrue();
        TransferResponse response = result.transfer();
        assertThat(response.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(response.sourceAmount()).isEqualByComparingTo("30.00");
        assertThat(response.targetAmount()).isEqualByComparingTo("30.00");
        assertThat(response.exchangeRate()).isNull();
        assertThat(source.getBalance()).isEqualByComparingTo("70.00");
        assertThat(target.getBalance()).isEqualByComparingTo("40.00");
        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void createTransfer_convertsCurrencyUsingFetchedRate() {
        stubSaveAssigningId();
        Account source = account(Currency.EUR, "100.00");
        Account target = account(Currency.USD, "0.00");
        when(transferRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(accountService.findAccountForUpdateOrThrow(source.getId())).thenReturn(source);
        when(accountService.findAccountForUpdateOrThrow(target.getId())).thenReturn(target);
        when(exchangeRateClient.getRate(Currency.EUR, Currency.USD)).thenReturn(new BigDecimal("1.10"));

        CreateTransferRequest request = new CreateTransferRequest(
                source.getId(), target.getId(), new BigDecimal("10.00"), "key-1");

        TransferResponse response = transferService.createTransfer(request).transfer();

        assertThat(response.exchangeRate()).isEqualByComparingTo("1.10");
        assertThat(response.targetAmount()).isEqualByComparingTo("11.0000");
        assertThat(source.getBalance()).isEqualByComparingTo("90.00");
        assertThat(target.getBalance()).isEqualByComparingTo("11.0000");
    }

    @Test
    void createTransfer_recordsFailedTransfer_whenInsufficientFunds() {
        stubSaveAssigningId();
        Account source = account(Currency.EUR, "5.00");
        Account target = account(Currency.EUR, "0.00");
        when(transferRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(accountService.findAccountForUpdateOrThrow(source.getId())).thenReturn(source);
        when(accountService.findAccountForUpdateOrThrow(target.getId())).thenReturn(target);

        CreateTransferRequest request = new CreateTransferRequest(
                source.getId(), target.getId(), new BigDecimal("50.00"), "key-1");

        TransferCreationResult result = transferService.createTransfer(request);

        assertThat(result.newlyCreated()).isTrue();
        assertThat(result.transfer().status()).isEqualTo(TransferStatus.FAILED);
        assertThat(source.getBalance()).isEqualByComparingTo("5.00");
        assertThat(target.getBalance()).isEqualByComparingTo("0.00");
        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void createTransfer_throwsNotFound_whenSourceAccountMissing() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(transferRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(accountService.findAccountForUpdateOrThrow(any(UUID.class)))
                .thenThrow(new NotFoundException("Account not found: " + sourceId));

        CreateTransferRequest request = new CreateTransferRequest(sourceId, targetId, new BigDecimal("10"), "key-1");

        assertThatThrownBy(() -> transferService.createTransfer(request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createTransfer_replaysExistingTransfer_whenSamePayload() {
        Account source = account(Currency.EUR, "100.00");
        Account target = account(Currency.EUR, "0.00");
        Transfer existing = new Transfer(source, target, Currency.EUR, Currency.EUR,
                new BigDecimal("30.00"), new BigDecimal("30.00"), null, TransferStatus.COMPLETED, "key-1");
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
        when(transferRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        CreateTransferRequest request = new CreateTransferRequest(
                source.getId(), target.getId(), new BigDecimal("30.00"), "key-1");

        TransferCreationResult result = transferService.createTransfer(request);

        assertThat(result.newlyCreated()).isFalse();
        assertThat(result.transfer().id()).isEqualTo(existing.getId());
        verifyNoInteractions(accountService, exchangeRateClient);
        verify(transferRepository, never()).saveAndFlush(any());
    }

    @Test
    void createTransfer_throwsConflict_whenSameKeyDifferentPayload() {
        Account source = account(Currency.EUR, "100.00");
        Account target = account(Currency.EUR, "0.00");
        Transfer existing = new Transfer(source, target, Currency.EUR, Currency.EUR,
                new BigDecimal("30.00"), new BigDecimal("30.00"), null, TransferStatus.COMPLETED, "key-1");
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
        when(transferRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        CreateTransferRequest request = new CreateTransferRequest(
                source.getId(), target.getId(), new BigDecimal("99.00"), "key-1");

        assertThatThrownBy(() -> transferService.createTransfer(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createTransfer_throwsConflict_whenConcurrentSaveRacesOnSameKey() {
        Account source = account(Currency.EUR, "100.00");
        Account target = account(Currency.EUR, "0.00");
        when(transferRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(accountService.findAccountForUpdateOrThrow(source.getId())).thenReturn(source);
        when(accountService.findAccountForUpdateOrThrow(target.getId())).thenReturn(target);
        when(transferRepository.saveAndFlush(any(Transfer.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint violated"));

        CreateTransferRequest request = new CreateTransferRequest(
                source.getId(), target.getId(), new BigDecimal("10.00"), "key-1");

        assertThatThrownBy(() -> transferService.createTransfer(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void listTransfers_mapsRepositoryResults() {
        Account source = account(Currency.EUR, "100.00");
        Account target = account(Currency.EUR, "0.00");
        Transfer transfer = new Transfer(source, target, Currency.EUR, Currency.EUR,
                new BigDecimal("10"), new BigDecimal("10"), null, TransferStatus.COMPLETED, "key-1");
        ReflectionTestUtils.setField(transfer, "id", UUID.randomUUID());
        when(transferRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(transfer));

        List<TransferResponse> responses = transferService.listTransfers();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(transfer.getId());
    }

    @Test
    void listTransfersForAccount_throwsNotFound_whenAccountMissing() {
        UUID accountId = UUID.randomUUID();
        when(accountService.findAccountOrThrow(accountId))
                .thenThrow(new NotFoundException("Account not found: " + accountId));

        assertThatThrownBy(() -> transferService.listTransfersForAccount(accountId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getTransfer_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(transferRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.getTransfer(id))
                .isInstanceOf(NotFoundException.class);
    }
}
