package com.example.finset.service;

import com.example.finset.entity.User;
import com.example.finset.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Called after a successful Google OAuth2 login.
     * Finds the existing user by providerId or email, or creates a new one.
     */
    @Transactional
    public User findOrCreateGoogleUser(String email, String name,
                                       String picture, String providerId) {
        // 1. Try match by Google sub (most reliable)
        return userRepository
                .findByProviderAndProviderId(User.AuthProvider.GOOGLE, providerId)
                .map(existing -> {
                    // Update mutable fields on re-login
                    existing.setName(name);
                    existing.setAvatar(picture);
                    return userRepository.save(existing);
                })
                .orElseGet(() ->
                    // 2. Try match by email (user may have registered with email/pass first)
                    userRepository.findByEmail(email)
                            .map(existing -> {
                                existing.setProvider(User.AuthProvider.GOOGLE);
                                existing.setProviderId(providerId);
                                existing.setAvatar(picture);
                                return userRepository.save(existing);
                            })
                            .orElseGet(() -> {
                                // 3. Brand new user
                                log.info("Creating new Google user: {}", email);
                                return userRepository.save(User.builder()
                                        .email(email)
                                        .name(name)
                                        .avatar(picture)
                                        .provider(User.AuthProvider.GOOGLE)
                                        .providerId(providerId)
                                        .build());
                            })
                );
    }
}