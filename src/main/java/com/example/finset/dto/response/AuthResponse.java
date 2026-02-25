package com.example.finset.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder @AllArgsConstructor
public class AuthResponse {
    private Long   id;
    private String email;
    private String name;
    private String avatar;
    private String role;
    private String provider;
    // Note: tokens are in HTTP-only cookies, NOT in this response body
}