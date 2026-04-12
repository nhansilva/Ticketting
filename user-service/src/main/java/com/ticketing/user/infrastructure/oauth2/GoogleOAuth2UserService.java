package com.ticketing.user.infrastructure.oauth2;

import com.ticketing.user.entity.User;
import com.ticketing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuth2UserService extends DefaultReactiveOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public Mono<OAuth2User> loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        return super.loadUser(userRequest)
                .flatMap(oauth2User -> {
                    String googleId = oauth2User.getAttribute("sub");
                    String email    = oauth2User.getAttribute("email");
                    String firstName = oauth2User.getAttribute("given_name");
                    String lastName  = oauth2User.getAttribute("family_name");
                    String picture   = oauth2User.getAttribute("picture");

                    if (email == null) {
                        return Mono.error(new OAuth2AuthenticationException("Email not provided by Google"));
                    }

                    return userRepository.findByEmail(email)
                            .flatMap(existingUser -> {
                                // User đã có — update googleId nếu chưa có
                                if (existingUser.getGoogleId() == null) {
                                    existingUser.setGoogleId(googleId);
                                    existingUser.setUpdatedAt(LocalDateTime.now());
                                    if (picture != null && existingUser.getProfileImageUrl() == null) {
                                        existingUser.setProfileImageUrl(picture);
                                    }
                                    return userRepository.save(existingUser);
                                }
                                return Mono.just(existingUser);
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                // User chưa có — tạo mới
                                User newUser = User.builder()
                                        .id(UUID.randomUUID())
                                        .isNew(true)
                                        .email(email)
                                        .googleId(googleId)
                                        .authProvider(User.AuthProvider.GOOGLE)
                                        .firstName(firstName != null ? firstName : "")
                                        .lastName(lastName != null ? lastName : "")
                                        .profileImageUrl(picture)
                                        .emailVerified(true)
                                        .role(User.UserRole.CUSTOMER)
                                        .status(User.UserStatus.ACTIVE)
                                        .createdAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                        .build();
                                log.info("Creating new user from Google OAuth2: {}", email);
                                return userRepository.save(newUser);
                            }))
                            .map(user -> new GoogleOAuth2User(oauth2User, user));
                });
    }
}
