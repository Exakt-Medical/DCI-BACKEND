package com.exakt.vvip.merchantCallback.service;

import com.exakt.vvip.merchantCallback.client.TlpeClient;
import com.exakt.vvip.config.TlpeApiProperties;
import com.exakt.vvip.entity.Orders;
import com.exakt.vvip.repository.OrdersRepository;
import com.exakt.vvip.merchantCallback.config.MerchantCallbackProperties;
import com.exakt.vvip.merchantCallback.dto.BilleroConfirmResult;
import com.exakt.vvip.merchantCallback.dto.MerchantCallbackResponse;
import com.exakt.vvip.merchantCallback.dto.MerchantWebhookClaims;
import com.exakt.vvip.merchantCallback.dto.PaymentSummaryResponse;
import com.exakt.vvip.merchantCallback.dto.TransactionReport;
import com.exakt.vvip.merchantCallback.exception.MerchantCallbackException;
import com.exakt.vvip.merchantCallback.mapper.MerchantCallbackMapper;
import com.exakt.vvip.merchantCallback.util.TransactionIdValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MerchantWebhookService {

    private final TlpeClient tlpeClient;
    private final MerchantCallbackProperties properties;
    private final TlpeApiProperties tlpeApiProperties;
    private final BilleroVoucherService billeroVoucherService;
    private final ObjectMapper objectMapper;
    private final MerchantWebhookStreamService streamService;
    private final OrdersRepository ordersRepository;

    private final Set<String> processedWebhookKeys = ConcurrentHashMap.newKeySet();

    public MerchantCallbackResponse processWebhook(String authorizationHeader, byte[] rawBody) {
        validateAuthorizationHeader(authorizationHeader);

        String compactJwt = normalizeJwt(rawBody);
        Claims claims = parseAndVerifyJwt(compactJwt);
        MerchantWebhookClaims webhookClaims = toWebhookClaims(claims);

        validateClaims(webhookClaims);
        String transactionId = TransactionIdValidator.normalize(webhookClaims.getData().getTransactionId());
        registerWebhook(buildWebhookKey(webhookClaims.getJti(), transactionId));

        TransactionReport report = toTransactionReport(webhookClaims, claims);

        // ── Step 2: set PAYMENT_VERIFYING (idempotency guard: only if PENDING_PAYMENT) ──
        String merchantRef = report.getMerchantReference();
        Orders order = findOrderByRef(merchantRef);
        if (order != null) {
            if ("PENDING_PAYMENT".equals(order.getStatus())) {
                order.setTlpeTransactionId(report.getTransactionId());
                order.setStatus("PAYMENT_VERIFYING");
                ordersRepository.save(order);
                System.out.println("Order " + order.getId() + " → PAYMENT_VERIFYING");
            } else {
                System.out.println("Order " + order.getId() + " skipped PAYMENT_VERIFYING update (current status=" + order.getStatus() + ")");
            }
        }

        // ── Step 3: Billeroo confirm (best-effort) + set PAYMENT_CONFIRMED ───
        // PAYMENT_CONFIRMED is gated on OK.00.00 status from TLPE, NOT on Billeroo success.
        BilleroConfirmResult confirmResult = confirmPayment(report);
        if (order != null && report.isSuccess() && "PAYMENT_VERIFYING".equals(order.getStatus())) {
            java.math.BigDecimal processingFee = report.getProcessingFee() != null
                    ? report.getProcessingFee() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal totalCharged = order.getOriginalAmount().add(processingFee);

            order.setPaymentReference(report.getPaymentReference());
            order.setProcessingFee(processingFee);
            order.setTotalCharged(totalCharged);
            order.setStatus("PAYMENT_CONFIRMED");
            ordersRepository.save(order);
            System.out.println("Order " + order.getId() + " → PAYMENT_CONFIRMED, processingFee=" + processingFee);
        }

        PaymentSummaryResponse summary = MerchantCallbackMapper.toPaymentSummary(report, confirmResult);

        MerchantCallbackResponse response = MerchantCallbackResponse.builder()
                .success(true)
                .message("Merchant webhook processed successfully")
                .data(summary)
                .build();

        streamService.publish(transactionId, response);

        return response;
    }

    private BilleroConfirmResult confirmPayment(TransactionReport report) {
        try {
            return billeroVoucherService.confirmPayment(report);
        } catch (MerchantCallbackException exception) {
            return null;
        }
    }

    private Orders findOrderByRef(String merchantRef) {
        if (merchantRef == null || merchantRef.isBlank()) {
            return null;
        }
        return ordersRepository.findByMerchantReferenceId(merchantRef).orElse(null);
    }

    private void validateAuthorizationHeader(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            throw new MerchantCallbackException(HttpStatus.UNAUTHORIZED,
                    "missing_authorization",
                    "Authorization header is required for merchant webhook");
        }

        String expectedAuthKey = tlpeApiProperties.getAuthKey();
        if (!StringUtils.hasText(expectedAuthKey)) {
            throw new MerchantCallbackException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "tlpe_not_configured",
                    "TLPE auth key is not configured");
        }

        String trimmedHeader = authorizationHeader.trim();
        if (!expectedAuthKey.equals(trimmedHeader)) {
            throw new MerchantCallbackException(HttpStatus.UNAUTHORIZED,
                    "invalid_authorization",
                    "Authorization header is invalid");
        }
    }

    private String normalizeJwt(byte[] rawBody) {
        if (rawBody == null || rawBody.length == 0) {
            throw new MerchantCallbackException(HttpStatus.BAD_REQUEST,
                    "missing_payload",
                    "Webhook payload is required");
        }

        String trimmed = new String(rawBody, StandardCharsets.UTF_8).trim();
        try {
            JsonNode root = objectMapper.readTree(trimmed);
            JsonNode payloadNode = root.path("payload");
            if (payloadNode.isMissingNode() || payloadNode.isNull() || !payloadNode.isTextual()) {
                throw new MerchantCallbackException(HttpStatus.BAD_REQUEST,
                        "invalid_payload",
                        "Webhook payload must contain a string field named payload");
            }
            String payload = payloadNode.asText().trim();
            if (!StringUtils.hasText(payload)) {
                throw new MerchantCallbackException(HttpStatus.BAD_REQUEST,
                        "invalid_payload",
                        "Webhook payload must contain a non-empty JWT string");
            }
            return payload;
        } catch (MerchantCallbackException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new MerchantCallbackException(HttpStatus.BAD_REQUEST,
                    "invalid_payload",
                    "Webhook payload is not valid JSON with a payload field",
                    exception.getMessage(),
                    exception);
        }
    }

    private Claims parseAndVerifyJwt(String compactJwt) {
        String signingSecret = properties.getWebhookSigningSecret();
        if (!StringUtils.hasText(signingSecret)) {
            throw new MerchantCallbackException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "webhook_not_configured",
                    "Merchant webhook signing secret is not configured");
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(signingSecret.getBytes(StandardCharsets.UTF_8));
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .clockSkewSeconds(properties.getAllowedClockSkewSeconds())
                    .build()
                    .parseSignedClaims(compactJwt);

            return jws.getPayload();
        } catch (JwtException exception) {
            throw new MerchantCallbackException(HttpStatus.UNAUTHORIZED,
                    "invalid_jwt",
                    "Webhook JWT could not be verified",
                    exception.getMessage(),
                    exception);
        } catch (Exception exception) {
            throw new MerchantCallbackException(HttpStatus.UNAUTHORIZED,
                    "invalid_jwt",
                    "Webhook JWT could not be verified",
                    exception.getMessage(),
                    exception);
        }
    }

    private MerchantWebhookClaims toWebhookClaims(Claims claims) {
        try {
            JsonNode claimsNode = objectMapper.valueToTree(claims);
            return objectMapper.treeToValue(claimsNode, MerchantWebhookClaims.class);
        } catch (Exception exception) {
            throw new MerchantCallbackException(HttpStatus.BAD_REQUEST,
                    "invalid_webhook_claims",
                    "Webhook claims could not be parsed",
                    exception.getMessage(),
                    exception);
        }
    }

    private void validateClaims(MerchantWebhookClaims webhookClaims) {
        if (webhookClaims == null) {
            throw new MerchantCallbackException(HttpStatus.BAD_REQUEST,
                    "invalid_webhook_claims",
                    "Webhook claims are missing");
        }

        if (!StringUtils.hasText(webhookClaims.getIss())) {
            throw new MerchantCallbackException(HttpStatus.BAD_REQUEST,
                    "invalid_webhook_claims",
                    "Webhook issuer is required");
        }
        if (!StringUtils.hasText(webhookClaims.getSub())) {
            throw new MerchantCallbackException(HttpStatus.BAD_REQUEST,
                    "invalid_webhook_claims",
                    "Webhook subject is required");
        }
        List<String> audiences = resolveAudience(webhookClaims.getAud());
        if (audiences.isEmpty()) {
            throw new MerchantCallbackException(HttpStatus.BAD_REQUEST,
                    "invalid_webhook_claims",
                    "Webhook audience is required");
        }
        if (!StringUtils.hasText(webhookClaims.getJti())) {
            throw new MerchantCallbackException(HttpStatus.BAD_REQUEST,
                    "invalid_webhook_claims",
                    "Webhook jti is required");
        }
        if (webhookClaims.getExp() == null) {
            throw new MerchantCallbackException(HttpStatus.BAD_REQUEST,
                    "invalid_webhook_claims",
                    "Webhook exp is required");
        }
        if (webhookClaims.getIat() == null) {
            throw new MerchantCallbackException(HttpStatus.BAD_REQUEST,
                    "invalid_webhook_claims",
                    "Webhook iat is required");
        }
        if (webhookClaims.getData() == null || !StringUtils.hasText(webhookClaims.getData().getTransactionId())) {
            throw new MerchantCallbackException(HttpStatus.BAD_REQUEST,
                    "invalid_webhook_claims",
                    "Webhook transaction_id is required");
        }

        if (StringUtils.hasText(properties.getExpectedIssuer())
                && !properties.getExpectedIssuer().equals(webhookClaims.getIss())) {
            throw new MerchantCallbackException(HttpStatus.UNAUTHORIZED,
                    "invalid_issuer",
                    "Webhook issuer is invalid");
        }
        if (StringUtils.hasText(properties.getExpectedSubject())
                && !properties.getExpectedSubject().equals(webhookClaims.getSub())) {
            throw new MerchantCallbackException(HttpStatus.UNAUTHORIZED,
                    "invalid_subject",
                    "Webhook subject is invalid");
        }
        if (StringUtils.hasText(properties.getExpectedAudience())
            && audiences.stream().noneMatch(properties.getExpectedAudience()::equals)) {
            throw new MerchantCallbackException(HttpStatus.UNAUTHORIZED,
                    "invalid_audience",
                    "Webhook audience is invalid");
        }

        long nowSeconds = System.currentTimeMillis() / 1000;
        if (webhookClaims.getExp() < nowSeconds) {
            throw new MerchantCallbackException(HttpStatus.UNAUTHORIZED,
                    "webhook_expired",
                    "Webhook JWT has expired");
        }
        if (webhookClaims.getIat() > nowSeconds + properties.getAllowedClockSkewSeconds()) {
            throw new MerchantCallbackException(HttpStatus.UNAUTHORIZED,
                    "webhook_not_yet_valid",
                    "Webhook JWT is not yet valid");
        }
    }

    private void registerWebhook(String webhookKey) {
        if (!processedWebhookKeys.add(webhookKey)) {
            throw new MerchantCallbackException(HttpStatus.CONFLICT,
                    "duplicate_webhook",
                    "This webhook transaction was already processed");
        }
    }

    private List<String> resolveAudience(Object audClaim) {
        List<String> audiences = new ArrayList<>();
        if (audClaim == null) {
            return audiences;
        }
        if (audClaim instanceof String audString) {
            if (StringUtils.hasText(audString)) {
                audiences.add(audString);
            }
            return audiences;
        }
        if (audClaim instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item == null) {
                    continue;
                }
                String value = item.toString();
                if (StringUtils.hasText(value)) {
                    audiences.add(value);
                }
            }
            return audiences;
        }

        String value = audClaim.toString();
        if (StringUtils.hasText(value)) {
            audiences.add(value);
        }
        return audiences;
    }

    private String buildWebhookKey(String jti, String transactionId) {
        return (jti + "::" + transactionId).toLowerCase(Locale.ROOT);
    }

    private TransactionReport toTransactionReport(MerchantWebhookClaims webhookClaims, Claims claims) {
        MerchantWebhookClaims.Data data = webhookClaims.getData();
        MerchantWebhookClaims.Payment payment = data.getPayment();
        MerchantWebhookClaims.Result result = data.getResult();
        MerchantWebhookClaims.CustomParameters customParameters = data.getCustomParameters();

        BigDecimal amountPaid = resolveAmount(payment, customParameters);
        String merchantReference = payment == null ? null : payment.getMerchantReferenceId();
        String paymentReference = resolvePaymentReference(payment, data.getTransactionId());
        String companyCode = customParameters == null ? null : customParameters.getCompanyCode();
        String companyName = customParameters == null ? null : customParameters.getCompanyName();
        Integer voucherCount = customParameters == null ? null : customParameters.getVoucherCount();
        BigDecimal voucherFee = customParameters == null ? null : customParameters.getVoucherFee();
        BigDecimal processingFee = customParameters == null ? null : customParameters.getEplAddOnFee();
        String statusCode = result == null ? null : result.getStatusCode();
        String voucherDescription = result == null ? null : result.getMessage();
        String message = result == null ? null : result.getMessage();

        MerchantWebhookClaims.Customer customer = data.getCustomer();
        String firstName = customer == null ? null : customer.getFirstName();
        String lastName = customer == null ? null : customer.getLastName();
        String contactMobile = (customer == null || customer.getContact() == null) ? null : customer.getContact().getMobile();
        String email = (customer == null || customer.getContact() == null) ? null : customer.getContact().getEmail();

        return TransactionReport.builder()
                .success(isSuccessfulStatus(statusCode, message))
                .transactionId(TransactionIdValidator.normalize(data.getTransactionId()))
                .amountPaid(amountPaid)
                .processingFee(processingFee)
                .merchantReference(merchantReference)
                .paymentReference(paymentReference)
                .companyCode(companyCode)
                .voucherCount(voucherCount)
                .voucherFee(voucherFee)
                .statusCode(statusCode)
                .voucherDescription(voucherDescription)
                .message(message)
                .rawResponse(objectMapper.valueToTree(claims))
                .firstName(firstName)
                .lastName(lastName)
                .contactMobile(contactMobile)
                .email(email)
                .companyName(companyName)
                .build();
    }

    private BigDecimal resolveAmount(MerchantWebhookClaims.Payment payment, MerchantWebhookClaims.CustomParameters customParameters) {
        if (customParameters != null && customParameters.getOriginalAmount() != null) {
            return customParameters.getOriginalAmount();
        }
        if (payment != null && payment.getAmount() != null) {
            return payment.getAmount();
        }
        return BigDecimal.ZERO;
    }

    private String resolvePaymentReference(MerchantWebhookClaims.Payment payment, String transactionId) {
        // Martin.md: payment_reference = processor_reference_id from TLPE (e.g. 8ac7a4a19e771a1b019e77b7f5550cbb)
        if (payment != null && StringUtils.hasText(payment.getProcessorReferenceId())) {
            return payment.getProcessorReferenceId();
        }

        if (payment == null) {
            return null;
        }

        List<String> candidates = new ArrayList<>();
        if (payment.getOtherReferences() != null) {
            candidates.addAll(payment.getOtherReferences());
        }
        if (StringUtils.hasText(payment.getMerchantReferenceId())) {
            candidates.add(payment.getMerchantReferenceId());
        }

        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate;
            }
        }

        // Fall back to transactionId only if nothing else is available
        return StringUtils.hasText(transactionId) ? transactionId : null;
    }

    private boolean isSuccessfulStatus(String statusCode, String message) {
        if (StringUtils.hasText(statusCode) && statusCode.toUpperCase(Locale.ROOT).startsWith("OK")) {
            return true;
        }
        return StringUtils.hasText(message) && message.toLowerCase(Locale.ROOT).contains("successful");
    }
}