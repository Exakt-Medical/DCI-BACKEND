package com.dci.clearance.inspector;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/inspector")
@RequiredArgsConstructor
public class WebhookInspectorController {

    private final WebhookLogRepository webhookLogRepository;

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
