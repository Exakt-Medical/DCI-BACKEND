package com.exakt.vvip.service;

import com.exakt.vvip.config.TlpeApiProperties;
import com.exakt.vvip.dto.AddPaymentRequest;
import com.exakt.vvip.dto.AddPaymentResponse;
import com.exakt.vvip.dto.PaymentsRequest;
import com.exakt.vvip.dto.PaymentsResponse;
import com.exakt.vvip.entity.Payments;
import com.exakt.vvip.entity.Purchase;
import com.exakt.vvip.repository.PaymentsRepository;
import com.exakt.vvip.repository.PurchaseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PaymentsService {

    private final RestTemplate restTemplate;
    private final TlpeApiProperties props;
    private final ObjectMapper objectMapper;
    private final PaymentsRepository paymentsRepository;
    private final PurchaseRepository purchaseRepository;

    public PaymentsService(@Qualifier("tlpeRestTemplate") RestTemplate restTemplate,
                           TlpeApiProperties props,
                           ObjectMapper objectMapper,
                           PaymentsRepository paymentsRepository,
                           PurchaseRepository purchaseRepository) {
        this.restTemplate = restTemplate;
        this.props = props;
        this.objectMapper = objectMapper;
        this.paymentsRepository = paymentsRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional
    public AddPaymentResponse addPayment(AddPaymentRequest request) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String merchantRefId = "VVIPCTPL" + timestamp + randomSuffix;
        log.info("Adding payment for transactionId: {}, generated merchantRefId: {}", request.getTransactionId(), merchantRefId);

        Purchase purchase = null;
        if (request.getPurchaseId() != null) {
            purchase = purchaseRepository.findById(request.getPurchaseId())
                    .orElseThrow(() -> new PaymentException("Purchase not found with id: " + request.getPurchaseId()));
        }

        Payments payment = Payments.builder()
                .transactionId(request.getTransactionId())
                .merchantRefId(merchantRefId)
                .status("PENDING")
                .purchase(purchase)
                .build();

        Payments savedPayment = paymentsRepository.save(payment);

        return AddPaymentResponse.builder()
                .transactionId(savedPayment.getTransactionId())
                .merchantRefId(savedPayment.getMerchantRefId())
                .paymentStatus(savedPayment.getStatus())
                .purchaseId(savedPayment.getPurchase() != null ? savedPayment.getPurchase().getId() : null)
                .message("Payment added successfully")
                .build();
    }

    @Transactional
    public AddPaymentResponse confirmPayment(Long transactionId) {
        log.info("Confirming payment for transactionId: {}", transactionId);

        Payments payment = paymentsRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new PaymentException("Payment not found for transactionId: " + transactionId));

        payment.setStatus("SUCCESS");
        Payments savedPayment = paymentsRepository.save(payment);

        return AddPaymentResponse.builder()
                .transactionId(savedPayment.getTransactionId())
                .merchantRefId(savedPayment.getMerchantRefId())
                .paymentStatus(savedPayment.getStatus())
                .purchaseId(savedPayment.getPurchase() != null ? savedPayment.getPurchase().getId() : null)
                .message("Payment confirmed successfully")
                .build();
    }

    public PaymentsResponse createPaymentLink(PaymentsRequest request) {
        validateRequest(request);

        request.setKey(props.getAuthKey());

        String paymentUrl = resolvePaymentUrl();
        HttpEntity<PaymentsRequest> entity = buildHttpEntity(request);
        String rawBody = callTlpeApi(paymentUrl, entity);

        return extractResponse(rawBody);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void validateRequest(PaymentsRequest request) {
        if (request == null) {
            throw new PaymentException("Request body is required");
        }
    }

    private HttpEntity<PaymentsRequest> buildHttpEntity(PaymentsRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(request, headers);
    }

    private String resolvePaymentUrl() {
        String url = props.getPayment();
        if (url == null || url.isBlank()) {
            throw new PaymentException("TLPE payment URL is not configured");
        }
        return stripBearerFromTokenParam(url);
    }

    /**
     * TLPE expects a raw JWT in the {@code token} query parameter.
     * If the URL was built with a "Bearer <jwt>" value, strip the prefix.
     */
    private String stripBearerFromTokenParam(String url) {
        if (!url.contains("token=")) {
            return url;
        }
        try {
            int tokenStart = url.indexOf("token=") + "token=".length();
            int tokenEnd   = url.indexOf('&', tokenStart);

            String encoded = tokenEnd == -1
                    ? url.substring(tokenStart)
                    : url.substring(tokenStart, tokenEnd);

            String decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            if (decoded.startsWith("Bearer ")) {
                decoded = decoded.substring("Bearer ".length());
            }

            String reEncoded = URLEncoder.encode(decoded, StandardCharsets.UTF_8);
            String suffix    = tokenEnd == -1 ? "" : url.substring(tokenEnd);

            return url.substring(0, tokenStart) + reEncoded + suffix;

        } catch (Exception e) {
            log.warn("Could not normalize token query param, using URL as-is: {}", e.getMessage());
            return url;
        }
    }

    private String callTlpeApi(String url, HttpEntity<PaymentsRequest> entity) {
        try {
            return restTemplate
                    .exchange(url, HttpMethod.POST, entity, String.class)
                    .getBody();
        } catch (Exception e) {
            throw new PaymentException("TLPE payment call failed: " + e.getMessage(), e);
        }
    }

    /**
     * TLPE returns a single-level JSON response:
     * {"link": "https://..."}
     *
     * We parse the outer JSON and read the link field directly.
     */
    private PaymentsResponse extractResponse(String rawBody) {
        try {
            JsonNode outer = objectMapper.readTree(rawBody);
            String linkValue = outer.path("link").asText();

            if (linkValue.isBlank()) {
                throw new PaymentException("TLPE response contained no usable link");
            }

            return PaymentsResponse.builder().link(linkValue).build();

        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException("Failed to parse TLPE response: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Exception
    // -------------------------------------------------------------------------

    public static class PaymentException extends RuntimeException {
        public PaymentException(String msg)                  { super(msg); }
        public PaymentException(String msg, Throwable cause) { super(msg, cause); }
    }
}