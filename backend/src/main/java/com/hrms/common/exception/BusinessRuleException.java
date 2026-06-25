package com.hrms.common.exception;

/**
 * Thrown when a business rule is violated (e.g. overlapping effective periods,
 * duplicate code, invalid hierarchy placement). Maps to HTTP 422.
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
