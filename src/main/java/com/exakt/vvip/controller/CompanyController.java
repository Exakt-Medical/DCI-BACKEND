package com.exakt.vvip.controller;

import com.exakt.vvip.dto.CompanyRequest;
import com.exakt.vvip.dto.CompanyResponse;
import com.exakt.vvip.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Companies", description = "Company CRUD operations")
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    @Operation(summary = "List all companies")
    public ResponseEntity<List<CompanyResponse>> getAll() {
        return ResponseEntity.ok(companyService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get company by ID")
    public ResponseEntity<CompanyResponse> getById(@PathVariable Long id) {
        CompanyResponse company = companyService.getById(id);
        if (company == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(company);
    }

    @PostMapping("")
    @Operation(summary = "Create a new company")
    public ResponseEntity<?> create(@RequestBody CompanyRequest request, Authentication auth) {
        try {
            return ResponseEntity.ok(companyService.create(request, auth.getName()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing company")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CompanyRequest request, Authentication auth) {
        try {
            CompanyResponse company = companyService.update(id, request, auth.getName());
            if (company == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(company);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a company")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}