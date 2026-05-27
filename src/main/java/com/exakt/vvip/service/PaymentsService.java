package com.exakt.vvip.service;

import com.exakt.vvip.config.TlpeApiProperties;
import com.exakt.vvip.dto.PaymentsRequest;
import com.exakt.vvip.dto.PaymentsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class PaymentsService {

    private final RestTemplate restTemplate;
    private final TlpeApiProperties props;
    private final ObjectMapper objectMapper;

    public PaymentsService(@Qualifier("tlpeRestTemplate") RestTemplate restTemplate,
                           TlpeApiProperties props,
                           ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public PaymentsResponse createPaymentLink(PaymentsRequest request) {
        if (request == null) {
            throw new PaymentException("Request body is required");
        }

        // attach key to the outgoing request
        request.setKey(props.getAuthKey());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        // Use the configured payment URL as-is (TLPE expects token in query params)
        // But normalize token param: strip any leading "Bearer " prefix if present so the
        // query param contains the raw JWT (Postman / TLPE examples use token=<jwt>).
        String paymentUrl = props.getPayment();
        try {
            if (paymentUrl != null && paymentUrl.contains("token=")) {
                String[] parts = paymentUrl.split("token=", 2);
                String before = parts[0];
                String after = parts[1];
                String tokenPart = after.split("&", 2)[0];
                String decoded = java.net.URLDecoder.decode(tokenPart, java.nio.charset.StandardCharsets.UTF_8.name());
                // If decoded token contains a Bearer prefix, remove it
                if (decoded != null && decoded.startsWith("Bearer ")) {
                    decoded = decoded.substring("Bearer ".length());
                }
                if (decoded == null) {
                    decoded = "";
                }
                // Re-encode the cleaned token and rebuild the URL
                String encoded = java.net.URLEncoder.encode(decoded, java.nio.charset.StandardCharsets.UTF_8.name());
                String cleaned;
                if (after.contains("&")) {
                    String rest = after.substring(after.indexOf('&') + 1);
                    cleaned = before + "token=" + encoded + "&" + rest;
                } else {
                    cleaned = before + "token=" + encoded;
                }
                paymentUrl = cleaned;
            }
        } catch (Exception e) {
            log.warn("Failed to normalize token query param: {}", e.getMessage());
        }

        HttpEntity<PaymentsRequest> entity = new HttpEntity<>(request, headers);

        if (paymentUrl == null || paymentUrl.isBlank()) {
            throw new PaymentException("TLPE payment URL is not configured");
        }

        String responseBody;
        try {
            org.springframework.http.ResponseEntity<String> resp = restTemplate.exchange(
                    paymentUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            responseBody = resp.getBody();
        } catch (Exception e) {
            throw new PaymentException("TLPE payment call failed: " + e.getMessage(), e);
        }

        // Return whatever TLPE returned (JSON link or HTML page) in the PaymentsResponse.link
        return PaymentsResponse.builder().link(responseBody).build();
    }

    // Note: we now return TLPE's raw response body directly inside PaymentsResponse.link

    public static class PaymentException extends RuntimeException {
        public PaymentException(String msg) { super(msg); }
        public PaymentException(String msg, Throwable cause) { super(msg, cause); }
    }
}