package com.example.finset.repository;

import com.example.finset.entity.Group;
import com.example.finset.entity.GroupMember;
import com.example.finset.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    Optional<GroupMember> findByGroupAndUser(Group group, User user);

    int countByGroup(Group group);
}