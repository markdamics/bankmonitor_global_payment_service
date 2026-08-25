package hu.bankmonitor.paymentservice.account;

import hu.bankmonitor.paymentservice.account.dto.AccountResponse;
import hu.bankmonitor.paymentservice.account.dto.CreateAccountRequest;
import hu.bankmonitor.paymentservice.common.Currency;
import hu.bankmonitor.paymentservice.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository);
    }

    private Account accountWithId(UUID id, String owner, Currency currency, BigDecimal balance) {
        Account account = new Account(owner, currency, balance);
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    @Test
    void createAccount_savesAndReturnsResponse() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
            return account;
        });

        AccountResponse response = accountService.createAccount(
                new CreateAccountRequest("Alice", Currency.EUR, new BigDecimal("100.00")));

        assertThat(response.owner()).isEqualTo("Alice");
        assertThat(response.currency()).isEqualTo(Currency.EUR);
        assertThat(response.balance()).isEqualByComparingTo("100.00");
        assertThat(response.id()).isNotNull();
    }

    @Test
    void listAccounts_mapsAllAccounts() {
        Account a = accountWithId(UUID.randomUUID(), "Alice", Currency.EUR, new BigDecimal("10"));
        Account b = accountWithId(UUID.randomUUID(), "Bob", Currency.USD, new BigDecimal("20"));
        when(accountRepository.findAll()).thenReturn(List.of(a, b));

        List<AccountResponse> responses = accountService.listAccounts();

        assertThat(responses).hasSize(2)
                .extracting(AccountResponse::owner)
                .containsExactly("Alice", "Bob");
    }

    @Test
    void getAccount_returnsResponse_whenFound() {
        UUID id = UUID.randomUUID();
        Account account = accountWithId(id, "Alice", Currency.EUR, new BigDecimal("10"));
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccount(id);

        assertThat(response.id()).isEqualTo(id);
    }

    @Test
    void getAccount_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findAccountOrThrow_returnsAccount_whenFound() {
        UUID id = UUID.randomUUID();
        Account account = accountWithId(id, "Alice", Currency.EUR, new BigDecimal("10"));
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));

        assertThat(accountService.findAccountOrThrow(id)).isSameAs(account);
    }

    @Test
    void findAccountOrThrow_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findAccountOrThrow(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findAccountForUpdateOrThrow_returnsAccount_whenFound() {
        UUID id = UUID.randomUUID();
        Account account = accountWithId(id, "Alice", Currency.EUR, new BigDecimal("10"));
        when(accountRepository.findByIdForUpdate(id)).thenReturn(Optional.of(account));

        assertThat(accountService.findAccountForUpdateOrThrow(id)).isSameAs(account);
    }

    @Test
    void findAccountForUpdateOrThrow_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findAccountForUpdateOrThrow(id))
                .isInstanceOf(NotFoundException.class);
    }
}
