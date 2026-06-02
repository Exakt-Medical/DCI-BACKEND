package com.dci.clearance.merchantCallback.client;

import com.dci.clearance.merchantCallback.config.MerchantCallbackProperties;
import com.dci.clearance.merchantCallback.dto.TransactionReport;
import com.dci.clearance.merchantCallback.exception.MerchantCallbackException;
import com.dci.clearance.merchantCallback.util.TransactionIdValidator;
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

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class TlpeClient {

    private final RestTemplate restTemplate;
    private final MerchantCallbackProperties properties;
    private final ObjectMapper objectMapper;

    public TlpeClient(@Qualifier("merchantCallbackRestTemplate") RestTemplate restTemplate,
                      MerchantCallbackProperties properties,
                      ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public TransactionReport fetchReport(String transactionId) {
        String normalizedTransactionId = TransactionIdValidator.normalize(transactionId);
        String responseBody;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    buildUrl(normalizedTransactionId),
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders(properties.getIntegratorToken())),
                    String.class);
            responseBody = response.getBody();
        } catch (HttpStatusCodeException exception) {
            throw new MerchantCallbackException(resolveStatus(exception),
                    "tlpe_request_failed",
                    "Failed to verify transaction with TLPE",
                    safeBody(exception.getResponseBodyAsString()),
                    exception);
        }

        if (!StringUtils.hasText(responseBody)) {
            throw new MerchantCallbackException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "tlpe_empty_response",
                    "TLPE returned an empty response");
        }

        return parseReport(normalizedTransactionId, responseBody);
    }

    private String buildUrl(String transactionId) {
        String baseUrl = trimTrailingSlash(properties.getTlpeApiBaseUrl());
        if (!StringUtils.hasText(baseUrl)) {
            throw new MerchantCallbackException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "tlpe_not_configured",
                    "TLPE API base URL is not configured");
        }
        return baseUrl + "/report/" + URLEncoder.encode(transactionId, StandardCharsets.UTF_8);
    }

    private HttpHeaders createHeaders(String token) {
        if (!StringUtils.hasText(token)) {
            throw new MerchantCallbackException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "tlpe_not_configured",
                    "TLPE integrator token is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, token);
        return headers;
    }

    private TransactionReport parseReport(String transactionId, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode payload = extractPayload(root);

            if (isExplicitFailure(root, payload)) {
                throw new MerchantCallbackException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                        "tlpe_report_rejected",
                        firstText(payload, root, "message", "error", "description"),
                        responseBody);
            }

            // Resolve status code first so we can decide success/failure
            String resolvedStatusCode = firstText(payload, root,
                    "status_code", "statusCode", "payment.status_code",
                    "result.statusCode", "status", "result.status_code");

            // Any status code beginning with "ER" (e.g. ER.00.00) is a payment failure
            boolean isFailedStatusCode = resolvedStatusCode != null
                    && resolvedStatusCode.toUpperCase(java.util.Locale.ROOT).startsWith("ER");

            return TransactionReport.builder()
                    .success(!isFailedStatusCode)
                    .transactionId(transactionId)
                    .amountPaid(readBigDecimal(payload, root,
                            "custom_parameters.original_amount", "payment.amount",
                            "amount_paid", "amount", "total_amount"))
                    .merchantReference(firstText(payload, root,
                            "merchant_reference_id", "merchant_reference",
                            "merchantReferenceId", "merchantReference",
                            "payment.merchant_reference_id", "payment.merchantReferenceId",
                            "reference", "merchant.ref"))
                    .paymentReference(firstText(payload, root,
                            "processor_reference_id", "processorReferenceId",
                            "payment_reference", "paymentReference",
                            "payment.processor_reference_id", "payment.reference",
                            "reference_no"))
                    .companyCode(firstText(payload, root,
                            "company_code", "companyCode",
                            "merchant.company_code", "custom_parameters.company_code"))
                    .voucherCount(readInteger(payload, root,
                            "voucher_count", "voucherCount", "custom_parameters.voucher_count"))
                    .voucherFee(readBigDecimal(payload, root,
                            "voucher_fee", "voucherFee", "custom_parameters.voucher_fee"))
                    .statusCode(resolvedStatusCode)
                    .voucherDescription(firstText(payload, root,
                            "voucher_description", "voucherDescription", "description", "message"))
                    .message(firstText(payload, root, "message", "status_message"))
                    .rawResponse(root)
                    .build();
        } catch (MerchantCallbackException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new MerchantCallbackException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "tlpe_invalid_response",
                    "TLPE returned an invalid response",
                    responseBody,
                    exception);
        }
    }

    private JsonNode extractPayload(JsonNode root) {
        if (root == null) {
            return null;
        }

        JsonNode dataNode = root.path("data");
        if (!dataNode.isMissingNode() && !dataNode.isNull()) {
            return dataNode;
        }

        JsonNode resultNode = root.path("result");
        if (!resultNode.isMissingNode() && !resultNode.isNull()) {
            return resultNode;
        }

        return root;
    }

    private boolean isExplicitFailure(JsonNode root, JsonNode payload) {
        String successValue = firstText(payload, root, "success");
        return StringUtils.hasText(successValue) && "false".equalsIgnoreCase(successValue);
    }

    private String firstText(JsonNode primary, JsonNode secondary, String... paths) {
        String value = firstText(primary, paths);
        if (StringUtils.hasText(value)) {
            return value;
        }
        return firstText(secondary, paths);
    }

    private String firstText(JsonNode node, String... paths) {
        if (node == null) {
            return null;
        }

        for (String path : paths) {
            JsonNode resolved = resolvePath(node, path);
            if (resolved != null && !resolved.isNull()) {
                String text = resolved.asText();
                if (StringUtils.hasText(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    private BigDecimal readBigDecimal(JsonNode primary, JsonNode secondary, String... paths) {
        String value = firstText(primary, paths);
        if (!StringUtils.hasText(value)) {
            value = firstText(secondary, paths);
        }

        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return new BigDecimal(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer readInteger(JsonNode primary, JsonNode secondary, String... paths) {
        String value = firstText(primary, paths);
        if (!StringUtils.hasText(value)) {
            value = firstText(secondary, paths);
        }

        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return Integer.valueOf(value.trim());
        } catch (Exception ignored) {
            return null;
        }
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

    private String trimTrailingSlash(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String safeBody(String body) {
        return StringUtils.hasText(body) ? body : null;
    }

    private org.springframework.http.HttpStatus resolveStatus(HttpStatusCodeException exception) {
        return exception.getStatusCode() instanceof org.springframework.http.HttpStatus status ? status : org.springframework.http.HttpStatus.BAD_GATEWAY;
    }
}