package com.example.finset.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LoginRequest {
    @Email @NotBlank
    private String email;

    @NotBlank
    private String password;
}