package com.example.finset.service;

import com.example.finset.entity.User;
import com.example.finset.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
        return userRepository
                .findByProviderAndProviderId(User.AuthProvider.GOOGLE, providerId)
                .map(existing -> {
                    existing.setName(name);
                    existing.setAvatar(picture);
                    return userRepository.save(existing);
                })
                .orElseGet(() ->
                    userRepository.findByEmail(email)
                            .map(existing -> {
                                existing.setProvider(User.AuthProvider.GOOGLE);
                                existing.setProviderId(providerId);
                                existing.setAvatar(picture);
                                return userRepository.save(existing);
                            })
                            .orElseGet(() -> {
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

    // update preferred currency for a user — only USD or KHR accepted
    @Transactional
    public User updatePreferredCurrency(UUID userId, String currency) {
        if (!"USD".equals(currency) && !"KHR".equals(currency)) {
            throw new IllegalArgumentException("Currency must be USD or KHR");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPreferredCurrency(currency);
        return userRepository.save(user);
    }
}