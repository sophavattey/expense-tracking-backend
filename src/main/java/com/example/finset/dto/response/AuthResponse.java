package com.example.finset.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter @Builder @AllArgsConstructor
public class AuthResponse {
    private UUID   id;
    private String email;
    private String name;
    private String avatar;
    private String role;
    private String provider;
}