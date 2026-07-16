package com.dci.clearance.service;

import com.dci.clearance.dto.CitizenRegisterRequest;
import com.dci.clearance.entity.EmailVerificationToken;
import com.dci.clearance.entity.User;
import com.dci.clearance.repository.EmailVerificationTokenRepository;
import com.dci.clearance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class CitizenRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountSetupService accountSetupService;
    private final AuditTrailService auditTrailService;
    private final EmailService emailService;
    private final EmailVerificationTokenRepository verificationTokenRepository;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public User registerCitizen(CitizenRegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .role(User.UserRole.CITIZEN)
                .status("ACTIVE")
                .emailVerified(false)
                .billerooSyncStatus("PENDING")
                .billerooRetryCount(0)
                .build();
        user = userRepository.save(user);

        accountSetupService.setupCompanyAndBranch(user);
        user = userRepository.save(user);

        auditTrailService.logAction(
                "Register",
                "Citizen registered: " + request.getUsername()
                        + " (sync: " + user.getBillerooSyncStatus() + ")",
                request.getUsername(),
                "CITIZEN"
        );

        log.info("Citizen registration completed: username={}, companyCode={}, syncStatus={}",
                user.getUsername(), user.getCompanyCode(), user.getBillerooSyncStatus());

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            sendVerificationEmail(user);
        }

        return user;
    }

    private void sendVerificationEmail(User user) {
        byte[] tokenBytes = new byte[32];
        new SecureRandom().nextBytes(tokenBytes);
        String token = HexFormat.of().formatHex(tokenBytes);

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        verificationTokenRepository.save(verificationToken);

        String verificationUrl = frontendUrl + "/dci-access/verify-email?token=" + token;
        String firstName = user.getFirstName() != null ? user.getFirstName() : user.getUsername();
        emailService.sendVerificationEmail(user.getEmail(), firstName, verificationUrl);
    }
}
