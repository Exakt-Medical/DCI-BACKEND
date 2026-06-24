package com.dci.clearance.inspector;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/public/inspector")
@RequiredArgsConstructor
public class WebhookInspectorController {

    private final WebhookLogRepository webhookLogRepository;

    @GetMapping(value = "/ui", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getUi() {
        try {
            Resource resource = new ClassPathResource("static/webhook-inspector.html");
            byte[] bdata = FileCopyUtils.copyToByteArray(resource.getInputStream());
            String html = new String(bdata, StandardCharsets.UTF_8);
            return ResponseEntity.ok(html);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error loading UI");
        }
    }

    @GetMapping("/logs")
    public ResponseEntity<List<WebhookLog>> getLogs() {
        return ResponseEntity.ok(webhookLogRepository.findTop50ByOrderByTimestampDesc());
    }

    @DeleteMapping("/logs")
    public ResponseEntity<Void> clearLogs() {
        webhookLogRepository.deleteAll();
        return ResponseEntity.noContent().build();
    }
}
