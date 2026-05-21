package com.exakt.vvip.controller;

import com.exakt.vvip.dto.AuditTrailRequest;
import com.exakt.vvip.dto.AuditTrailResponse;
import com.exakt.vvip.service.AuditTrailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-trail")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Audit Trail", description = "Audit trail CRUD operations")
public class AuditTrailController {

    private final AuditTrailService auditTrailService;

    @GetMapping
    @Operation(summary = "List all audit trail records")
    public ResponseEntity<List<AuditTrailResponse>> getAll() {
        return ResponseEntity.ok(auditTrailService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get audit trail by ID")
    public ResponseEntity<AuditTrailResponse> getById(@PathVariable Long id) {
        AuditTrailResponse record = auditTrailService.getById(id);
        if (record == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(record);
    }

    @PostMapping
    @Operation(summary = "Create a new audit trail record")
    public ResponseEntity<AuditTrailResponse> create(@RequestBody AuditTrailRequest request, Authentication auth) {
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(g -> g.getAuthority().replace("ROLE_", ""))
                .orElse("");
        return ResponseEntity.ok(auditTrailService.create(request, auth.getName(), role));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing audit trail record")
    public ResponseEntity<AuditTrailResponse> update(@PathVariable Long id, @RequestBody AuditTrailRequest request, Authentication auth) {
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(g -> g.getAuthority().replace("ROLE_", ""))
                .orElse("");
        AuditTrailResponse record = auditTrailService.update(id, request, auth.getName(), role);
        if (record == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(record);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an audit trail record")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        auditTrailService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
