package com.banking.accountservice.service;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatus;
import com.banking.accountservice.entity.AccountType;
import com.banking.accountservice.repository.AccountRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {
    private final AccountRepository accountRepository;
    private final SecureRandom secureRandom;
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest account) {
        log.info("Creating account for :" + account.getEmail());
        if(accountRepository.existsByEmail(account.getEmail())){
            throw new IllegalArgumentException("account already exists");
        }
        Account newAccount = new Account();
        newAccount.builder()
                .email(account.getEmail())
                .accountType(account.getAccountType())
                .email(account.getEmail())
                .phone(account.getPhone())
                .accountType(account.getAccountType())
                .balance(account.getInitialDeposit())
                .accountNumber(generateAccountNumber())
                .dailyTransactionLimit(
                        account.getAccountType() == AccountType.SAVINGS
                        ? new BigDecimal(100000)
                                : new BigDecimal(50000)
                )
                .build();
        accountRepository.save(newAccount);
        log.info("Account created " + newAccount.getAccountType());
        return mapToResponse(newAccount);
    }
    //create unique 12 digit account number
    public String generateAccountNumber() {
        String accountNumber;
        do{
            long number = secureRandom.nextLong(1_000_000_000_000L);
            accountNumber = String.format("%012d", number);
        }while(accountRepository.existsByAccountNumber(generateAccountNumber()));
        return accountNumber;
    }

    public AccountResponse getAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return mapToResponse(account);
    }
    public BigDecimal getBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return account.getBalance();

    }

    /**
     * Block account - called by Fraud detection service via kafka
     *
     */
    public void blockAccount(String accountNumber) {
        Account account =  accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.setAccountStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);
        log.info("Account blocked " + accountNumber);
    }

    /*
    * deduct balance from sender account
    * called by Transaction Service
    * @param accountNumber
    * @param amount
    * */
    public void deductBalance(String accountNumber,BigDecimal amount) {
        log.info("deducting balance for " + accountNumber);
        Account account =  accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if(!account.getAccountStatus().equals(AccountStatus.ACTIVE)){
            throw new IllegalArgumentException("account is not active");
        }
        if(account.getBalance().subtract(amount).compareTo(amount) < 0){
            throw new IllegalArgumentException("account balance not enough");
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
    }

    /*
       *Credit balance
       * Called by transaction via kafka
       *
       *
    * */
    public void creditBalance(String accountNumber,BigDecimal amount) {
        log.info("creating balance for " + accountNumber);
        Account account =  accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if(!account.getAccountStatus().equals(AccountStatus.ACTIVE)){
            throw new IllegalArgumentException("account is not active");
        }
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        log.info("credit balance for " + accountNumber);
    }
    public AccountResponse mapToResponse(Account account) {
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.builder()
                .email(account.getEmail())
                .accountType(account.getAccountType())
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountHolderName(account.getAccountHolderName())
                .phone(account.getPhone())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .dailyTransactionLimit(account.getDailyTransactionLimit())
                .createdAt(account.getCreatedAt())
                .build();
        return accountResponse;
    }
}
