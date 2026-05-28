package com.exakt.vvip.controller;

import com.exakt.vvip.dto.AttachmentRequest;
import com.exakt.vvip.entity.Attachment;
import com.exakt.vvip.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attachment")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Attachment", description = "Attachment Management Endpoints")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping
    @Operation(summary = "Get all attachments")
    public ResponseEntity<List<Attachment>> getAll() {
        return ResponseEntity.ok(attachmentService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get attachment by ID")
    public ResponseEntity<Attachment> getById(@PathVariable Long id) {
        return ResponseEntity.ok(attachmentService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create attachment")
    public ResponseEntity<Attachment> create(
            @RequestBody AttachmentRequest request) {

        return ResponseEntity.ok(attachmentService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update attachment")
    public ResponseEntity<Attachment> update(
            @PathVariable Long id,
            @RequestBody AttachmentRequest request) {

        return ResponseEntity.ok(attachmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete attachment")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        attachmentService.delete(id);

        return ResponseEntity.noContent().build();
    }
}