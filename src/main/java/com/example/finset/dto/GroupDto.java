package com.example.finset.dto;

import com.example.finset.entity.GroupMember;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class GroupDto {

    /* ── Create group ──────────────────────────────────────────── */
    @Data
    public static class CreateRequest {
        @NotBlank(message = "Group name is required")
        @Size(max = 100, message = "Name must be 100 characters or less")
        private String name;
    }

    /* ── Join group ────────────────────────────────────────────── */
    @Data
    public static class JoinRequest {
        @NotBlank(message = "Invite code is required")
        private String inviteCode;
    }

    /* ── Member DTO (inside group response) ────────────────────── */
    @Data
    public static class MemberResponse {
        private UUID              id;        // GroupMember id
        private UUID              userId;
        private String            name;
        private String            email;
        private String            avatar;
        private GroupMember.Role  role;
        private LocalDateTime     joinedAt;
    }

    /* ── Full group response ───────────────────────────────────── */
    @Data
    public static class Response {
        private UUID                  id;
        private String                name;
        private String                inviteCode;
        private LocalDateTime         inviteCodeExpiresAt;  // so frontend can show expiry
        private boolean               inviteCodeExpired;    // convenience flag
        private UUID                  ownerId;
        private List<MemberResponse>  members;
        private LocalDateTime         createdAt;
    }
}