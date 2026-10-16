package com.skating.platform.backend.controller;

import com.skating.platform.backend.dto.service.request.CreateSchoolServiceRequest;
import com.skating.platform.backend.dto.service.request.UpdateSchoolServiceRequest;
import com.skating.platform.backend.dto.service.response.SchoolServiceResponse;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.security.SecurityConfig;
import com.skating.platform.backend.service.SchoolServiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(SchoolServiceController.class)
@Import(SecurityConfig.class)
class SchoolServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SchoolServiceService schoolServiceService;


    private RequestPostProcessor userJwt(String email, String role) {

        return jwt()
                .jwt(jwt -> jwt
                        .tokenValue("test-token")
                        .subject(email)
                        .claim("userId", 1L)
                        .claim("role", role)
                )
                .authorities(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );
    }

    private SchoolServiceResponse response() {

        return new SchoolServiceResponse(
                1L,
                "Group training",
                "Group figure skating lesson",
                new BigDecimal("2500.00"),
                "GROUP",
                true
        );
    }

    private Page<SchoolServiceResponse> page() {

        return new PageImpl<>(
                List.of(response()),
                PageRequest.of(0, 10),
                1
        );
    }

    private String createJson() {

        return """
                {
                    "name": "Group training",
                    "description": "Group figure skating lesson",
                    "price": 2500.00,
                    "type": "GROUP"
                }
                """;
    }

    private String updateJson() {

        return """
                {
                    "name": "Updated group training",
                    "description": "Updated description",
                    "price": 3000.00,
                    "type": "GROUP"
                }
                """;
    }


    @Test
    void shouldGetAllServicesSuccessfully() throws Exception {

        when(schoolServiceService.getAllServices(any(Pageable.class))).thenReturn(page());

        mockMvc.perform(
                        get("/api/services")
                                .with(userJwt(
                                        "student@test.com",
                                        "STUDENT"
                                ))
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content[0].id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.content[0].name")
                                .value("Group training")
                );
    }

    @Test
    void shouldReturn401WhenAuthenticationIsMissing() throws Exception {

        mockMvc.perform(
                        get("/api/services")
                )
                .andExpect(status().isUnauthorized());
    }


    @Test
    void shouldGetServiceByIdSuccessfully() throws Exception {

        when(schoolServiceService.getServiceById(1L))
                .thenReturn(response());

        mockMvc.perform(
                        get("/api/services/1")
                                .with(userJwt(
                                        "student@test.com",
                                        "STUDENT"
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Group training")
                )
                .andExpect(
                        jsonPath("$.type")
                                .value("GROUP")
                );
    }

    @Test
    void shouldReturn404WhenServiceNotFound() throws Exception {

        when(schoolServiceService.getServiceById(1L))
                .thenThrow(new ResourceNotFoundException("Service not found"));

        mockMvc.perform(
                        get("/api/services/1")
                                .with(userJwt(
                                        "student@test.com",
                                        "STUDENT"
                                ))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                );
    }


    @Test
    void shouldCreateServiceSuccessfully() throws Exception {

        when(schoolServiceService.createService(any(CreateSchoolServiceRequest.class)))
                .thenReturn(response());

        mockMvc.perform(
                        post("/api/services")
                                .with(userJwt(
                                        "admin@test.com",
                                        "ADMIN"
                                ))
                                .contentType(APPLICATION_JSON)
                                .content(createJson())
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Group training")
                );
    }

    @Test
    void shouldRejectCreateWithInvalidData() throws Exception {

        String json = """
                {
                    "name": "",
                    "description": "Test",
                    "price": -1,
                    "type": "GROUP"
                }
                """;

        mockMvc.perform(
                        post("/api/services")
                                .with(userJwt(
                                        "admin@test.com",
                                        "ADMIN"
                                ))
                                .contentType(APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                );
    }

    @Test
    void shouldReturn403WhenStudentCreatesService() throws Exception {

        mockMvc.perform(
                        post("/api/services")
                                .with(userJwt(
                                        "student@test.com",
                                        "STUDENT"
                                ))
                                .contentType(APPLICATION_JSON)
                                .content(createJson())
                )
                .andExpect(status().isForbidden());
    }



    @Test
    void shouldUpdateServiceSuccessfully() throws Exception {

        SchoolServiceResponse updatedResponse =
                new SchoolServiceResponse(
                        1L,
                        "Updated group training",
                        "Updated description",
                        new BigDecimal("3000.00"),
                        "GROUP",
                        true
                );

        when(schoolServiceService.updateService(eq(1L), any(UpdateSchoolServiceRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(
                        put("/api/services/1")
                                .with(userJwt(
                                        "admin@test.com",
                                        "ADMIN"
                                ))
                                .contentType(APPLICATION_JSON)
                                .content(updateJson())
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Updated group training")
                );
    }

    @Test
    void shouldRejectUpdateWithInvalidData() throws Exception {

        String json = """
                {
                    "name": "",
                    "description": "Test",
                    "price": -500,
                    "type": "GROUP"
                }
                """;

        mockMvc.perform(
                        put("/api/services/1")
                                .with(userJwt(
                                        "admin@test.com",
                                        "ADMIN"
                                ))
                                .contentType(APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                );
    }

    @Test
    void shouldReturn404WhenUpdatingUnknownService() throws Exception {

        when(schoolServiceService.updateService(eq(1L), any(UpdateSchoolServiceRequest.class)))
                .thenThrow(new ResourceNotFoundException("Service not found"));

        mockMvc.perform(
                        put("/api/services/1")
                                .with(userJwt(
                                        "admin@test.com",
                                        "ADMIN"
                                ))
                                .contentType(APPLICATION_JSON)
                                .content(updateJson())
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                );
    }

    @Test
    void shouldReturn403WhenTrainerUpdatesService() throws Exception {

        mockMvc.perform(
                        put("/api/services/1")
                                .with(userJwt(
                                        "trainer@test.com",
                                        "TRAINER"
                                ))
                                .contentType(APPLICATION_JSON)
                                .content(updateJson())
                )
                .andExpect(status().isForbidden());
    }


    @Test
    void shouldDeleteServiceSuccessfully() throws Exception {

        mockMvc.perform(
                        delete("/api/services/1")
                                .with(userJwt(
                                        "admin@test.com",
                                        "ADMIN"
                                ))
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingUnknownService() throws Exception {

        doThrow(new ResourceNotFoundException("Service not found"))
                .when(schoolServiceService)
                .deleteService(1L);

        mockMvc.perform(
                        delete("/api/services/1")
                                .with(userJwt(
                                        "admin@test.com",
                                        "ADMIN"
                                ))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                );
    }

    @Test
    void shouldReturn403WhenStudentDeletesService() throws Exception {

        mockMvc.perform(
                        delete("/api/services/1")
                                .with(userJwt(
                                        "student@test.com",
                                        "STUDENT"
                                ))
                )
                .andExpect(status().isForbidden());
    }


    @Test
    void shouldSearchServicesSuccessfully() throws Exception {

        when(schoolServiceService.searchServices(eq("group"), any(Pageable.class))).thenReturn(page());

        mockMvc.perform(
                        get("/api/services/search")
                                .with(userJwt(
                                        "student@test.com",
                                        "STUDENT"
                                ))
                                .param("query", "group")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content[0].id")
                                .value(1)
                );
    }

    @Test
    void shouldReturn400WhenSearchQueryIsMissing() throws Exception {

        mockMvc.perform(
                        get("/api/services/search")
                                .with(userJwt(
                                        "student@test.com",
                                        "STUDENT"
                                ))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFilterServicesSuccessfully() throws Exception {

        when(schoolServiceService.filterServices(
                eq("Group"),
                eq("GROUP"),
                eq(new BigDecimal("1000")),
                eq(new BigDecimal("5000")),
                eq(true),
                any(Pageable.class)
        )).thenReturn(page());

        mockMvc.perform(
                        get("/api/services/filter")
                                .with(userJwt(
                                        "student@test.com",
                                        "STUDENT"
                                ))
                                .param("name", "Group")
                                .param("type", "GROUP")
                                .param("minPrice", "1000")
                                .param("maxPrice", "5000")
                                .param("active", "true")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content[0].name")
                                .value("Group training")
                );
    }
}