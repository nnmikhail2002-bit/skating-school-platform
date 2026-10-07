package com.skating.platform.backend.service;

import com.skating.platform.backend.dto.appusers.request.CreateTrainerAccountRequest;
import com.skating.platform.backend.dto.appusers.request.LoginRequest;
import com.skating.platform.backend.dto.appusers.request.RegisterStudentRequest;
import com.skating.platform.backend.dto.appusers.response.AppUserResponse;
import com.skating.platform.backend.dto.appusers.response.AuthResponse;
import com.skating.platform.backend.entity.AppUser;
import com.skating.platform.backend.entity.Role;
import com.skating.platform.backend.entity.Student;
import com.skating.platform.backend.entity.Trainer;
import com.skating.platform.backend.exception.ConflictException;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.exception.UnauthorizedException;
import com.skating.platform.backend.mapper.AppUserMapper;
import com.skating.platform.backend.repository.AppUserRepository;
import com.skating.platform.backend.repository.StudentRepository;
import com.skating.platform.backend.repository.TrainerRepository;
import com.skating.platform.backend.security.JwtService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final AppUserMapper appUserMapper;
    private final AppUserRepository appUserRepository;
    private final JwtService jwtService;
    private final StudentRepository studentRepository;
    private final TrainerRepository trainerRepository;

    public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, AppUserMapper appUserMapper, JwtService jwtService,
                       TrainerRepository trainerRepository, StudentRepository studentRepository) {
        this.appUserMapper = appUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.appUserRepository = appUserRepository;
        this.jwtService = jwtService;
        this.studentRepository = studentRepository;
        this.trainerRepository = trainerRepository;
    }

    @Transactional
    public AppUserResponse register(RegisterStudentRequest request) {
        String email = request.getEmail()
                .trim()
                .toLowerCase();

        if (appUserRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already in use");
        }

        Student student = new Student();

        if (studentRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException(
                    "Student with this phone already exists"
            );
        }

        student.setPhone(request.getPhone());
        student.setEmail(email);
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setActive(true);

        Student savedStudent = studentRepository.save(student);

        AppUser user = new AppUser();
        user.setStudent(savedStudent);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.STUDENT);
        user.setActive(true);
        AppUser saved = appUserRepository.save(user);
        return appUserMapper.toResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail()
                .trim()
                .toLowerCase();

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password")
                );
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!passwordMatches) throw new UnauthorizedException("Invalid email or password");
        if (!user.getActive()) throw new UnauthorizedException("Invalid email or password");

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, "Bearer", appUserMapper.toResponse(user));

    }

    @Transactional
    public AppUserResponse createTrainerAccount(Long trainerId, CreateTrainerAccountRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        if (appUserRepository.existsByEmail(email)) {
            throw new ConflictException(
                    "Email is already in use"
            );
        }

        Trainer trainer = trainerRepository.findById(trainerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trainer not found"
                        )
                );

        if (appUserRepository.existsByTrainer_Id(trainerId)) {
            throw new ConflictException(
                    "Trainer already has an account"
            );
        }

        AppUser user = new AppUser();

        user.setEmail(email);

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setRole(Role.TRAINER);

        user.setTrainer(trainer);

        user.setActive(true);

        AppUser saved = appUserRepository.save(user);

        return appUserMapper.toResponse(saved);
    }
}
