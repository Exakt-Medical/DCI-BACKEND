package com.dci.clearance.controller;

import com.dci.clearance.entity.CertificateRequest;
import com.dci.clearance.repository.UserRepository;
import com.dci.clearance.service.CertificateRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/certificate-requests")
@RequiredArgsConstructor
public class CertificateRequestController {

    private final CertificateRequestService service;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getMyRequests(Authentication auth) {
        Long userId = getUserId(auth);
        List<CertificateRequest> records = service.getMyRequests(userId);
        
        // Instead of returning the entity directly, we just return the payloadJson 
        // as a map so the frontend gets back exactly what it saved.
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        List<Map<String, Object>> response = records.stream().map(record -> {
            try {
                Map<String, Object> map = mapper.readValue(record.getPayloadJson(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){});
                map.put("id", record.getId());
                return map;
            } catch (Exception e) {
                return Map.<String, Object>of("id", record.getId());
            }
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> upsertRequest(@RequestBody Map<String, Object> payload, Authentication auth) {
        Long userId = getUserId(auth);
        try {
            CertificateRequest saved = service.upsertRequest(userId, payload);
            return ResponseEntity.ok(Map.of("message", "Saved successfully", "id", saved.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Long getUserId(Authentication auth) {
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}
