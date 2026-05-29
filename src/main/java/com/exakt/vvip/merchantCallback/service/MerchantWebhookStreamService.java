package com.exakt.vvip.merchantCallback.service;

import com.exakt.vvip.merchantCallback.dto.MerchantCallbackResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MerchantWebhookStreamService {

    private static final long DEFAULT_TIMEOUT_MS = 5 * 60 * 1000L;
    private static final long CACHE_TTL_MS = 60 * 1000L;

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    public SseEmitter register(String transactionId) {
        pruneCache();
        CachedResult cached = cache.get(transactionId);
        if (cached != null && !cached.isExpired()) {
            return emitImmediately(cached.response());
        }

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
        emitters.computeIfAbsent(transactionId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(transactionId, emitter));
        emitter.onTimeout(() -> {
            removeEmitter(transactionId, emitter);
            emitter.complete();
        });
        emitter.onError(error -> removeEmitter(transactionId, emitter));

        return emitter;
    }

    public void publish(String transactionId, MerchantCallbackResponse response) {
        pruneCache();
        cache.put(transactionId, new CachedResult(response, System.currentTimeMillis() + CACHE_TTL_MS));

        List<SseEmitter> listeners = emitters.get(transactionId);
        if (listeners == null || listeners.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : listeners) {
            try {
                emitter.send(SseEmitter.event()
                        .name("payment-result")
                        .data(response));
                emitter.complete();
            } catch (Exception ex) {
                try {
                    emitter.completeWithError(ex);
                } catch (Exception ignored) {
                    // Ignore IllegalStateException if AsyncContext is already complete
                }
            } finally {
                removeEmitter(transactionId, emitter);
            }
        }
    }

    private void removeEmitter(String transactionId, SseEmitter emitter) {
        List<SseEmitter> listeners = emitters.get(transactionId);
        if (listeners == null) {
            return;
        }

        listeners.remove(emitter);
        if (listeners.isEmpty()) {
            emitters.remove(transactionId);
        }
    }

    private SseEmitter emitImmediately(MerchantCallbackResponse response) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
        try {
            emitter.send(SseEmitter.event()
                    .name("payment-result")
                    .data(response));
            emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    private void pruneCache() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private record CachedResult(MerchantCallbackResponse response, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
