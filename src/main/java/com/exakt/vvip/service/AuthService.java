package com.exakt.vvip.service;

import com.exakt.vvip.dto.*;
import com.exakt.vvip.entity.User;
import com.exakt.vvip.entity.User.UserRole;
import com.exakt.vvip.repository.UserRepository;
import com.exakt.vvip.security.JwtUtils;
import com.exakt.vvip.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final JavaMailSender mailSender;

    @Value("${app.2fa.code-length}")
    private int codeLength;

    @Value("${app.2fa.code-expiration-minutes}")
    private int codeExpirationMinutes;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!user.getActive()) {
            throw new BadCredentialsException("Account is deactivated");
        }

        if (user.getMfaEnabled()) {
            String code = generateMfaCode();
            user.setMfaCode(code);
            user.setMfaCodeExpiry(LocalDateTime.now().plusMinutes(codeExpirationMinutes));
            user.setMfaVerified(false);
            userRepository.save(user);

            sendMfaEmail(user.getEmail(), code);

            return LoginResponse.builder()
                    .mfaRequired(true)
                    .username(user.getUsername())
                    .message("Verification code sent to your email")
                    .build();
        }

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole().name());
        user.setMfaVerified(true);
        userRepository.save(user);

        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .mfaRequired(false)
                .message("Login successful")
                .build();
    }

    public LoginResponse verifyMfa(MfaVerifyRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid request"));

        if (user.getMfaCode() == null) {
            throw new BadCredentialsException("No verification code requested");
        }

        if (user.getMfaCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Verification code has expired");
        }

        if (!user.getMfaCode().equals(request.getCode())) {
            throw new BadCredentialsException("Invalid verification code");
        }

        user.setMfaCode(null);
        user.setMfaCodeExpiry(null);
        user.setMfaVerified(true);
        userRepository.save(user);

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole().name());

        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .mfaRequired(false)
                .message("Login successful")
                .build();
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
    }

    private String generateMfaCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    private void sendMfaEmail(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("exaktdev@exakt.com.ph");
            message.setTo(to);
            message.setSubject("VVIP - Two-Factor Authentication Code");
            message.setText(String.format(
                    "Your verification code is: %s\n\n" +
                    "This code will expire in %d minutes.\n\n" +
                    "If you did not request this code, please ignore this email.\n\n" +
                    "VVIP - Vehicle Verification Insurance Program",
                    code, codeExpirationMinutes));
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email: " + e.getMessage());
        }
    }
}