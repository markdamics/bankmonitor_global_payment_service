package hu.bankmonitor.paymentservice.account;

import hu.bankmonitor.paymentservice.account.dto.AccountResponse;
import hu.bankmonitor.paymentservice.account.dto.CreateAccountRequest;
import hu.bankmonitor.paymentservice.common.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = new Account(request.owner(), request.currency(), request.initialBalance());
        account = accountRepository.save(account);
        log.info("Created account {} (owner='{}', currency={}, initialBalance={})",
                account.getId(), account.getOwner(), account.getCurrency(), account.getBalance());
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listAccounts() {
        List<Account> accounts = accountRepository.findAll();
        log.debug("Listing {} accounts", accounts.size());
        return accounts.stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID id) {
        return AccountResponse.from(findAccountOrThrow(id));
    }

    public Account findAccountOrThrow(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Account not found: {}", id);
                    return new NotFoundException("Account not found: " + id);
                });
    }
}
