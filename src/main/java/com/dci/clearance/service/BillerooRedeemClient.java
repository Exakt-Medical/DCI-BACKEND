package com.dci.clearance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Component
public class BillerooRedeemClient {
    @Value("${merchant-callback.billero-redeem-url}")
    private String redeemUrl;

    @Value("${merchant-callback.billero-token}")
    private String token;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String redeem(String transactionReference, String companyCode) {
        String url = redeemUrl;
        String body = String.format(
                "{\"transactionReference\":\"%s\",\"companyCode\":\"%s\",\"voucherCount\":\"1\"}",
                transactionReference, companyCode
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-Type", "application/json")
                    .header("Authorization", token)
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            log.info("Billeroo redeem response status={} body={}",
                    response.statusCode(), response.body());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                // voucherReference is an array — get first element
                JsonNode voucherRefNode = root.path("data").path("voucherReference");
                if (voucherRefNode.isArray() && voucherRefNode.size() > 0) {
                    return voucherRefNode.get(0).asText(null);
                }
                log.warn("voucherReference array missing or empty in Billeroo response");
                return null;
            } else {
                log.error("Billeroo redeem failed: status={} body={}",
                        response.statusCode(), response.body());
                return null;
            }

        } catch (Exception e) {
            log.error("Billeroo redeem exception: {}", e.getMessage(), e);
            return null;
        }
    }
}