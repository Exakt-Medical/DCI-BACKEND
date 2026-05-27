package com.exakt.vvip.service;

import com.exakt.vvip.dto.VoucherRedeemWebhookDto;
import com.exakt.vvip.entity.TransactionLog;
import com.exakt.vvip.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionLogService {

    private final TransactionLogRepository transactionLogRepository;

    @Transactional
    public void processVoucherRedeemWebhook(VoucherRedeemWebhookDto webhookData) {
        log.info("Received webhook: transactionReference={}, statusCode={}",
                webhookData.getTransactionReference(), webhookData.getStatusCode());

        // CHANGE THIS: rename variable from 'log' to 'transactionLog'
        TransactionLog transactionLog = TransactionLog.builder()
                .account(getAccountName(webhookData.getCompanyCode()))
                .company(getCompanyFullName(webhookData.getCompanyCode()))
                .refNo(webhookData.getTransactionReference())
                .description(generateDescription())
                .response(getResponseMessage(webhookData))
                .origin("Web")
                .status(getStatus(webhookData))
                .dateCreated(parseTimestamp(webhookData.getTimestamp()))
                .build();

        transactionLogRepository.save(transactionLog);
        log.info("Transaction log saved with id: {}", transactionLog.getId()); // Now this works
    }

    public Page<TransactionLog> getTransactionLogs(String status, String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return transactionLogRepository.searchAllFields(search, pageable);
        } else if (status != null && !"all".equals(status)) {
            return transactionLogRepository.findByStatus(status, pageable);
        } else {
            return transactionLogRepository.findAll(pageable);
        }
    }

    private String getAccountName(String companyCode) {
        return "Juan Dela Cruz";
    }

    private String getCompanyFullName(String companyCode) {
        switch(companyCode) {
            case "ALPHA":
                return "ALPHA INSURANCE AND SURETY COMPANY INC. - MANILA";
            case "EDC":
                return "EDC INSURANCE COMPANY INC. - MAIN";
            default:
                return companyCode + " INSURANCE COMPANY - MANILA";
        }
    }

    private String generateDescription() {
        return "INITIATED VEHICLE VERIFICATION VIA WEB (PLATE NO: LAJ1234)";
    }

    private String getResponseMessage(VoucherRedeemWebhookDto dto) {
        if ("OK.00.00".equals(dto.getStatusCode())) {
            return "VEHICLE HAS BEEN VERIFIED";
        } else {
            if (dto.getStatusDescription() != null) {
                return dto.getStatusDescription();
            }
            return "VEHICLE NOT FOUND!";
        }
    }

    private String getStatus(VoucherRedeemWebhookDto dto) {
        if ("OK.00.00".equals(dto.getStatusCode())) {
            return "Verified";
        } else {
            return "Failed";
        }
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
            return LocalDateTime.parse(timestamp, formatter);
        } catch (Exception e) {
            log.error("Error parsing timestamp: {}", timestamp, e);
            return LocalDateTime.now();
        }
    }
}