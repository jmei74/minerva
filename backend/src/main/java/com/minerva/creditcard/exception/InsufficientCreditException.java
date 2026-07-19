package com.minerva.creditcard.exception;

public class InsufficientCreditException extends BusinessException {
    
    public InsufficientCreditException(String message) {
        super("INSUFFICIENT_CREDIT", message);
    }
}
