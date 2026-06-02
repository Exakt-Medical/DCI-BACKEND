package com.dci.clearance.controller;

import com.dci.clearance.dto.CitizenRegisterRequest;
import com.dci.clearance.entity.User;
import com.dci.clearance.repository.UserRepository;
import com.dci.clearance.service.AccountSetupService;
import com.dci.clearance.service.AuditTrailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Public Registration", description = "Open registration for citizens")
public class PublicController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditTrailService auditTrailService;
    private final AccountSetupService accountSetupService;

    @PostMapping("/register")
    @Operation(summary = "Register a new citizen account", description = "Creates a new user with CITIZEN role")
    public ResponseEntity<?> register(@Valid @RequestBody CitizenRegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Passwords do not match"));
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .role(User.UserRole.CITIZEN)
                .status("ACTIVE")
                .build();

        userRepository.save(user);
        accountSetupService.setupCompanyAndBranch(user);
        userRepository.save(user);
        auditTrailService.logAction("Public Registration", "Citizen registered: " + request.getUsername(), request.getUsername(), "CITIZEN");

        return ResponseEntity.ok(Map.of(
                "message", "Registration successful",
                "username", request.getUsername(),
                "firstName", request.getFirstName(),
                "lastName", request.getLastName(),
                "email", request.getEmail()
        ));
    }
}
