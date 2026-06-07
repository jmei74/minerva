package com.minerva.creditcard.exception;

/**
 * 额度不足异常
 * 架构参考: architecture-design.md §3.2
 */
public class InsufficientCreditException extends RuntimeException {
    private final String accountId;

    public InsufficientCreditException(String accountId) {
        super("Insufficient credit for account: " + accountId);
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }
}