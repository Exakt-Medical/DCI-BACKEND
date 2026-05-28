package com.exakt.vvip.merchantCallback.util;

public final class TransactionIdValidator {

    private TransactionIdValidator() {
    }

    public static String normalize(String transactionId) {
        if (transactionId == null) {
            throw new IllegalArgumentException("transaction_id is required");
        }

        String normalized = transactionId.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("transaction_id is required");
        }

        return normalized;
    }
}