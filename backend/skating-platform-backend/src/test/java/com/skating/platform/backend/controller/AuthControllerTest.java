package com.skating.platform.backend.controller;

import com.skating.platform.backend.dto.appusers.response.AppUserResponse;
import com.skating.platform.backend.dto.appusers.response.AuthResponse;
import com.skating.platform.backend.entity.Role;

import com.skating.platform.backend.exception.ConflictException;
import com.skating.platform.backend.exception.UnauthorizedException;
import com.skating.platform.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verifyNoInteractions;

@WebMvcTest(AuthController.class)
class AuthControllerTest {


    @Autowired
    private MockMvc mockMvc;


    @MockitoBean
    private AuthService authService;

    @Test
    void shouldRegisterStudentSuccessfully() throws Exception {
        AppUserResponse response =
                new AppUserResponse(
                        1L,
                        "student@test.com",
                        Role.STUDENT,
                        true
                );
        when(authService.register(any()))
                .thenReturn(response);


        String json = """
                {
                   "email": "student@test.com",
                   "password": "password123"
                }
                """;
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("student@test.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void shouldRejectRegistrationWithInvalidEmail() throws Exception {

        String json = """
                {
                  "firstName": "Михаил",
                  "lastName": "Test",
                  "phone": "+79990000001",
                  "email": "wrong-email",
                  "password": "password123"
                }
                """;


        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
        verifyNoInteractions(authService);
    }

    @Test
    void shouldRejectRegistrationWithExistingEmail() throws Exception {

        String json = """
                {
                   "firstName": "Михаил",
                   "lastName": "Test",
                   "phone": "+79990000001",
                   "email": "student@test.com",
                   "password": "password123"
                }
                """;
        when(authService.register(any()))
                .thenThrow(new ConflictException("Email is already in use"));

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(json)
                ).andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Email is already in use"));
        verifyNoInteractions(authService);
    }

    @Test
    void shoudLoginStudentSuccessfully() throws Exception {

        AuthResponse response =
                new AuthResponse(
                        "token",
                        "Bearer",
                        new AppUserResponse()
                );

        when(authService.login(any()))
                .thenReturn(response);

        String json = """
                {
                   "email": "student@test.com",
                   "password": "password123"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.type").value("Bearer"));
    }


    @Test
    void shouldRejectLoginWithInvalidPassword() throws Exception {
        String json = """
                {
                  "email": "student@test.com",
                  "password": "wrong-password"
                }
                """;

        when(authService.login(any()))
                .thenThrow(new UnauthorizedException("Invalid email or password"));

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldRejectLoginNoActiveUser() throws Exception {

        when(authService.login(any()))
                .thenThrow(new UnauthorizedException("Invalid email or password"));

        String json = """
                {
                   "email": "student@test.com",
                   "password": "password123"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(json)
                )  .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());

    }

}