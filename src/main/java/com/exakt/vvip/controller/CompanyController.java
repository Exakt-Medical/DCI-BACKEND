package com.exakt.vvip.controller;

import com.exakt.vvip.entity.Company;
import com.exakt.vvip.entity.Company.CompanyStatus;
import com.exakt.vvip.repository.CompanyRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/companies")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Company Management", description = "Insurance company CRUD operations")
public class CompanyController {

    private final CompanyRepository companyRepository;

    @GetMapping
    @Operation(summary = "List all companies")
    public ResponseEntity<List<Company>> getAll() {
        return ResponseEntity.ok(companyRepository.findAll());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filter companies by status")
    public ResponseEntity<List<Company>> getByStatus(@PathVariable String status) {
        try {
            CompanyStatus st = CompanyStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(companyRepository.findByStatus(st));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    @Operation(summary = "Create a new company")
    public ResponseEntity<Company> create(@RequestBody Company company) {
        return ResponseEntity.ok(companyRepository.save(company));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing company")
    public ResponseEntity<Company> update(@PathVariable Long id, @RequestBody Company company) {
        Company existing = companyRepository.findById(id).orElseThrow();
        existing.setName(company.getName());
        existing.setCode(company.getCode());
        existing.setBranch(company.getBranch());
        existing.setProvider(company.getProvider());
        existing.setAddress(company.getAddress());
        existing.setStatus(company.getStatus());
        return ResponseEntity.ok(companyRepository.save(existing));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Approve or decline a company")
    public ResponseEntity<Company> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Company company = companyRepository.findById(id).orElseThrow();
        String action = body.get("action");
        if ("approve".equalsIgnoreCase(action)) {
            company.setStatus(CompanyStatus.ACTIVE);
        } else if ("decline".equalsIgnoreCase(action)) {
            company.setStatus(CompanyStatus.DECLINED);
        }
        return ResponseEntity.ok(companyRepository.save(company));
    }
}