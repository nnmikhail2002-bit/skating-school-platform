package com.skating.platform.backend.controller;

import com.skating.platform.backend.dto.booking.request.CreateBookingRequest;
import com.skating.platform.backend.dto.booking.response.BookingResponse;
import com.skating.platform.backend.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Management of bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService){
        this.bookingService = bookingService;
    }

    @Operation(
            summary = "Get all bookings",
            description = "Returns all bookings with pagination and sorting"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bookings successfully returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required")
    })
    @GetMapping
    public Page<BookingResponse> getAllBookings(Pageable pageable){
        return bookingService.getAllBookings(pageable);
    }


    @Operation(
            summary = "Get current trainer bookings",
            description = "Returns bookings for training sessions belonging to the authenticated trainer"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trainer bookings successfully returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Trainer role required"),
            @ApiResponse(responseCode = "404", description = "Trainer profile not found")
    })
    @GetMapping("/trainer/me")
    public Page<BookingResponse> getCurrentTrainerBookings(@AuthenticationPrincipal Jwt jwt, Pageable pageable) {
        return bookingService.getCurrentTrainerBookings(jwt.getSubject(), pageable);
    }

    @Operation(
            summary = "Create booking",
            description = "Creates a booking for the authenticated student and selected training session"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid booking data or training session is unavailable"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Student role required"),
            @ApiResponse(responseCode = "404", description = "Student profile or training session not found"),
            @ApiResponse(responseCode = "409", description = "Student is already booked or training session is full")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request,  @AuthenticationPrincipal Jwt jwt){
        return bookingService.createBooking(request, jwt.getSubject());
    }

    @Operation(
            summary = "Get booking by ID",
            description = "Returns a booking by its identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking found"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Administrator role required"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @GetMapping("/{bookingId}")
    public BookingResponse getBookingById(@PathVariable Long bookingId){
        return bookingService.getBookingById(bookingId);
    }

    @Operation(
            summary = "Get current student bookings",
            description = "Returns bookings belonging to the authenticated student"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student bookings successfully returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Student role required"),
            @ApiResponse(responseCode = "404", description = "Student profile not found")
    })
    @GetMapping("/me")
    public Page<BookingResponse> getCurrentBookings(@AuthenticationPrincipal Jwt jwt, Pageable pageable){
        return bookingService.getCurrentBookings(jwt.getSubject(), pageable);
    }

    @Operation(
            summary = "Cancel current student booking",
            description = "Cancels the authenticated student's booking for the specified training session"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking successfully cancelled"),
            @ApiResponse(responseCode = "400", description = "Invalid cancellation request data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Student role required"),
            @ApiResponse(responseCode = "404", description = "Student profile or booking not found")
    })
    @PatchMapping("/cancel")
    public BookingResponse cancelBooking(@Valid @RequestBody CreateBookingRequest request, @AuthenticationPrincipal Jwt jwt){
        return bookingService.cancelBooking(request, jwt.getSubject());
    }

}
