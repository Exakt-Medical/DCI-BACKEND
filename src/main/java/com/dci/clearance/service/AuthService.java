package com.dci.clearance.service;

import com.dci.clearance.dto.LoginRequest;
import com.dci.clearance.dto.LoginResponse;
import com.dci.clearance.dto.OtpRequiredResponse;
import com.dci.clearance.entity.Company;
import com.dci.clearance.entity.EmailVerificationToken;
import com.dci.clearance.entity.User;
import com.dci.clearance.repository.CompanyRepository;
import com.dci.clearance.repository.EmailVerificationTokenRepository;
import com.dci.clearance.repository.UserRepository;
import com.dci.clearance.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final CompanyRepository companyRepository;
    private final EmailService emailService;
    private final EmailVerificationTokenRepository verificationTokenRepository;

    private static final long OTP_EXPIRY_MS = 5 * 60 * 1000L;
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public Object login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BadCredentialsException("Account is deactivated");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified()) && user.getEmail() != null && !user.getEmail().isBlank()) {
            throw new BadCredentialsException("Please verify your email address before logging in");
        }

        String otpCode = generateOtp();
        otpStore.put(user.getId().toString(), new OtpEntry(otpCode, Instant.now().toEpochMilli()));

        String displayName = user.getFirstName() != null ? user.getFirstName() : user.getUsername();
        emailService.sendOtpEmail(user.getEmail(), otpCode, displayName);

        return OtpRequiredResponse.builder()
                .userId(user.getId().toString())
                .email(user.getEmail())
                .message("OTP sent to your email")
                .otpRequired(true)
                .build();
    }

    public LoginResponse verifyOtp(String userId, String otpCode) {
        OtpEntry entry = otpStore.get(userId);
        if (entry == null) {
            throw new BadCredentialsException("OTP expired or not found. Please login again.");
        }
        if (Instant.now().toEpochMilli() - entry.createdAt > OTP_EXPIRY_MS) {
            otpStore.remove(userId);
            throw new BadCredentialsException("OTP expired. Please login again.");
        }
        if (!entry.code.equals(otpCode)) {
            throw new BadCredentialsException("Invalid OTP code");
        }
        otpStore.remove(userId);

        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole().name());
        Long companyId = companyRepository.findByCode(user.getCompanyCode())
                .map(Company::getId)
                .orElse(null);

        return LoginResponse.builder()
                .userId(user.getId())
                .token(token)
                .email(user.getEmail())
                .firstname(user.getFirstName())
                .lastname(user.getLastName())
                .username(user.getUsername())
                .role(user.getRole().name())
                .companyCode(user.getCompanyCode())
                .companyId(companyId)
                .branchRef(user.getBranchRef())
                .message("Login successful")
                .build();
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification link"));

        if (Boolean.TRUE.equals(verificationToken.getUsed())) {
            throw new IllegalArgumentException("This verification link has already been used");
        }

        if (LocalDateTime.now().isAfter(verificationToken.getExpiryDate())) {
            throw new IllegalArgumentException("This verification link has expired. Please register again.");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);

        log.info("Email verified for user: {}", user.getUsername());
    }

    private String generateOtp() {
        int code = 100000 + (int) (Math.random() * 900000);
        return String.valueOf(code);
    }

    private record OtpEntry(String code, long createdAt) {}
}
