package com.exakt.vvip.controller;

import com.exakt.vvip.dto.AttachmentRequest;
import com.exakt.vvip.entity.Attachment;
import com.exakt.vvip.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    // New endpoint to serve image files
    @GetMapping("/{id}/image/{type}")
    public ResponseEntity<byte[]> getImage(
            @PathVariable Long id,
            @PathVariable String type) {
        try {
            Attachment attachment = attachmentService.getById(id);
            if (attachment == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] imageData = null;
            String contentType = "image/jpeg";

            switch (type.toLowerCase()) {
                case "cr":
                    imageData = attachment.getCrAttachment();
                    break;
                case "plate":
                    imageData = attachment.getPlateCertificationAttachment();
                    break;
                case "actual":
                    imageData = attachment.getActualPlateAttachment();
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }

            if (imageData == null || imageData.length == 0) {
                return ResponseEntity.notFound().build();
            }

            // Try to detect content type from byte array
            if (imageData.length > 4) {
                if (imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xD8) {
                    contentType = "image/jpeg";
                } else if (imageData[0] == (byte) 0x89 && imageData[1] == (byte) 0x50 &&
                        imageData[2] == (byte) 0x4E && imageData[3] == (byte) 0x47) {
                    contentType = "image/png";
                }
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("Content-Disposition", "inline; filename=\"attachment_" + id + ".jpg\"")
                    .body(imageData);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
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

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @Operation(summary = "Upload attachment file")
    public ResponseEntity<Attachment> uploadAttachment(
            @RequestParam String referenceNumber,
            @RequestParam String requestedBy,
            @RequestParam(required = false) MultipartFile crAttachment,
            @RequestParam(required = false) MultipartFile plateCertificationAttachment,
            @RequestParam(required = false) MultipartFile actualPlateAttachment
    ) throws IOException {

        return ResponseEntity.ok(
                attachmentService.uploadAttachment(
                        referenceNumber,
                        requestedBy,
                        crAttachment,
                        plateCertificationAttachment,
                        actualPlateAttachment
                )
        );
    }
}