package com.dci.clearance.controller;

import com.dci.clearance.dto.UserRequest;
import com.dci.clearance.dto.UserResponse;
import com.dci.clearance.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Users", description = "User CRUD operations per branch")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    @Operation(summary = "List all users")
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userManagementService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        UserResponse user = userManagementService.getById(id);
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(user);
    }

    @GetMapping("/by-role/{role}")
    @Operation(summary = "Get users by role (CITIZEN, AGENT_FIXER, HPG, DCI, ADMIN)")
    public ResponseEntity<List<UserResponse>> getByRole(@PathVariable String role) {
        return ResponseEntity.ok(userManagementService.getByRole(role));
    }

    @PostMapping("")
    @Operation(summary = "Create a new user")
    public ResponseEntity<?> create(@RequestBody UserRequest request, Authentication auth) {
        try {
            return ResponseEntity.ok(userManagementService.create(request, auth.getName()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing user")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UserRequest request, Authentication auth) {
        try {
            UserResponse user = userManagementService.update(id, request, auth.getName());
            if (user == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        userManagementService.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk create users (CSV/Excel upload)")
    public ResponseEntity<?> bulkCreate(@RequestBody List<UserRequest> requests, Authentication auth) {
        try {
            return ResponseEntity.ok(userManagementService.bulkCreate(requests, auth.getName()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
