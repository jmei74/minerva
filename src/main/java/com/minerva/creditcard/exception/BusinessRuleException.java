package com.minerva.creditcard.exception;

/**
 * 业务规则冲突（如账户未清账款不可销户）
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}