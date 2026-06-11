package com.dci.clearance.service;

import com.dci.clearance.entity.Company;
import com.dci.clearance.generateVoucher.client.BillerooClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Gateway service that encapsulates all Billeroo company-sync logic.
 * Controllers and other services should never call BillerooClient.syncCompany directly
 * for citizen shadow-company workflows — they go through this service instead.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillerooCompanySyncService {

    private final BillerooClient billerooClient;

    /**
     * Syncs a shadow company to Billeroo using the provided code, email, and company name.
     * Builds a transient Company entity to match the existing BillerooClient.syncCompany contract.
     *
     * @throws RuntimeException if Billeroo returns an error or the call times out
     */
    public void sync(String companyCode, String email, String companyName) {
        log.info("Syncing shadow company to Billeroo: code={}, name={}", companyCode, companyName);

        Company transientCompany = Company.builder()
                .code(companyCode)
                .email(email)
                .companyName(companyName)
                .status("ACTIVE")
                .build();

        billerooClient.syncCompany(transientCompany);

        log.info("Billeroo company sync succeeded for code={}", companyCode);
    }
}
