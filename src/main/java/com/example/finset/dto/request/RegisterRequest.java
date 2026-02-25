package com.example.finset.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterRequest {
    @NotBlank
    private String name;

    @Email @NotBlank
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    @NotBlank
    private String password;
}