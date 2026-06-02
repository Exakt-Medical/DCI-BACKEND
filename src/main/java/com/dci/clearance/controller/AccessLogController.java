package com.dci.clearance.controller;

import com.dci.clearance.dto.AccessTrailResponse;
import com.dci.clearance.service.AccessLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/access-logs")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Access Logs", description = "Access logs - Login and logout records")
public class AccessLogController {

    private final AccessLogService accessLogService;

    @GetMapping
    @Operation(summary = "List all access logs (login and logout records)")
    public ResponseEntity<List<AccessTrailResponse>> getAll() {
        return ResponseEntity.ok(accessLogService.getAll());
    }
}
