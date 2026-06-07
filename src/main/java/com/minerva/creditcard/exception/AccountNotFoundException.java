package com.minerva.creditcard.exception;

/**
 * 账户不存在或状态异常
 */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}