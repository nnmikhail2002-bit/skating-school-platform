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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AppUserMapper appUserMapper;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TrainerRepository trainerRepository;

    @InjectMocks
    private AuthService authService;


    private Trainer trainer() {
        return new Trainer(
                2L,
                "Trainer",
                "Test",
                "89999999999",
                10,
                true,
                new HashSet<>()
        );
    }


    // REGISTER STUDENT //

    @Test
    void shouldRegisterStudentSuccessfully() {

        RegisterStudentRequest request = mock(RegisterStudentRequest.class);

        AppUserResponse response = mock(AppUserResponse.class);


        when(request.getEmail())
                .thenReturn("  STUDENT@Test.COM  ");

        when(request.getPhone())
                .thenReturn("89999999999");

        when(request.getFirstName())
                .thenReturn("Mikhail");

        when(request.getLastName())
                .thenReturn("Test");

        when(request.getPassword())
                .thenReturn("password");

        when(appUserRepository.existsByEmail("student@test.com"))
                .thenReturn(false);

        when(studentRepository.existsByPhone("89999999999"))
                .thenReturn(false);

        when(studentRepository.save(any(Student.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(passwordEncoder.encode("password"))
                .thenReturn("encoded-password");

        when(appUserRepository.save(any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(appUserMapper.toResponse(any(AppUser.class)))
                .thenReturn(response);


        AppUserResponse result = authService.register(request);


        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);

        verify(studentRepository).save(studentCaptor.capture());

        verify(appUserRepository).save(userCaptor.capture());


        Student savedStudent = studentCaptor.getValue();

        AppUser savedUser = userCaptor.getValue();


        assertSame(response, result);

        assertEquals(
                "student@test.com",
                savedStudent.getEmail()
        );

        assertEquals(
                "89999999999",
                savedStudent.getPhone()
        );

        assertEquals(
                "Mikhail",
                savedStudent.getFirstName()
        );

        assertEquals(
                "Test",
                savedStudent.getLastName()
        );

        assertTrue(savedStudent.getActive());


        assertEquals(
                "student@test.com",
                savedUser.getEmail()
        );

        assertEquals(
                "encoded-password",
                savedUser.getPassword()
        );

        assertEquals(Role.STUDENT, savedUser.getRole());

        assertTrue(savedUser.getActive());

        assertSame(savedStudent, savedUser.getStudent());

        verify(passwordEncoder)
                .encode("password");
    }


    @Test
    void shouldRejectRegisterWhenEmailAlreadyExists() {

        RegisterStudentRequest request = mock(RegisterStudentRequest.class);

        when(request.getEmail())
                .thenReturn(" STUDENT@Test.COM ");

        when(appUserRepository.existsByEmail("student@test.com"))
                .thenReturn(true);


        ConflictException exception = assertThrows(ConflictException.class, () -> authService.register(request));


        assertEquals(
                "Email is already in use",
                exception.getMessage()
        );

        verifyNoInteractions(studentRepository);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(appUserMapper);

        verify(appUserRepository, never()).save(any(AppUser.class));
    }


    @Test
    void shouldRejectRegisterWhenPhoneAlreadyExists() {

        RegisterStudentRequest request = mock(RegisterStudentRequest.class);

        when(request.getEmail())
                .thenReturn("student@test.com");

        when(request.getPhone())
                .thenReturn("89999999999");

        when(appUserRepository.existsByEmail("student@test.com"))
                .thenReturn(false);

        when(studentRepository.existsByPhone("89999999999"))
                .thenReturn(true);


        ConflictException exception =
                assertThrows(ConflictException.class, () -> authService.register(request));


        assertEquals(
                "Student with this phone already exists",
                exception.getMessage()
        );

        verify(studentRepository, never()).save(any(Student.class));

        verify(appUserRepository, never()).save(any(AppUser.class));

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(appUserMapper);
    }


    // LOGIN //

    @Test
    void shouldLoginSuccessfully() {

        LoginRequest request = mock(LoginRequest.class);

        AppUser user = new AppUser();

        user.setEmail("student@test.com");
        user.setPassword("encoded-password");
        user.setActive(true);
        user.setRole(Role.STUDENT);

        AppUserResponse userResponse = mock(AppUserResponse.class);


        when(request.getEmail())
                .thenReturn("  STUDENT@Test.COM ");

        when(request.getPassword())
                .thenReturn("password");

        when(appUserRepository
                .findByEmail("student@test.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password", "encoded-password"))
                .thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("jwt-token");

        when(appUserMapper.toResponse(user))
                .thenReturn(userResponse);


        AuthResponse result =
                authService.login(request);


        assertEquals(
                "jwt-token",
                result.getToken()
        );

        assertEquals(
                "Bearer",
                result.getType()
        );

        assertSame(userResponse, result.getUser());

        verify(appUserRepository).findByEmail("student@test.com");

        verify(passwordEncoder)
                .matches(
                        "password",
                        "encoded-password"
                );

        verify(jwtService).generateToken(user);
    }


    @Test
    void shouldRejectLoginWhenUserNotFound() {

        LoginRequest request = mock(LoginRequest.class);

        when(request.getEmail())
                .thenReturn(" MISSING@TEST.COM ");

        when(appUserRepository.findByEmail("missing@test.com"))
                .thenReturn(Optional.empty());


        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> authService.login(request));


        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(appUserMapper);
    }


    @Test
    void shouldRejectLoginWhenPasswordIsInvalid() {

        LoginRequest request = mock(LoginRequest.class);

        AppUser user = new AppUser();

        user.setEmail("student@test.com");
        user.setPassword("encoded-password");
        user.setActive(true);


        when(request.getEmail())
                .thenReturn("student@test.com");

        when(request.getPassword())
                .thenReturn("wrong-password");

        when(appUserRepository.findByEmail("student@test.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("wrong-password", "encoded-password"))
                .thenReturn(false);


        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> authService.login(request));


        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(jwtService, never())
                .generateToken(any(AppUser.class));

        verifyNoInteractions(appUserMapper);
    }


    @Test
    void shouldRejectLoginWhenUserIsInactive() {

        LoginRequest request = mock(LoginRequest.class);

        AppUser user = new AppUser();

        user.setEmail("student@test.com");
        user.setPassword("encoded-password");
        user.setActive(false);


        when(request.getEmail())
                .thenReturn("student@test.com");

        when(request.getPassword())
                .thenReturn("password");

        when(appUserRepository.findByEmail("student@test.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password", "encoded-password"))
                .thenReturn(true);


        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> authService.login(request));


        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(jwtService, never()).generateToken(any(AppUser.class));

        verifyNoInteractions(appUserMapper);
    }


    // CREATE TRAINER ACCOUNT //

    @Test
    void shouldCreateTrainerAccountSuccessfully() {

        CreateTrainerAccountRequest request = mock(CreateTrainerAccountRequest.class);

        Trainer trainer = trainer();

        AppUserResponse response = mock(AppUserResponse.class);


        when(request.getEmail())
                .thenReturn(" TRAINER@Test.COM ");

        when(request.getPassword())
                .thenReturn("password");

        when(appUserRepository.existsByEmail("trainer@test.com"))
                .thenReturn(false);

        when(trainerRepository.findById(2L))
                .thenReturn(Optional.of(trainer));

        when(appUserRepository.existsByTrainer_Id(2L))
                .thenReturn(false);

        when(passwordEncoder.encode("password"))
                .thenReturn("encoded-password");

        when(appUserRepository.save(any(AppUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(appUserMapper.toResponse(any(AppUser.class)))
                .thenReturn(response);


        AppUserResponse result = authService.createTrainerAccount(2L, request);


        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);

        verify(appUserRepository).save(captor.capture());

        AppUser savedUser = captor.getValue();


        assertSame(response, result);

        assertEquals(
                "trainer@test.com",
                savedUser.getEmail()
        );

        assertEquals(
                "encoded-password",
                savedUser.getPassword()
        );

        assertEquals(Role.TRAINER, savedUser.getRole());

        assertTrue(savedUser.getActive());

        assertSame(trainer, savedUser.getTrainer());

        verify(passwordEncoder).encode("password");
    }


    @Test
    void shouldRejectCreateTrainerAccountWhenEmailAlreadyExists() {

        CreateTrainerAccountRequest request = mock(CreateTrainerAccountRequest.class);

        when(request.getEmail())
                .thenReturn(" TRAINER@Test.COM ");

        when(appUserRepository.existsByEmail("trainer@test.com"))
                .thenReturn(true);


        ConflictException exception =
                assertThrows(ConflictException.class,
                        () -> authService.createTrainerAccount(2L, request));


        assertEquals(
                "Email is already in use",
                exception.getMessage()
        );

        verifyNoInteractions(trainerRepository);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(appUserMapper);

        verify(appUserRepository, never()).save(any(AppUser.class));
    }


    @Test
    void shouldRejectCreateTrainerAccountWhenTrainerNotFound() {

        CreateTrainerAccountRequest request = mock(CreateTrainerAccountRequest.class);

        when(request.getEmail())
                .thenReturn("trainer@test.com");

        when(appUserRepository.existsByEmail("trainer@test.com"))
                .thenReturn(false);

        when(trainerRepository.findById(2L))
                .thenReturn(Optional.empty());


        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> authService.createTrainerAccount(2L, request));


        assertEquals(
                "Trainer not found",
                exception.getMessage()
        );

        verify(appUserRepository, never()).existsByTrainer_Id(2L);

        verify(appUserRepository, never()).save(any(AppUser.class));

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(appUserMapper);
    }


    @Test
    void shouldRejectCreateTrainerAccountWhenTrainerAlreadyHasAccount() {

        CreateTrainerAccountRequest request = mock(CreateTrainerAccountRequest.class);

        Trainer trainer = trainer();


        when(request.getEmail())
                .thenReturn("trainer@test.com");

        when(appUserRepository.existsByEmail("trainer@test.com"))
                .thenReturn(false);

        when(trainerRepository.findById(2L))
                .thenReturn(Optional.of(trainer));

        when(appUserRepository.existsByTrainer_Id(2L))
                .thenReturn(true);


        ConflictException exception =
                assertThrows(ConflictException.class,
                        () -> authService.createTrainerAccount(2L, request));


        assertEquals(
                "Trainer already has an account",
                exception.getMessage()
        );

        verify(appUserRepository, never()).save(any(AppUser.class));

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(appUserMapper);
    }
}