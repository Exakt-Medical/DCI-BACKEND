package com.exakt.vvip.generateVoucher.client;

import com.exakt.vvip.entity.Order;
import com.exakt.vvip.generateVoucher.dto.BillerooConfirmRequest;
import com.exakt.vvip.generateVoucher.dto.BillerooConfirmResponse;
import com.exakt.vvip.generateVoucher.dto.BillerooCountResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BillerooClient {

    private final RestTemplate restTemplate;
    private final String billerooConfirmUrl;
    private final String billerooAuthToken;

    public BillerooClient(@Qualifier("merchantCallbackRestTemplate") RestTemplate restTemplate,
                          @Value("${merchant-callback.billero-api-base-url}") String billerooConfirmUrl,
                          @Value("${merchant-callback.billero-token}") String billerooAuthToken) {
        this.restTemplate = restTemplate;
        this.billerooConfirmUrl = billerooConfirmUrl;
        this.billerooAuthToken = billerooAuthToken;
    }

    public BillerooConfirmResponse confirmPayment(Order order) {
        BillerooConfirmRequest payload = BillerooConfirmRequest.builder()
                .amountPaid(order.getOriginalAmount())
                .companyCode(order.getCompanyCode())
                .merchantReference(order.getMerchantReferenceId())
                .paymentReference(order.getPaymentReference() != null ? order.getPaymentReference() : "")
                .statusCode("OK.00.00")
                .voucherCount(order.getVoucherCount() != null ? order.getVoucherCount() : 0)
                .voucherFee(order.getVoucherFee())
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", billerooAuthToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<BillerooConfirmResponse> response = restTemplate.postForEntity(
                    billerooConfirmUrl,
                    new HttpEntity<>(payload, headers),
                    BillerooConfirmResponse.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new RuntimeException("Billeroo confirm-payment failed for order: " + order.getId());
            }

            return response.getBody();

        } catch (HttpStatusCodeException ex) {
            String body = ex.getResponseBodyAsString();
            // Billeroo returns 422 or 500 when the merchant reference was already processed
            // (NonUniqueResultException means it already exists in their DB — treat as success)
            if (isAlreadyProcessed(ex.getStatusCode().value(), body)) {
                log.warn("[BillerooClient] confirmPayment already processed for order {} — treating as success. Billeroo: {}",
                        order.getId(), body);
                return buildAlreadyProcessedResponse(order);
            }
            throw new RuntimeException(
                    "Billeroo confirm-payment failed for order " + order.getId()
                    + " [HTTP " + ex.getStatusCode().value() + "]: " + body, ex);
        }
    }

    /**
     * Returns true when Billeroo signals the payment was already confirmed:
     * - HTTP 422 (explicit "already processed" response)
     * - HTTP 500 with NonUniqueResultException (duplicate merchant reference in Billeroo DB)
     */
    private boolean isAlreadyProcessed(int statusCode, String body) {
        if (statusCode == 422) return true;
        if (body == null) return false;
        String lower = body.toLowerCase();
        return lower.contains("already been processed")
                || lower.contains("nonuniqueresultexception")
                || lower.contains("query did not return a unique result");
    }

    /**
     * Synthesises a minimal success response when Billeroo confirms the payment was
     * already recorded — allows processing to continue idempotently.
     */
    private BillerooConfirmResponse buildAlreadyProcessedResponse(Order order) {
        BillerooConfirmResponse resp = new BillerooConfirmResponse();
        resp.setStatus(200);
        resp.setMessage("Already processed");
        BillerooConfirmResponse.BillerooConfirmData data = new BillerooConfirmResponse.BillerooConfirmData();
        data.setVoucherCount(order.getVoucherCount() != null ? order.getVoucherCount() : 0);
        data.setDescription("already_processed");
        resp.setData(data);
        return resp;
    }

    public BillerooCountResponse getVoucherCount(String companyCode) {
        String baseUrl = billerooConfirmUrl.replace("/confirm-payment", "/count");
        String url = baseUrl + "?companyCode=" + companyCode; // Assuming query parameter, or could be /count/{companyCode}

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", billerooAuthToken);

        ResponseEntity<BillerooCountResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                BillerooCountResponse.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Billeroo voucher count failed for company: " + companyCode);
        }

        return response.getBody();
    }
    public void syncCompany(com.exakt.vvip.entity.Company company) {
        String url = billerooConfirmUrl.replace("/confirm-payment", "/company");

        com.exakt.vvip.generateVoucher.dto.BillerooCompanySyncRequest payload = new com.exakt.vvip.generateVoucher.dto.BillerooCompanySyncRequest();
        com.exakt.vvip.generateVoucher.dto.BillerooCompanySyncRequest.Data data = new com.exakt.vvip.generateVoucher.dto.BillerooCompanySyncRequest.Data();
        data.setCode(company.getCode());
        data.setEmail(company.getEmail());
        data.setName(company.getCompanyName());
        data.setStatus("ACTIVE".equals(company.getStatus()) ? 1 : 0);
        
        payload.setData(java.util.Collections.singletonList(data));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", billerooAuthToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                new HttpEntity<>(payload, headers),
                String.class
        );

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Billeroo company sync failed for company: " + company.getCompanyName());
        }
    }
}
