package com.dci.clearance.controller;

import com.dci.clearance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;

import jakarta.mail.internet.MimeMessage;
import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HealthController {

    private final DataSource dataSource;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${app.email.from:DCI Clearance System <noreply@exakt.com.ph>}")
    private String fromEmail;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());

        Map<String, Object> services = new LinkedHashMap<>();

        // API check
        services.put("api", Map.of("status", "UP", "message", "API is running"));

        // Database check
        try (Connection conn = dataSource.getConnection()) {
            boolean isValid = conn.isValid(3);
            services.put("database", Map.of(
                    "status", isValid ? "UP" : "DOWN",
                    "message", isValid ? "Connected to MySQL" : "Connection invalid"
            ));
        } catch (Exception e) {
            services.put("database", Map.of(
                    "status", "DOWN",
                    "message", e.getMessage()
            ));
            health.put("status", "DEGRADED");
        }

        // User count
        try {
            long userCount = userRepository.count();
            services.put("users", Map.of(
                    "status", "UP",
                    "count", userCount
            ));
        } catch (Exception e) {
            services.put("users", Map.of(
                    "status", "DOWN",
                    "message", e.getMessage()
            ));
        }

        health.put("services", services);
        return ResponseEntity.ok(health);
    }

    @GetMapping("/test-smtp")
    public ResponseEntity<Map<String, Object>> testSmtp(@RequestParam(defaultValue = "test@example.com") String to) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("SMTP Test - DCI Clearance");
            helper.setText("<h1>SMTP Test</h1><p>If you receive this, SMTP is working.</p>", true);
            mailSender.send(message);
            result.put("status", "OK");
            result.put("message", "Email sent successfully");
            result.put("from", fromEmail);
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("message", e.getClass().getName() + ": " + e.getMessage());
            result.put("from", fromEmail);
            result.put("cause", e.getCause() != null ? e.getCause().getMessage() : "none");
        }
        return ResponseEntity.ok(result);
    }
}
