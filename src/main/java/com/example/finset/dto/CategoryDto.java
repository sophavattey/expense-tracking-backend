package com.example.finset.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

public class CategoryDto {

    @Data
    public static class Request {
        @NotBlank(message = "Category name is required")
        @Size(max = 50, message = "Name must be 50 characters or less")
        private String name;

        @Size(max = 10, message = "Icon must be 10 characters or less")
        private String icon;

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex e.g. #2563eb")
        private String color;
    }

    @Data
    public static class Response {
        private UUID   id;
        private String name;
        private String icon;
        private String color;

        @JsonProperty("isDefault")
        private boolean isDefault;

        @JsonProperty("isOwned")
        private boolean isOwned;
    }
}