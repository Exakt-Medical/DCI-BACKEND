package com.dci.clearance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CitizenRegisterRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern.List({
        @Pattern(regexp = ".*[A-Z].*", message = "Password must contain at least one uppercase letter"),
        @Pattern(regexp = ".*[a-z].*", message = "Password must contain at least one lowercase letter"),
        @Pattern(regexp = ".*\\d.*", message = "Password must contain at least one digit")
    })
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    private String email;
}
