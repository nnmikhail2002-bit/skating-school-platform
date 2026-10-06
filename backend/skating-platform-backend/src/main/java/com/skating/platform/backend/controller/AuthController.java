package com.skating.platform.backend.controller;

import com.skating.platform.backend.dto.appusers.request.LoginRequest;
import com.skating.platform.backend.dto.appusers.request.RegisterStudentRequest;
import com.skating.platform.backend.dto.appusers.response.AppUserResponse;
import com.skating.platform.backend.dto.appusers.response.AuthResponse;
import com.skating.platform.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration and authentication")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @Operation(
            summary = "Register student",
            description = "Registers a new student account and creates the linked student profile"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Student account successfully registered"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid registration data"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email or phone number is already in use"
            )
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AppUserResponse register(@Valid @RequestBody RegisterStudentRequest request) {
        return authService.register(request);
    }

    @Operation(
            summary = "Login user",
            description = "Authenticates a user and returns a JWT access token"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid login request data"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid email or password"
            )
    })
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

}
