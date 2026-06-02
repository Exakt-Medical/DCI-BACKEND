package com.dci.clearance.service;

import com.dci.clearance.dto.LoginRequest;
import com.dci.clearance.dto.LoginResponse;
import com.dci.clearance.entity.User;
import com.dci.clearance.repository.UserRepository;
import com.dci.clearance.security.JwtUtils;
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

        return LoginResponse.builder()
                .userId(user.getId())
                .token(token)
                .email(user.getEmail())
                .firstname(user.getFirstName())
                .lastname(user.getLastName())
                .username(user.getUsername())
                .role(user.getRole().name())
                .companyCode(user.getCompanyCode())
                .branchRef(user.getBranchRef())
                .message("Login successful")
                .build();
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
    }
}