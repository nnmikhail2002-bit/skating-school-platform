package com.skating.platform.backend.config;

import com.skating.platform.backend.entity.AppUser;
import com.skating.platform.backend.entity.Role;
import com.skating.platform.backend.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    public AdminBootstrap(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }

        String email = adminEmail
                .trim()
                .toLowerCase();

        AppUser existingUser = appUserRepository
                .findByEmail(email)
                .orElse(null);

        if (existingUser != null) {

            if (existingUser.getRole() != Role.ADMIN) {
                throw new IllegalStateException(
                        "Admin email is already used by a non-admin account"
                );
            }

            return;
        }

        if (adminPassword.length() < 8) {
            throw new IllegalStateException(
                    "Admin password must contain at least 8 characters"
            );
        }

        AppUser admin = new AppUser();

        admin.setEmail(email);
        admin.setPassword(
                passwordEncoder.encode(adminPassword)
        );
        admin.setRole(Role.ADMIN);
        admin.setActive(true);

        appUserRepository.save(admin);
    }
}