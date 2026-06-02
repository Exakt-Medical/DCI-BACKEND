package com.dci.clearance.merchantCallback.service;

import com.dci.clearance.merchantCallback.client.TlpeClient;
import com.dci.clearance.merchantCallback.dto.TransactionReport;
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