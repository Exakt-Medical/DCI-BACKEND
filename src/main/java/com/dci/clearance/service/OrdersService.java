package com.dci.clearance.service;

import com.dci.clearance.config.TlpeApiProperties;
import com.dci.clearance.dto.OrdersRequest;
import com.dci.clearance.dto.OrdersResponse;
import com.dci.clearance.entity.Company;
import com.dci.clearance.entity.Order;
import com.dci.clearance.entity.User;
import com.dci.clearance.repository.CompanyRepository;
import com.dci.clearance.repository.OrderRepository;
import com.dci.clearance.repository.UserRepository;
import com.dci.clearance.generateVoucher.client.BillerooClient;
import com.dci.clearance.generateVoucher.dto.BillerooPurchaseRequest;
import com.dci.clearance.generateVoucher.dto.BillerooPurchaseResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class OrdersService {

    private final RestTemplate restTemplate;
    private final TlpeApiProperties props;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final BillerooClient billerooClient;

    private static final DateTimeFormatter REF_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    public OrdersService(@Qualifier("tlpeRestTemplate") RestTemplate restTemplate,
                         TlpeApiProperties props,
                         ObjectMapper objectMapper,
                         OrderRepository orderRepository,
                         UserRepository userRepository,
                         CompanyRepository companyRepository,
                         BillerooClient billerooClient) {
        this.restTemplate = restTemplate;
        this.props = props;
        this.objectMapper = objectMapper;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.billerooClient = billerooClient;
    }

    @Transactional
    public OrdersResponse createPaymentLink(OrdersRequest request, Authentication authentication) {
        validateRequest(request);

        // ── Resolve user from JWT principal ──────────────────────────────────
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new PaymentException("Authenticated user not found: " + username));

        // ── Billeroo sync guard (citizens only) ──────────────────────────────
        if (user.getRole() == User.UserRole.CITIZEN
                && !"SYNCED".equals(user.getBillerooSyncStatus())) {
            String message = "DEAD".equals(user.getBillerooSyncStatus())
                    ? "Your account setup could not be completed. Please contact support."
                    : "Your account is not yet fully set up due to a server issue. Please try again later.";
            throw new PaymentException(message);
        }

        // ── Resolve company entity ────────────────────────────────────────────
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new PaymentException("Company not found: " + request.getCompanyId()));

        // ── Generate unique merchant reference id ─────────────────────────────
        String merchantReferenceId = generateMerchantReferenceId();

        // ── Calculate amounts ─────────────────────────────────────────────────
        BigDecimal voucherFee   = request.getVoucherFee() != null ? request.getVoucherFee() : BigDecimal.ZERO;
        int        voucherCount = request.getVoucherCount() != null ? request.getVoucherCount() : 0;
        BigDecimal originalAmount = voucherFee.multiply(BigDecimal.valueOf(voucherCount));

        // ── Insert order row as PENDING ───────────────────────────────────────
        Order order = Order.builder()
                .user(user)
                .company(company)
                .companyCode(request.getCompanyCode())
                .voucherFee(voucherFee)
                .voucherCount(voucherCount)
                .originalAmount(originalAmount)
                .processingFee(BigDecimal.ZERO)
                .totalCharged(originalAmount)
                .merchantReferenceId(merchantReferenceId)
                .status("PENDING")
                .build();
        order = orderRepository.save(order);
        log.info("Order {} created (PENDING) for user {} ref={}", order.getId(), username, merchantReferenceId);

        // ── Check Threshold ───────────────────────────────────────────────────
        if (originalAmount.compareTo(new BigDecimal("50000")) > 0) {
            String firstName = request.getCustomer() != null ? request.getCustomer().getFirstName() : user.getFirstName();
            String lastName = request.getCustomer() != null ? request.getCustomer().getLastName() : user.getLastName();
            String email = request.getCustomer() != null && request.getCustomer().getContact() != null ? request.getCustomer().getContact().getEmail() : user.getEmail();
            String mobile = request.getCustomer() != null && request.getCustomer().getContact() != null ? request.getCustomer().getContact().getMobile() : user.getMobile();

            BillerooPurchaseRequest purchaseReq = BillerooPurchaseRequest.builder()
                    .companyCode(request.getCompanyCode())
                    .companyName(company.getCompanyName())
                    .contact(mobile)
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .reference(merchantReferenceId)
                    .totalAmount(originalAmount)
                    .voucherCount(voucherCount)
                    .voucherFee(voucherFee)
                    .build();

            BillerooPurchaseResponse purchaseResp = billerooClient.createPurchaseRequest(purchaseReq);
            String invoiceRef = purchaseResp.getData().getReference();

            order.setInvoiceReference(invoiceRef);
            order.setStatus("PENDING_INVOICE_PAYMENT");
            orderRepository.save(order);
            log.info("Order {} updated to PENDING_INVOICE_PAYMENT, invoice={}", order.getId(), invoiceRef);

            return OrdersResponse.builder()
                    .orderId(order.getId())
                    .merchantReferenceId(merchantReferenceId)
                    .invoiceReference(invoiceRef)
                    .link(null)
                    .status("PENDING_INVOICE_PAYMENT")
                    .build();
        }

        // ── Inject merchant ref and auth key into TLPE payload ────────────────
        if (request.getPayment() != null) {
            request.getPayment().setMerchantReferenceId(merchantReferenceId);
        }
        request.setKey(props.getAuthKey());

        // ── Call TLPE ─────────────────────────────────────────────────────────
        String paymentUrl = resolvePaymentUrl();
        HttpEntity<OrdersRequest> entity = buildHttpEntity(request);
        String rawBody = callTlpeApi(paymentUrl, entity);
        String link = extractLink(rawBody);

        // ── Update order to PENDING_PAYMENT ───────────────────────────────────
        order.setStatus("PENDING_PAYMENT");
        orderRepository.save(order);
        log.info("Order {} updated to PENDING_PAYMENT, link generated", order.getId());

        return OrdersResponse.builder()
                .orderId(order.getId())
                .merchantReferenceId(merchantReferenceId)
                .link(link)
                .status("PENDING_PAYMENT")
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String generateMerchantReferenceId() {
        String date = LocalDateTime.now().format(REF_FORMATTER);
        String uuidSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "VVIPCTPL" + date + uuidSuffix;
    }

    private void validateRequest(OrdersRequest request) {
        if (request == null) {
            throw new PaymentException("Request body is required");
        }
        if (request.getCompanyId() == null) {
            throw new PaymentException("company_id is required");
        }
        if (request.getCompanyCode() == null || request.getCompanyCode().isBlank()) {
            throw new PaymentException("company_code is required");
        }
        if (request.getVoucherFee() == null) {
            throw new PaymentException("voucher_fee is required");
        }
        if (request.getVoucherCount() == null || request.getVoucherCount() <= 0) {
            throw new PaymentException("voucher_count must be greater than 0");
        }
    }

    private HttpEntity<OrdersRequest> buildHttpEntity(OrdersRequest request) {
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

    private String callTlpeApi(String url, HttpEntity<OrdersRequest> entity) {
        try {
            return restTemplate
                    .exchange(url, HttpMethod.POST, entity, String.class)
                    .getBody();
        } catch (Exception e) {
            throw new PaymentException("TLPE payment call failed: " + e.getMessage(), e);
        }
    }

    private String extractLink(String rawBody) {
        try {
            JsonNode outer = objectMapper.readTree(rawBody);
            String linkValue = outer.path("link").asText();
            if (linkValue == null || linkValue.isBlank()) {
                throw new PaymentException("TLPE response contained no usable link");
            }
            return linkValue;
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException("Failed to parse TLPE response: " + e.getMessage(), e);
        }
    }

    // ── Exception ─────────────────────────────────────────────────────────────

    public static class PaymentException extends RuntimeException {
        public PaymentException(String msg)                  { super(msg); }
        public PaymentException(String msg, Throwable cause) { super(msg, cause); }
    }
}