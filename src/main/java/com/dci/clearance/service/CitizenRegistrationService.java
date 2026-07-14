package com.dci.clearance.service;

import com.dci.clearance.dto.CitizenRegisterRequest;
import com.dci.clearance.entity.User;
import com.dci.clearance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates citizen registration end-to-end:
 *
 * <ol>
 *   <li>Validates input (username uniqueness, password confirmation)</li>
 *   <li>Creates the user with CITIZEN role and PENDING sync status</li>
 *   <li>Delegates shadow-company creation + Billeroo sync to {@link AccountSetupService}</li>
 *   <li>Persists the final user state</li>
 * </ol>
 *
 * <p>Registration always succeeds (returns 201) regardless of Billeroo availability.
 * The retry job handles failed syncs asynchronously.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CitizenRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountSetupService accountSetupService;
    private final AuditTrailService auditTrailService;

    /**
     * Registers a new citizen account.
     *
     * @param request the registration request (already validated by @Valid)
     * @return the persisted User entity
     * @throws IllegalArgumentException if username already exists or passwords don't match
     */
    @Transactional
    public User registerCitizen(CitizenRegisterRequest request) {
        // 1. Username uniqueness check
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // 2. Email uniqueness check
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // 3. Confirm password match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }

        // 3. Create citizen user with PENDING sync status
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .role(User.UserRole.CITIZEN)
                .status("ACTIVE")
                .billerooSyncStatus("PENDING")
                .billerooRetryCount(0)
                .build();
        user = userRepository.save(user);

        // 4. Shadow company creation + Billeroo sync attempt
        //    (AccountSetupService sets companyCode and billerooSyncStatus on the user)
        accountSetupService.setupCompanyAndBranch(user);

        // 5. Persist final user state (sync status is now SYNCED or FAILED)
        user = userRepository.save(user);

        // 6. Audit trail
        auditTrailService.logAction(
                "Register",
                "Citizen registered: " + request.getUsername()
                        + " (sync: " + user.getBillerooSyncStatus() + ")",
                request.getUsername(),
                "CITIZEN"
        );

        log.info("Citizen registration completed: username={}, companyCode={}, syncStatus={}",
                user.getUsername(), user.getCompanyCode(), user.getBillerooSyncStatus());

        return user;
    }
}
