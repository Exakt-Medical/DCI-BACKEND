package com.dci.clearance.service;

import com.dci.clearance.dto.ProfileUpdateRequest;
import com.dci.clearance.dto.UserResponse;
import com.dci.clearance.dto.ChangePasswordRequest;
import com.dci.clearance.entity.Company;
import com.dci.clearance.entity.User;
import com.dci.clearance.repository.CompanyRepository;
import com.dci.clearance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditTrailService auditTrailService;
    private final CompanyRepository companyRepository;
    private final BillerooCompanySyncService billerooCompanySyncService;

    @Transactional
    public UserResponse updateProfile(String username, ProfileUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Track whether email changed for Billeroo resync
        boolean emailChanged = request.getEmail() != null
                && !request.getEmail().equals(user.getEmail());

        // Update user fields (matching your database columns)
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getMobile() != null) user.setMobile(request.getMobile());

        User updatedUser = userRepository.save(user);

        // Billeroo resync on email change for citizens
        if (emailChanged && user.getRole() == User.UserRole.CITIZEN
                && user.getCompanyCode() != null) {
            resyncBillerooOnEmailChange(user);
        }

        // Log profile update
        String displayName = (user.getFirstName() + " " + user.getLastName()).trim();
        auditTrailService.logAction(
                "Edit Profile",
                "Updated profile for " + displayName,
                username,
                user.getRole().name()
        );

        return toUserResponse(updatedUser);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditTrailService.logAction(
                "Change Password",
                "Password changed for user: " + username,
                username,
                user.getRole().name()
        );
    }

    @Transactional
    public String uploadAvatar(String username, MultipartFile file) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        try {
            String uploadDir = "uploads/avatars/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);
            Files.write(filePath, file.getBytes());

            return "/uploads/avatars/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload avatar", e);
        }
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .role(user.getRole() != null ? user.getRole().name() : "CITIZEN")
                .status(user.getStatus())
                .dateCreated(user.getDateCreated())
                .build();
    }

    /**
     * Re-syncs the citizen's shadow company to Billeroo after an email change.
     * Updates the local company record email first, then calls Billeroo.
     * On failure, resets sync status to FAILED and retry count to 0 so the
     * retry job picks it up automatically on the next cycle.
     */
    private void resyncBillerooOnEmailChange(User user) {
        // Update the company record email locally
        companyRepository.findByCode(user.getCompanyCode()).ifPresent(company -> {
            company.setEmail(user.getEmail());
            companyRepository.save(company);
        });

        // Attempt Billeroo re-sync with updated email
        try {
            String companyName = user.getFirstName() + " "
                    + user.getLastName() + " "
                    + user.getCompanyCode();
            billerooCompanySyncService.sync(
                    user.getCompanyCode(), user.getEmail(), companyName
            );
            log.info("Billeroo resync on email change succeeded for user id={}", user.getId());
        } catch (Exception e) {
            // Don't block the email update — let the retry job handle it
            user.setBillerooSyncStatus("FAILED");
            user.setBillerooRetryCount(0);
            userRepository.save(user);

            // Also update the company sync status
            companyRepository.findByCode(user.getCompanyCode()).ifPresent(company -> {
                company.setBillerooSyncStatus("FAILED");
                companyRepository.save(company);
            });

            log.warn("Billeroo resync on email change failed for user id={}. "
                    + "Retry job will pick this up. Error: {}", user.getId(), e.getMessage());
        }
    }
}