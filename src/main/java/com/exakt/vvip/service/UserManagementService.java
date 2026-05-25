package com.exakt.vvip.service;

import com.exakt.vvip.dto.UserRequest;
import com.exakt.vvip.dto.UserResponse;
import com.exakt.vvip.entity.Branch;
import com.exakt.vvip.entity.User;
import com.exakt.vvip.repository.BranchRepository;
import com.exakt.vvip.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditTrailService auditTrailService;

    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElse(null);
    }

    public List<UserResponse> getByBranch(Long branchId) {
        return userRepository.findByBranchId(branchId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getByManager(Long managerId) {
        return userRepository.findByManagerId(managerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

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

        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found"));
        }

        User manager = null;
        if (request.getManagerId() != null) {
            manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new RuntimeException("Manager not found"));
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .userId(request.getUserId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .middleInitial(request.getMiddleInitial())
                .extName(request.getExtName())
                .email(request.getEmail())
                .role(request.getRole() != null ? User.UserRole.valueOf(request.getRole().toUpperCase()) : User.UserRole.AGENT)
                .branch(branch)
                .manager(manager)
                .isactive(request.getIsactive() != null ? request.getIsactive() : true)
                .isSubAgent(request.getIsSubAgent() != null ? request.getIsSubAgent() : false)
                .userstamp(currentUser)
                .build();

        user = userRepository.save(user);
        String displayName = (request.getFirstName() + " " + request.getLastName()).trim() + " (" + request.getUsername() + ")";
        String roleName = request.getRole() != null ? request.getRole() : "AGENT";
        auditTrailService.logAction("Added a New Account " + displayName + " as " + roleName, "Created user " + displayName + " as " + roleName, username, currentUser.getRole().name());
        return toResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request, String username) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String oldUserId = user.getUserId();
        String oldUsername = user.getUsername();
        String oldFirstName = user.getFirstName();
        String oldLastName = user.getLastName();
        String oldEmail = user.getEmail();
        String oldRole = user.getRole().name();
        String oldBranchName = user.getBranch() != null ? user.getBranch().getBranchName() : null;
        String oldManagerName = user.getManager() != null ? user.getManager().getFirstName() + " " + user.getManager().getLastName() : null;
        Boolean oldActive = user.getIsactive();

        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found"));
        }

        User manager = null;
        if (request.getManagerId() != null) {
            manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new RuntimeException("Manager not found"));
        }

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setUserId(request.getUserId());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setMiddleInitial(request.getMiddleInitial());
        user.setExtName(request.getExtName());
        user.setEmail(request.getEmail());
        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(User.UserRole.valueOf(request.getRole().toUpperCase()));
        }
        user.setBranch(branch);
        user.setManager(manager);
        user.setIsactive(request.getIsactive() != null ? request.getIsactive() : user.getIsactive());
        user.setIsSubAgent(request.getIsSubAgent() != null ? request.getIsSubAgent() : user.getIsSubAgent());
        user.setUserstamp(currentUser);

        user = userRepository.save(user);
        String displayName = (user.getFirstName() + " " + user.getLastName()).trim() + " (" + user.getUsername() + ")";

        if (Boolean.FALSE.equals(request.getIsactive()) && Boolean.TRUE.equals(oldActive)) {
            auditTrailService.logAction("Deactivated Account " + displayName, "Set Account " + displayName + " to inactive", username, currentUser.getRole().name());
            return toResponse(user);
        }
        if (Boolean.TRUE.equals(request.getIsactive()) && Boolean.FALSE.equals(oldActive)) {
            auditTrailService.logAction("Activated Account " + displayName, "Set Account " + displayName + " to active", username, currentUser.getRole().name());
            return toResponse(user);
        }
        List<String> changes = new ArrayList<>();
        if (request.getUserId() != null && !oldUserId.equals(request.getUserId())) {
            changes.add("User ID from '" + oldUserId + "' to '" + request.getUserId() + "'");
        }
        if (request.getUsername() != null && !oldUsername.equals(request.getUsername())) {
            changes.add("Username from '" + oldUsername + "' to '" + request.getUsername() + "'");
        }
        if (request.getFirstName() != null && !oldFirstName.equals(request.getFirstName())) {
            changes.add("First Name from '" + oldFirstName + "' to '" + request.getFirstName() + "'");
        }
        if (request.getLastName() != null && !oldLastName.equals(request.getLastName())) {
            changes.add("Last Name from '" + oldLastName + "' to '" + request.getLastName() + "'");
        }
        if (request.getEmail() != null && !oldEmail.equals(request.getEmail())) {
            changes.add("Email from '" + oldEmail + "' to '" + request.getEmail() + "'");
        }
        if (request.getRole() != null && !request.getRole().isBlank() && !oldRole.equals(request.getRole().toUpperCase())) {
            changes.add("Role from '" + oldRole + "' to '" + request.getRole().toUpperCase() + "'");
        }
        String newBranchName = branch != null ? branch.getBranchName() : null;
        if ((oldBranchName == null && newBranchName != null) || (oldBranchName != null && !oldBranchName.equals(newBranchName))) {
            changes.add("Branch from '" + oldBranchName + "' to '" + newBranchName + "'");
        }
        String newManagerName = manager != null ? manager.getFirstName() + " " + manager.getLastName() : null;
        if ((oldManagerName == null && newManagerName != null) || (oldManagerName != null && !oldManagerName.equals(newManagerName))) {
            changes.add("Manager from '" + oldManagerName + "' to '" + newManagerName + "'");
        }
        String actionMade, details;
        if (changes.isEmpty()) {
            actionMade = "Edited Account " + displayName;
            details = "No changes detected";
        } else {
            actionMade = "Edited Account " + displayName + " (" + String.join("; ", changes) + ")";
            details = String.join("; ", changes);
        }
        auditTrailService.logAction(actionMade, details, username, currentUser.getRole().name());
        return toResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id).orElse(null);
        userRepository.deleteById(id);
        if (user != null) {
            String displayName = (user.getFirstName() + " " + user.getLastName()).trim() + " (" + user.getUsername() + ")";
            auditTrailService.logAction("Deleted Account " + displayName, "Deleted user " + displayName, "system", "SYSTEM");
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
            Branch branch = null;
            if (request.getBranchId() != null) {
                branch = branchRepository.findById(request.getBranchId())
                        .orElseThrow(() -> new RuntimeException("Branch not found"));
            }
            User manager = null;
            if (request.getManagerId() != null) {
                manager = userRepository.findById(request.getManagerId())
                        .orElseThrow(() -> new RuntimeException("Manager not found"));
            }
            User user = User.builder()
                    .username(request.getUsername())
                    .password(passwordEncoder.encode(request.getPassword() != null ? request.getPassword() : "password123"))
                    .userId(request.getUserId())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .middleInitial(request.getMiddleInitial())
                    .extName(request.getExtName())
                    .email(request.getEmail())
                    .role(request.getRole() != null ? User.UserRole.valueOf(request.getRole().toUpperCase()) : User.UserRole.AGENT)
                    .branch(branch)
                    .manager(manager)
                    .isactive(true)
                    .isSubAgent(request.getIsSubAgent() != null ? request.getIsSubAgent() : false)
                    .userstamp(currentUser)
                    .build();
            return userRepository.save(user);
        }).map(this::toResponse).collect(Collectors.toList());
        auditTrailService.logAction("Bulk Added " + responses.size() + " Accounts", "Bulk created " + responses.size() + " users", username, currentUser.getRole().name());
        return responses;
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .middleInitial(user.getMiddleInitial())
                .extName(user.getExtName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .branchId(user.getBranch() != null ? user.getBranch().getId() : null)
                .branchName(user.getBranch() != null ? user.getBranch().getBranchName() : null)
                .managerId(user.getManager() != null ? user.getManager().getId() : null)
                .managerName(user.getManager() != null ? user.getManager().getFirstName() + " " + user.getManager().getLastName() : null)
                .isactive(user.getIsactive())
                .isSubAgent(user.getIsSubAgent())
                .userstamp(user.getUserstamp() != null ? user.getUserstamp().getUsername() : null)
                .timestamp(user.getTimestamp())
                .build();
    }
}
