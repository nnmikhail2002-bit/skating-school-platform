package com.skating.platform.backend.controller;

import com.skating.platform.backend.dto.booking.request.CreateBookingRequest;
import com.skating.platform.backend.dto.booking.response.BookingResponse;
import com.skating.platform.backend.dto.trainer.response.TrainerResponse;
import com.skating.platform.backend.entity.BookingStatus;
import com.skating.platform.backend.exception.ConflictException;
import com.skating.platform.backend.exception.ResourceNotFoundException;
import com.skating.platform.backend.exception.UnauthorizedException;
import com.skating.platform.backend.security.SecurityConfig;
import com.skating.platform.backend.service.AuthService;
import com.skating.platform.backend.service.BookingService;
import com.skating.platform.backend.service.TrainerService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(BookingController.class)
@Import(SecurityConfig.class)
public class BookingControllerTest {
    @Autowired
    private MockMvc mockMvc;


    @MockitoBean
    private BookingService bookingService;

    @Test
    void shouldShowCurrentStudentBookingsSuccessfully() throws Exception {


        BookingResponse response =
                new BookingResponse(
                        1L,
                        2L,
                        3L,
                        LocalDateTime.of(2026, 10, 14, 15, 30),
                        BookingStatus.BOOKED
                );
        Page<BookingResponse> page =
                new PageImpl<>(
                        List.of(response),
                        PageRequest.of(0, 10),
                        1
                );

        when(bookingService.getCurrentBookings(eq("Student@test.com"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(
                        get("/api/bookings/me")
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .tokenValue("test-token")
                                                .subject("Student@test.com")
                                                .claim("userId", 1L)
                                                .claim("role", "STUDENT")
                                        )
                                        .authorities(
                                                new SimpleGrantedAuthority("ROLE_STUDENT")
                                        )
                                )
                                .param("page", "1")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void shouldRejectShowWithNotFoundProfile() throws Exception {

        when(bookingService.getCurrentBookings(eq("Student@test.com"), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Student profile not found"));

        mockMvc.perform(
                        get("/api/bookings/me")
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .tokenValue("test-token")
                                                .subject("Student@test.com")
                                                .claim("userId", 1L)
                                                .claim("role", "STUDENT")
                                        )
                                        .authorities(
                                                new SimpleGrantedAuthority("ROLE_STUDENT")
                                        )
                                )
                                .param("page", "1")
                                .param("size", "10")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturn401WhenJwtIsMissing() throws Exception {

        mockMvc.perform(
                        get("/api/bookings/me")
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenUserHasWrongRole() throws Exception {

        mockMvc.perform(
                        get("/api/bookings/me")
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .tokenValue("test-token")
                                                .subject("NotStudent@test.com")
                                                .claim("userId", 1L)
                                                .claim("role", "NotStudent")
                                        )
                                        .authorities(
                                                new SimpleGrantedAuthority("ROLE_ADMIN")
                                        )
                                )
                                .param("page", "0")
                                .param("size", "10")
                                .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());
    }


    @Test
    void shouldCreateBookingSuccessfully() throws Exception {
        BookingResponse response =
                new BookingResponse(
                        1L,
                        2L,
                        3L,
                        LocalDateTime.of(2026, 10, 14, 15, 30),
                        BookingStatus.BOOKED
                );

        when(bookingService.createBooking(any(), eq("Student@test.com")))
                .thenReturn(response);

        String json = """
                {
                   "trainingSessionId": 1
                }
                """;
        mockMvc.perform(
                        post("/api/bookings")
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .tokenValue("test-token")
                                                .subject("Student@test.com")
                                                .claim("userId", 1L)
                                                .claim("role", "STUDENT")
                                        )
                                        .authorities(
                                                new SimpleGrantedAuthority("ROLE_STUDENT")
                                        )
                                )
                                .contentType(APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRejectWithConflict() throws Exception {

        when(bookingService.createBooking(any(), eq("Student@test.com")))
                .thenThrow(new ConflictException("Student is already booked for this training session"));


        String json = """
                {
                   "trainingSessionId": 1
                }
                """;
        mockMvc.perform(
                        post("/api/bookings")
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .tokenValue("test-token")
                                                .subject("Student@test.com")
                                                .claim("userId", 1L)
                                                .claim("role", "STUDENT")
                                        )
                                        .authorities(
                                                new SimpleGrantedAuthority("ROLE_STUDENT")
                                        )
                                )
                                .contentType(APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").exists());
    }


    @Test
    void shouldReturn401WithJwtIsMissing() throws Exception {

        when(bookingService.createBooking(any(), eq("Student@test.com")))
                .thenThrow(new UnauthorizedException("Authentication required"));

        String json = """
                {
                   "trainingSessionId": 1
                }
                """;

        mockMvc.perform(
                        post("/api/bookings")
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .tokenValue("test-token")
                                                .subject("Student@test.com")
                                                .claim("userId", 1L)
                                                .claim("role", "STUDENT")
                                        )
                                        .authorities(
                                                new SimpleGrantedAuthority("ROLE_STUDENT")
                                        )
                                )
                                .contentType(APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldRejectWithInvalidData() throws Exception {

        String json = """
                {
                   "trainingSessionId": null
                }
                """;

        mockMvc.perform(
                        post("/api/bookings")
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .tokenValue("test-token")
                                                .subject("Student@test.com")
                                                .claim("userId", 1L)
                                                .claim("role", "STUDENT")
                                        )
                                        .authorities(
                                                new SimpleGrantedAuthority("ROLE_STUDENT")
                                        )
                                )
                                .contentType(APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldRejectWithErrorRole() throws Exception {

        String json = """
                {
                   "trainingSessionId": 1
                }
                """;

        mockMvc.perform(
                        post("/api/bookings")
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .tokenValue("test-token")
                                                .subject("Student@test.com")
                                                .claim("userId", 1L)
                                                .claim("role", "ADNIN")
                                        )
                                        .authorities(
                                                new SimpleGrantedAuthority("ROLE_ADMIN")
                                        )
                                )
                                .contentType(APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectWithNotFoundStudent() throws Exception {

        when(bookingService.createBooking(any(), eq("Student@test.com")))
                .thenThrow(new ResourceNotFoundException("Student profile not found"));

        String json = """
                {
                   "trainingSessionId": 1
                }
                """;
        mockMvc.perform(
                        post("/api/bookings")
                                .with(jwt()
                                        .jwt(jwt -> jwt
                                                .tokenValue("test-token")
                                                .subject("Student@test.com")
                                                .claim("userId", 1L)
                                                .claim("role", "STUDENT")
                                        )
                                        .authorities(
                                                new SimpleGrantedAuthority("ROLE_STUDENT")
                                        )
                                )
                                .contentType(APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

}