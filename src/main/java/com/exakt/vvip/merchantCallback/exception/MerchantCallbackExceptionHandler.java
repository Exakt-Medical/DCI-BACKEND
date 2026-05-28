package com.exakt.vvip.merchantCallback.exception;

import com.exakt.vvip.merchantCallback.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.exakt.vvip.merchantCallback")
public class MerchantCallbackExceptionHandler {

    @ExceptionHandler(MerchantCallbackException.class)
    public ResponseEntity<ApiErrorResponse> handleMerchantCallbackException(MerchantCallbackException exception) {
        HttpStatus status = exception.getStatus() == null ? HttpStatus.INTERNAL_SERVER_ERROR : exception.getStatus();
        return ResponseEntity.status(status).body(ApiErrorResponse.builder()
                .success(false)
                .code(exception.getErrorCode())
                .message(exception.getMessage())
                .details(exception.getDetails())
                .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.builder()
                .success(false)
                .code("bad_request")
                .message(exception.getMessage())
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiErrorResponse.builder()
                .success(false)
                .code("internal_error")
                .message("Unexpected error while processing merchant callback")
                .details(exception.getMessage())
                .build());
    }
}