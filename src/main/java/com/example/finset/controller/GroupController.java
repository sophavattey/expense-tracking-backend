package com.example.finset.controller;

import com.example.finset.dto.GroupDto;
import com.example.finset.service.GroupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @GetMapping("/mine")
    public ResponseEntity<?> getMyGroups(@AuthenticationPrincipal UserDetails principal) {
        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(Map.of("success", true, "data", groupService.getMyGroups(userId)));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroup(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID groupId
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(Map.of("success", true, "data", groupService.getGroup(userId, groupId)));
    }

    @PostMapping
    public ResponseEntity<?> create(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody GroupDto.CreateRequest req
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.status(201).body(Map.of(
            "success", true, "message", "Group created",
            "data", groupService.create(userId, req)
        ));
    }

    @PatchMapping("/{groupId}")
    public ResponseEntity<?> rename(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID groupId,
        @Valid @RequestBody GroupDto.UpdateRequest req
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(Map.of(
            "success", true, "message", "Group renamed",
            "data", groupService.rename(userId, groupId, req)
        ));
    }

    @PostMapping("/join")
    public ResponseEntity<?> join(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody GroupDto.JoinRequest req,
        HttpServletRequest httpRequest
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        String ip = extractIp(httpRequest);
        try {
            GroupDto.Response group = groupService.join(userId, req, ip);
            return ResponseEntity.ok(Map.of("success", true, "message", "Joined group", "data", group));
        } catch (GroupService.RateLimitException ex) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).headers(headers)
                .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @DeleteMapping("/{groupId}/leave")
    public ResponseEntity<?> leave(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID groupId
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        groupService.leave(userId, groupId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Left group"));
    }

    @DeleteMapping("/{groupId}/members/{targetUserId}")
    public ResponseEntity<?> removeMember(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID groupId,
        @PathVariable UUID targetUserId
    ) {
        UUID requesterId = UUID.fromString(principal.getUsername());
        groupService.removeMember(requesterId, groupId, targetUserId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Member removed"));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> dissolve(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID groupId
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        groupService.dissolve(userId, groupId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Group dissolved"));
    }

    @PostMapping("/{groupId}/invite-code/regenerate")
    public ResponseEntity<?> regenerateInviteCode(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID groupId
    ) {
        UUID userId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(Map.of(
            "success", true, "message", "Invite code regenerated",
            "data", groupService.regenerateInviteCode(userId, groupId)
        ));
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}