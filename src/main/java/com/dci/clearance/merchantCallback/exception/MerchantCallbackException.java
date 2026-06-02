package com.dci.clearance.merchantCallback.exception;

import org.springframework.http.HttpStatus;

public class MerchantCallbackException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String details;

    public MerchantCallbackException(HttpStatus status, String errorCode, String message) {
        this(status, errorCode, message, null, null);
    }

    public MerchantCallbackException(HttpStatus status, String errorCode, String message, String details) {
        this(status, errorCode, message, details, null);
    }

    public MerchantCallbackException(HttpStatus status, String errorCode, String message, String details, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
        this.details = details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDetails() {
        return details;
    }
}