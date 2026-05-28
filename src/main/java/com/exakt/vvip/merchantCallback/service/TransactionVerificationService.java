package com.exakt.vvip.merchantCallback.service;

import com.exakt.vvip.merchantCallback.client.TlpeClient;
import com.exakt.vvip.merchantCallback.dto.TransactionReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionVerificationService {

    private final TlpeClient tlpeClient;

    public TransactionReport fetchReport(String transactionId) {
        return tlpeClient.fetchReport(transactionId);
    }
}