package com.dci.clearance.inspector;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebhookLoggingFilter extends OncePerRequestFilter {

    private final WebhookLogRepository webhookLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/api/billeroo/") || path.equals("/api/merchant-callback/payment-result"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);

        // Pass the wrapped request down the chain. This allows the actual controllers to read the body.
        filterChain.doFilter(wrappedRequest, response);

        // ONLY AFTER the chain finishes, we read the cached body.
        byte[] buf = wrappedRequest.getContentAsByteArray();
        String payload = buf.length > 0 ? new String(buf, wrappedRequest.getCharacterEncoding()) : "";

        // Collect headers
        Map<String, String> headersMap = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headersMap.put(headerName, request.getHeader(headerName));
            }
        }

        String headersJson = "";
        try {
            headersJson = objectMapper.writeValueAsString(headersMap);
        } catch (Exception e) {
            log.error("Failed to serialize headers for WebhookInspector", e);
        }

        final String finalPayload = payload;
        final String finalHeaders = headersJson;
        final String method = request.getMethod();
        final String endpoint = request.getRequestURI();
        final LocalDateTime timestamp = LocalDateTime.now();

        // Save asynchronously to ensure zero performance impact on the webhook response time
        CompletableFuture.runAsync(() -> {
            try {
                WebhookLog logEntry = WebhookLog.builder()
                        .endpoint(endpoint)
                        .method(method)
                        .headers(finalHeaders)
                        .payload(finalPayload)
                        .timestamp(timestamp)
                        .build();
                webhookLogRepository.save(logEntry);
            } catch (Exception e) {
                log.error("Failed to save WebhookLog", e);
            }
        });
    }
}
