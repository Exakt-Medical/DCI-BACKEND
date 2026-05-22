package com.exakt.vvip.controller;

import com.exakt.vvip.dto.NotificationRequest;
import com.exakt.vvip.dto.NotificationResponse;
import com.exakt.vvip.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Notifications", description = "Notification CRUD operations")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List all notifications")
    public ResponseEntity<List<NotificationResponse>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<NotificationResponse> getById(@PathVariable Long id) {
        NotificationResponse record = notificationService.getById(id);
        if (record == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(record);
    }

    @PostMapping("")
    @Operation(summary = "Create a new notification")
    public ResponseEntity<?> create(@RequestBody NotificationRequest request, Authentication auth) {
        try {
            String role = auth.getAuthorities().stream()
                    .findFirst()
                    .map(g -> g.getAuthority().replace("ROLE_", ""))
                    .orElse("");
            return ResponseEntity.ok(notificationService.create(request, auth.getName(), role));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing notification")
    public ResponseEntity<NotificationResponse> update(@PathVariable Long id, @RequestBody NotificationRequest request, Authentication auth) {
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(g -> g.getAuthority().replace("ROLE_", ""))
                .orElse("");
        NotificationResponse record = notificationService.update(id, request, auth.getName(), role);
        if (record == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(record);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
