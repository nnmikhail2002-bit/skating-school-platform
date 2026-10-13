package com.skating.platform.backend.controller;

import com.skating.platform.backend.dto.student.response.StudentResponse;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.security.SecurityConfig;
import com.skating.platform.backend.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(StudentController.class)
@Import(SecurityConfig.class)
public class StudentControllerTest {
    @Autowired
    private MockMvc mockMvc;


    @MockitoBean
    private StudentService studentService;

    @Test
    void shouldShowStudentSuccessfully() throws Exception {


        StudentResponse response =
                new StudentResponse(
                        1L,
                        "firstName",
                        "lastName",
                        "89873944420",
                        "student@test.com",
                        true
                );

        when(studentService.getCurrentStudent("student@test.com"))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/students/me")
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .tokenValue("test-token")
                                                .subject("student@test.com")
                                                .claim("id", 1L)
                                                .claim("role", "STUDENT")
                                        )
                                        .authorities(
                                                new SimpleGrantedAuthority("ROLE_STUDENT")
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@test.com"));
    }

    @Test
    void shouldRejectShowWithNotFoundProfile() throws Exception {

        when(studentService.getCurrentStudent("student@test.com"))
                .thenThrow(new ResourceNotFoundException("Student profile not found"));

        mockMvc.perform(
                        get("/api/students/me")
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .tokenValue("test-token")
                                                .subject("student@test.com")
                                                .claim("id", 1L)
                                                .claim("role", "STUDENT")
                                        )
                                        .authorities(
                                                new SimpleGrantedAuthority("ROLE_STUDENT")
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
                        get("/api/students/me")
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenUserHasWrongRole() throws Exception {

        mockMvc.perform(
                        get("/api/students/me")
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .tokenValue("test-token")
                                                .subject("Notstudent@test.com")
                                                .claim("id", 1L)
                                                .claim("role", "NotStudent")
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
