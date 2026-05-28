package com.exakt.vvip.merchantCallback.client;

import com.exakt.vvip.merchantCallback.config.MerchantCallbackProperties;
import com.exakt.vvip.merchantCallback.dto.BilleroConfirmRequest;
import com.exakt.vvip.merchantCallback.dto.BilleroConfirmResult;
import com.exakt.vvip.merchantCallback.exception.MerchantCallbackException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Component
public class BilleroClient {

    private static final String CONFIRM_PAYMENT_PATH = "/voucher/confirm-payment";

    private final RestTemplate restTemplate;
    private final MerchantCallbackProperties properties;
    private final ObjectMapper objectMapper;

    public BilleroClient(@Qualifier("merchantCallbackRestTemplate") RestTemplate restTemplate,
                         MerchantCallbackProperties properties,
                         ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public BilleroConfirmResult confirmPayment(BilleroConfirmRequest request) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    buildConfirmUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(request, createHeaders(properties.getBilleroToken())),
                    String.class);

            return buildResult(response.getBody(), response.getStatusCode().value(), false);
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().value() == 422) {
                return buildResult(exception.getResponseBodyAsString(), 422, true);
            }

            throw new MerchantCallbackException(resolveStatus(exception),
                    "billero_request_failed",
                    "Failed to confirm payment with Billero",
                    exception.getResponseBodyAsString(),
                    exception);
        }
    }

    private String buildConfirmUrl() {
        String baseUrl = trimTrailingSlash(properties.getBilleroApiBaseUrl());
        if (!StringUtils.hasText(baseUrl)) {
            throw new MerchantCallbackException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "billero_not_configured",
                    "Billero API base URL is not configured");
        }

        if (baseUrl.endsWith(CONFIRM_PAYMENT_PATH)) {
            return baseUrl;
        }

        return baseUrl + CONFIRM_PAYMENT_PATH;
    }

    private HttpHeaders createHeaders(String token) {
        if (!StringUtils.hasText(token)) {
            throw new MerchantCallbackException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "billero_not_configured",
                    "Billero token is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, token);
        return headers;
    }

    private BilleroConfirmResult buildResult(String body, int statusCode, boolean alreadyProcessed) {
        try {
            JsonNode root = StringUtils.hasText(body) ? objectMapper.readTree(body) : null;
            String message = text(root, "message", "description", "status_message");
            String error = text(root, "error", "raw_error");
            boolean processed = alreadyProcessed || containsAlreadyProcessed(message) || containsAlreadyProcessed(error);

            return BilleroConfirmResult.builder()
                    .success((statusCode >= 200 && statusCode < 300) || processed)
                    .voucherAlreadyProcessed(processed)
                    .statusCode(statusCode)
                    .message(message)
                    .error(error)
                    .rawResponse(root)
                    .build();
        } catch (Exception exception) {
            return BilleroConfirmResult.builder()
                    .success((statusCode >= 200 && statusCode < 300) || alreadyProcessed)
                    .voucherAlreadyProcessed(alreadyProcessed)
                    .statusCode(statusCode)
                    .rawError(body)
                    .build();
        }
    }

    private String text(JsonNode node, String... paths) {
        if (node == null) {
            return null;
        }

        for (String path : paths) {
            JsonNode resolved = resolvePath(node, path);
            if (resolved != null && !resolved.isNull()) {
                String value = resolved.asText();
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private JsonNode resolvePath(JsonNode node, String path) {
        JsonNode current = node;
        for (String segment : path.split("\\.")) {
            if (current == null) {
                return null;
            }
            current = current.path(segment);
            if (current.isMissingNode()) {
                return null;
            }
        }
        return current;
    }

    private boolean containsAlreadyProcessed(String message) {
        return StringUtils.hasText(message) && message.toLowerCase().contains("already been processed");
    }

    private String trimTrailingSlash(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private org.springframework.http.HttpStatus resolveStatus(HttpStatusCodeException exception) {
        return exception.getStatusCode() instanceof org.springframework.http.HttpStatus status ? status : org.springframework.http.HttpStatus.BAD_GATEWAY;
    }
}