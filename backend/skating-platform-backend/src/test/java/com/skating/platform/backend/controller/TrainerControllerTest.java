package com.skating.platform.backend.controller;

import com.skating.platform.backend.dto.appusers.response.AppUserResponse;
import com.skating.platform.backend.dto.trainer.response.TrainerResponse;
import com.skating.platform.backend.entity.Role;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.security.SecurityConfig;
import com.skating.platform.backend.service.AuthService;
import com.skating.platform.backend.service.TrainerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;




@WebMvcTest(TrainerController.class)
@Import(SecurityConfig.class)
public class TrainerControllerTest {
    @Autowired
    private MockMvc mockMvc;


    @MockitoBean
    private TrainerService trainerService;

    @MockitoBean
    private AuthService authService;

    @Test
    void shouldShowTrainerSuccessfully() throws Exception {


        TrainerResponse response =
                new TrainerResponse(
                        1L,
                        "firstName",
                        "lastName",
                        "89873944420",
                        10,
                        true
                );

        when(trainerService.getCurrentTrainer("trainer@test.com"))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/trainers/me")
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .tokenValue("test-token")
                                                .subject("trainer@test.com")
                                                .claim("userId", 1L)
                                                .claim("role", "TRAINER")
                                        )
                                        .authorities(
                                                new SimpleGrantedAuthority("ROLE_TRAINER")
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("firstName"));
    }

    @Test
    void shouldRejectShowWithNotFoundProfile() throws Exception {

        when(trainerService.getCurrentTrainer("trainer@test.com"))
                .thenThrow(new ResourceNotFoundException("Trainer profile not found"));

        mockMvc.perform(
                        get("/api/trainers/me")
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .tokenValue("test-token")
                                                .subject("trainer@test.com")
                                                .claim("userId", 1L)
                                                .claim("role", "TRAINER")
                                        )
                                        .authorities(
                                                new SimpleGrantedAuthority("ROLE_TRAINER")
                                        )
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn401WhenJwtIsMissing() throws Exception {

        mockMvc.perform(
                        get("/api/trainers/me")
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenUserHasWrongRole() throws Exception {

        mockMvc.perform(
                        get("/api/trainers/me")
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .tokenValue("test-token")
                                                .subject("NotTrainer@test.com")
                                                .claim("userId", 1L)
                                                .claim("role", "NotTrainer")
                                        )
                                        .authorities(
                                                new SimpleGrantedAuthority("ROLE_ADMIN")
                                        )
                                )
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());
    }


}