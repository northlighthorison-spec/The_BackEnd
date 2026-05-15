package com.wha.config;

import com.wha.entity.User;
import com.wha.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_EMAIL = "admin@whaid.org";
    private static final String ADMIN_PASSWORD = "Admin@WHA2024!";

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail(ADMIN_EMAIL).isEmpty()) {
            User admin = User.builder()
                .email(ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                .firstName("Super")
                .lastName("Admin")
                .role(User.Role.SUPER_ADMIN)
                .active(true)
                .emailVerified(true)
                .build();
            userRepository.save(admin);
            log.info("Super admin account created: {}", ADMIN_EMAIL);
        }
    }
}
