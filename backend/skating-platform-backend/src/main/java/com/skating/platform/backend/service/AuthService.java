package com.skating.platform.backend.service;

import com.skating.platform.backend.dto.appusers.request.CreateAppUserRequest;
import com.skating.platform.backend.dto.appusers.request.LoginRequest;
import com.skating.platform.backend.dto.appusers.response.AppUserResponse;
import com.skating.platform.backend.entity.AppUser;
import com.skating.platform.backend.entity.Role;
import com.skating.platform.backend.exception.ConflictException;
import com.skating.platform.backend.exception.UnauthorizedException;
import com.skating.platform.backend.mapper.AppUserMapper;
import com.skating.platform.backend.repository.AppUserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final AppUserMapper appUserMapper;
    private final AppUserRepository appUserRepository;

    public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, AppUserMapper appUserMapper) {
        this.appUserMapper = appUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public AppUserResponse register(CreateAppUserRequest request) {
        String email = request.getEmail()
                .trim()
                .toLowerCase();

        if (appUserRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already in use");
        }

        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.STUDENT);
        user.setActive(true);
        AppUser saved = appUserRepository.save(user);
        return appUserMapper.toResponse(saved);
    }

    public AppUserResponse login(LoginRequest request) {
        String email = request.getEmail()
                .trim()
                .toLowerCase();

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password")
                );
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!passwordMatches) throw new UnauthorizedException("Invalid email or password");
        if (!user.getActive()) throw new UnauthorizedException("Invalid email or password");

        return appUserMapper.toResponse(user);
    }

}
