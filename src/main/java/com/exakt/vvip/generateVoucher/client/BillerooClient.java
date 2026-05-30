package com.exakt.vvip.generateVoucher.client;

import com.exakt.vvip.entity.Order;
import com.exakt.vvip.generateVoucher.dto.BillerooConfirmRequest;
import com.exakt.vvip.generateVoucher.dto.BillerooConfirmResponse;
import com.exakt.vvip.generateVoucher.dto.BillerooCountResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Qualifier;

@Component
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
                .paymentReference(order.getPaymentReference())
                .statusCode("OK.00.00")
                .voucherCount(order.getVoucherCount())
                .voucherFee(order.getVoucherFee())
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", billerooAuthToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<BillerooConfirmResponse> response = restTemplate.postForEntity(
                billerooConfirmUrl,
                new HttpEntity<>(payload, headers),
                BillerooConfirmResponse.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Billeroo confirm-payment failed for order: " + order.getId());
        }

        return response.getBody();
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
}
