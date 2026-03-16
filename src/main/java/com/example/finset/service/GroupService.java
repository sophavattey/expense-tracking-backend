package com.example.finset.service;

import com.example.finset.dto.GroupDto;
import com.example.finset.entity.Group;
import com.example.finset.entity.GroupMember;
import com.example.finset.entity.User;
import com.example.finset.exception.ResourceNotFoundException;
import com.example.finset.repository.BudgetRepository;
import com.example.finset.repository.ExpenseRepository;
import com.example.finset.repository.GroupMemberRepository;
import com.example.finset.repository.GroupRepository;
import com.example.finset.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private static final String INVITE_CHARS  = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int    INVITE_LEN    = 8;
    private static final int    MAX_MEMBERS   = 10;
    private static final int    CODE_TTL_DAYS = 7;

    private final GroupRepository       groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository        userRepository;
    private final BudgetRepository      budgetRepository;
    private final ExpenseRepository     expenseRepository;
    private final JoinRateLimiter       rateLimiter;

    @Transactional(readOnly = true)
    public List<GroupDto.Response> getMyGroups(UUID userId) {
        User user = getUser(userId);
        return groupRepository.findAllByMember(user).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GroupDto.Response getGroup(UUID userId, UUID groupId) {
        User  user  = getUser(userId);
        Group group = getGroupAndVerifyMember(groupId, user);
        return toResponse(group);
    }

    @Transactional
    public GroupDto.Response create(UUID userId, GroupDto.CreateRequest req) {
        User user = getUser(userId);
        Group group = Group.builder()
            .name(req.getName()).owner(user)
            .inviteCode(generateInviteCode())
            .inviteCodeExpiresAt(LocalDateTime.now().plusDays(CODE_TTL_DAYS))
            .build();
        GroupMember ownerMember = GroupMember.builder()
            .group(group).user(user).role(GroupMember.Role.OWNER).build();
        group.getMembers().add(ownerMember);
        return toResponse(groupRepository.save(group));
    }

    @Transactional
    public GroupDto.Response rename(UUID userId, UUID groupId, GroupDto.UpdateRequest req) {
        User  user  = getUser(userId);
        Group group = getGroupAndVerifyOwner(groupId, user);
        group.setName(req.getName().trim());
        return toResponse(groupRepository.save(group));
    }

    @Transactional
    public GroupDto.Response join(UUID userId, GroupDto.JoinRequest req, String ipAddress) {
        if (!rateLimiter.tryConsume(ipAddress)) {
            long retryAfter = rateLimiter.secondsUntilReset(ipAddress);
            throw new RateLimitException(
                "Too many join attempts. Please try again in " + (retryAfter / 60 + 1) + " minutes.", retryAfter);
        }
        User  user  = getUser(userId);
        Group group = groupRepository.findByInviteCode(req.getInviteCode().toUpperCase().trim())
            .orElseThrow(() -> new ResourceNotFoundException("Invalid invite code — group not found."));
        if (group.getInviteCodeExpiresAt() != null &&
                LocalDateTime.now().isAfter(group.getInviteCodeExpiresAt()))
            throw new IllegalStateException("This invite code has expired. Ask the group owner to regenerate it.");
        if (groupRepository.isMember(group.getId(), user)) {
            return groupRepository.findAllByMember(user).stream()
                .filter(g -> g.getId().equals(group.getId())).findFirst().map(this::toResponse).orElseThrow();
        }
        if (memberRepository.countByGroup(group) >= MAX_MEMBERS)
            throw new IllegalStateException("This group has reached the maximum of " + MAX_MEMBERS + " members.");
        GroupMember member = GroupMember.builder().group(group).user(user).role(GroupMember.Role.MEMBER).build();
        try {
            memberRepository.saveAndFlush(member);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            return groupRepository.findAllByMember(user).stream()
                .filter(g -> g.getId().equals(group.getId())).findFirst().map(this::toResponse).orElseThrow();
        }
        return groupRepository.findAllByMember(user).stream()
            .filter(g -> g.getId().equals(group.getId())).findFirst().map(this::toResponse).orElseThrow();
    }

    @Transactional
    public void leave(UUID userId, UUID groupId) {
        User  user  = getUser(userId);
        Group group = groupRepository.findByIdWithMembers(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found."));
        if (group.getOwner().getId().equals(userId))
            throw new IllegalStateException("You are the owner — transfer ownership or dissolve the group instead.");
        GroupMember member = group.getMembers().stream()
            .filter(m -> m.getUser().getId().equals(userId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this group."));
        group.getMembers().remove(member);
        groupRepository.save(group);
    }

    @Transactional
    public void removeMember(UUID requesterId, UUID groupId, UUID targetUserId) {
        User  requester = getUser(requesterId);
        Group group     = groupRepository.findByIdWithMembers(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found."));
        if (!group.getOwner().getId().equals(requester.getId()))
            throw new SecurityException("Only the group owner can perform this action.");
        if (requesterId.equals(targetUserId))
            throw new IllegalStateException("Use 'leave' to remove yourself.");
        GroupMember member = group.getMembers().stream()
            .filter(m -> m.getUser().getId().equals(targetUserId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("User is not a member of this group."));
        group.getMembers().remove(member);
        groupRepository.save(group);
    }

    @Transactional
    public void dissolve(UUID userId, UUID groupId) {
        User  user  = getUser(userId);
        Group group = getGroupAndVerifyOwner(groupId, user);
        // Delete child rows first — no ON DELETE CASCADE on expense/budget FKs
        expenseRepository.deleteAllByGroup(group);
        budgetRepository.deleteAllByGroup(group);
        // GroupMember rows cascade via CascadeType.ALL on Group.members
        groupRepository.delete(group);
    }

    @Transactional
    public GroupDto.Response regenerateInviteCode(UUID userId, UUID groupId) {
        User  user  = getUser(userId);
        Group group = getGroupAndVerifyOwner(groupId, user);
        group.setInviteCode(generateInviteCode());
        group.setInviteCodeExpiresAt(LocalDateTime.now().plusDays(CODE_TTL_DAYS));
        return toResponse(groupRepository.save(group));
    }

    /* ─── Internal helpers ── */

    private Group getGroupAndVerifyMember(UUID groupId, User user) {
        Group group = groupRepository.findByIdWithMembers(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found."));
        if (!groupRepository.isMember(groupId, user))
            throw new SecurityException("You are not a member of this group.");
        return group;
    }

    private Group getGroupAndVerifyOwner(UUID groupId, User user) {
        Group group = groupRepository.findByIdWithMembers(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found."));
        if (!group.getOwner().getId().equals(user.getId()))
            throw new SecurityException("Only the group owner can perform this action.");
        return group;
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private String generateInviteCode() {
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(INVITE_LEN);
        for (int i = 0; i < INVITE_LEN; i++) sb.append(INVITE_CHARS.charAt(rng.nextInt(INVITE_CHARS.length())));
        String code = sb.toString();
        return groupRepository.findByInviteCode(code).isPresent() ? generateInviteCode() : code;
    }

    GroupDto.Response toResponse(Group g) {
        GroupDto.Response r = new GroupDto.Response();
        r.setId(g.getId()); r.setName(g.getName());
        r.setInviteCode(g.getInviteCode());
        r.setInviteCodeExpiresAt(g.getInviteCodeExpiresAt());
        r.setInviteCodeExpired(LocalDateTime.now().isAfter(g.getInviteCodeExpiresAt()));
        r.setOwnerId(g.getOwner().getId());
        r.setCreatedAt(g.getCreatedAt());
        r.setMembers(g.getMembers().stream().map(m -> {
            GroupDto.MemberResponse mr = new GroupDto.MemberResponse();
            mr.setId(m.getId()); mr.setUserId(m.getUser().getId());
            mr.setName(m.getUser().getName()); mr.setEmail(m.getUser().getEmail());
            mr.setAvatar(m.getUser().getAvatar()); mr.setRole(m.getRole()); mr.setJoinedAt(m.getJoinedAt());
            return mr;
        }).toList());
        return r;
    }

    public static class RateLimitException extends RuntimeException {
        private final long retryAfterSeconds;
        public RateLimitException(String message, long retryAfterSeconds) {
            super(message); this.retryAfterSeconds = retryAfterSeconds;
        }
        public long getRetryAfterSeconds() { return retryAfterSeconds; }
    }
}