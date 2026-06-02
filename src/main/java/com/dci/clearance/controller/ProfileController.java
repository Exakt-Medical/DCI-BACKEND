package com.dci.clearance.controller;

import com.dci.clearance.dto.ChangePasswordRequest;
import com.dci.clearance.dto.ProfileUpdateRequest;
import com.dci.clearance.dto.UserResponse;
import com.dci.clearance.service.UserManagementService;
import com.dci.clearance.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Profile", description = "User profile management endpoints")  // ADD THIS
public class ProfileController {

    private final UserManagementService userManagementService;
    private final ProfileService profileService;

    @GetMapping
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        String username = authentication.getName();
        UserResponse profile = userManagementService.getByUsername(username);
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    @Operation(summary = "Update user profile")
    public ResponseEntity<UserResponse> updateProfile(
            @RequestBody ProfileUpdateRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        UserResponse updatedProfile = profileService.updateProfile(username, request);
        return ResponseEntity.ok(updatedProfile);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change user password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        profileService.changePassword(username, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/avatar")
    @Operation(summary = "Upload profile avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        String username = authentication.getName();
        String avatarUrl = profileService.uploadAvatar(username, file);
        return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
    }
}   