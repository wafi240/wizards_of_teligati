package com.teligati.backend.security;

import com.teligati.backend.entity.User;
import com.teligati.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("userSecurity")
public class UserSecurity {

    private final UserRepository userRepository;

    public UserSecurity(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isVerified(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String identifier = authentication.getName();
        Optional<User> user = userRepository.findByIdentifier(identifier);
        return user.map(User::isVerified).orElse(false);
    }
}
