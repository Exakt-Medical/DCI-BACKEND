package com.dci.clearance.service;

import com.dci.clearance.dto.UserRequest;
import com.dci.clearance.dto.UserResponse;
import com.dci.clearance.entity.User;
import com.dci.clearance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditTrailService auditTrailService;
    private final AccountSetupService accountSetupService;

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getByRole(String role) {
        return userRepository.findByRole(User.UserRole.valueOf(role.toUpperCase())).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse create(UserRequest request, String username) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .role(request.getRole() != null ? User.UserRole.valueOf(request.getRole().toUpperCase()) : User.UserRole.CITIZEN)
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .mobile(request.getMobile())
                .build();

        user = userRepository.save(user);
        
        if (user.getRole() == User.UserRole.CITIZEN || user.getRole() == User.UserRole.AGENT_FIXER) {
            accountSetupService.setupCompanyAndBranch(user);
            user = userRepository.save(user);
        }
        String displayName = (request.getFirstName() + " " + request.getLastName()).trim() + " (" + request.getUsername() + ")";
        String roleName = request.getRole() != null ? request.getRole() : "CITIZEN";
        auditTrailService.logAction("Add Account", "Added account: " + displayName + " as " + roleName, username, currentUser.getRole().name());
        return toResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request, String username) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String oldUsername = user.getUsername();
        String oldFirstName = user.getFirstName();
        String oldLastName = user.getLastName();
        String oldEmail = user.getEmail();
        String oldRole = user.getRole().name();

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(User.UserRole.valueOf(request.getRole().toUpperCase()));
        }
        user.setStatus(request.getStatus() != null ? request.getStatus() : user.getStatus());
        user.setMobile(request.getMobile());

        user = userRepository.save(user);
        String displayName = (user.getFirstName() + " " + user.getLastName()).trim() + " (" + user.getUsername() + ")";

        String newStatus = request.getStatus();
        String oldStatus = user.getStatus();
        if ("INACTIVE".equals(newStatus) && "ACTIVE".equals(oldStatus)) {
            auditTrailService.logAction("Deactivate Account", "Deactivated account: " + displayName, username, currentUser.getRole().name());
            return toResponse(user);
        }
        if ("ACTIVE".equals(newStatus) && "INACTIVE".equals(oldStatus)) {
            auditTrailService.logAction("Activate Account", "Activated account: " + displayName, username, currentUser.getRole().name());
            return toResponse(user);
        }
        StringBuilder changes = new StringBuilder();
        if (request.getUsername() != null && !oldUsername.equals(request.getUsername())) {
            changes.append("Username from '").append(oldUsername).append("' to '").append(request.getUsername()).append("'; ");
        }
        if (request.getFirstName() != null && !oldFirstName.equals(request.getFirstName())) {
            changes.append("First Name from '").append(oldFirstName).append("' to '").append(request.getFirstName()).append("'; ");
        }
        if (request.getLastName() != null && !oldLastName.equals(request.getLastName())) {
            changes.append("Last Name from '").append(oldLastName).append("' to '").append(request.getLastName()).append("'; ");
        }
        if (request.getEmail() != null && !oldEmail.equals(request.getEmail())) {
            changes.append("Email from '").append(oldEmail).append("' to '").append(request.getEmail()).append("'; ");
        }
        if (request.getRole() != null && !request.getRole().isBlank() && !oldRole.equals(request.getRole().toUpperCase())) {
            changes.append("Role from '").append(oldRole).append("' to '").append(request.getRole().toUpperCase()).append("'; ");
        }
        String actionMade, details;
        if (changes.length() == 0) {
            actionMade = "Edit Account";
            details = "No changes detected for account: " + displayName;
        } else {
            actionMade = "Edit Account";
            details = "Updated account: " + displayName + " - " + changes.toString().replaceAll("; $", "");
        }
        auditTrailService.logAction(actionMade, details, username, currentUser.getRole().name());
        return toResponse(user);
    }

    @Transactional
    public void delete(Long id, String username) {
        User user = userRepository.findById(id).orElse(null);
        User currentUser = userRepository.findByUsername(username).orElse(null);
        userRepository.deleteById(id);
        if (user != null) {
            String displayName = (user.getFirstName() + " " + user.getLastName()).trim() + " (" + user.getUsername() + ")";
            auditTrailService.logAction("Delete Account", "Deleted account: " + displayName, username, currentUser != null ? currentUser.getRole().name() : "SYSTEM");
        }
    }

    @Transactional
    public List<UserResponse> bulkCreate(List<UserRequest> requests, String username) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<UserResponse> responses = requests.stream().map(request -> {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already exists: " + request.getUsername());
            }
            User user = User.builder()
                    .username(request.getUsername())
                    .password(passwordEncoder.encode(request.getPassword() != null ? request.getPassword() : "password123"))
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .role(request.getRole() != null ? User.UserRole.valueOf(request.getRole().toUpperCase()) : User.UserRole.CITIZEN)
                    .status("ACTIVE")
                    .mobile(request.getMobile())
                    .build();
            user = userRepository.save(user);
            if (user.getRole() == User.UserRole.CITIZEN || user.getRole() == User.UserRole.AGENT_FIXER) {
                accountSetupService.setupCompanyAndBranch(user);
                user = userRepository.save(user);
            }
            return user;
        }).map(this::toResponse).collect(Collectors.toList());
        auditTrailService.logAction("Bulk Add Accounts", "Bulk added " + responses.size() + " accounts", username, currentUser.getRole().name());
        return responses;
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .role(user.getRole().name())
                .status(user.getStatus())
                .companyCode(user.getCompanyCode())
                .branchRef(user.getBranchRef())
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return toResponse(user);
    }
}
