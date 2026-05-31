package com.exakt.vvip.service;

import com.exakt.vvip.dto.LoginRequest;
import com.exakt.vvip.dto.LoginResponse;
import com.exakt.vvip.entity.User;
import com.exakt.vvip.repository.UserRepository;
import com.exakt.vvip.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {



    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BadCredentialsException("Account is deactivated");
        }

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole().name());

        com.exakt.vvip.entity.Company company = (user.getBranch() != null) ? user.getBranch().getCompany() : null;

        return LoginResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstname(user.getFirstName())
                .lastname(user.getLastName())
                .username(user.getUsername())
                .role(user.getRole().name())
                .allowedToBuyVoucher(user.getIsBuyVoucherAllowed())
                .message("Login successful")
                .companyId(company != null ? company.getId() : null)
                .companyCode(company != null ? company.getCode() : null)
                .build();
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
    }
}