package com.minerva.creditcard.exception;

public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String resourceType, String identifier) {
        super("NOT_FOUND", resourceType + " not found: " + identifier);
    }
}
