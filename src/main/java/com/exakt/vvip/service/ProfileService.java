package com.exakt.vvip.service;

import com.exakt.vvip.dto.ProfileUpdateRequest;
import com.exakt.vvip.dto.UserResponse;
import com.exakt.vvip.dto.ChangePasswordRequest;
import com.exakt.vvip.entity.User;
import com.exakt.vvip.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditTrailService auditTrailService;

    @Transactional
    public UserResponse updateProfile(String username, ProfileUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Update user fields (matching your database columns)
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getMiddleInitial() != null) user.setMiddleInitial(request.getMiddleInitial());
        if (request.getExtName() != null) user.setExtName(request.getExtName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getMobile() != null) user.setMobile(request.getMobile());

        User updatedUser = userRepository.save(user);

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
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .middleInitial(user.getMiddleInitial())
                .extName(user.getExtName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .role(user.getRole() != null ? user.getRole().name() : "AGENT")
                .branchId(user.getBranch() != null ? user.getBranch().getId() : null)
                .branchName(user.getBranch() != null ? user.getBranch().getBranchName() : null)
                .branchCompanyName(user.getBranch() != null && user.getBranch().getCompany() != null ? user.getBranch().getCompany().getCompanyName() : null)
                .status(user.getStatus())
                .dateCreated(user.getDateCreated())
                .build();
    }
}