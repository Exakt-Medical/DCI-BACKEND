package com.exakt.vvip.controller;

import com.exakt.vvip.dto.BranchRequest;
import com.exakt.vvip.dto.BranchResponse;
import com.exakt.vvip.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Branches", description = "Branch CRUD operations")
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @Operation(summary = "List all branches")
    public ResponseEntity<List<BranchResponse>> getAll() {
        return ResponseEntity.ok(branchService.getAll());
    }

    @GetMapping("/by-company/{companyId}")
    @Operation(summary = "Get branches by company ID")
    public ResponseEntity<List<BranchResponse>> getByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(branchService.getByCompany(companyId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get branch by ID")
    public ResponseEntity<BranchResponse> getById(@PathVariable Long id) {
        BranchResponse branch = branchService.getById(id);
        if (branch == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(branch);
    }

    @PostMapping("")
    @Operation(summary = "Create a new branch")
    public ResponseEntity<?> create(@RequestBody BranchRequest request, Authentication auth) {
        try {
            return ResponseEntity.ok(branchService.create(request, auth.getName()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing branch")
    public ResponseEntity<BranchResponse> update(@PathVariable Long id, @RequestBody BranchRequest request, Authentication auth) {
        BranchResponse branch = branchService.update(id, request, auth.getName());
        if (branch == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(branch);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a branch")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        branchService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
