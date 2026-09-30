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

    @Operation(summary = "Get the list of bookings", description =  "Returns bookings with pagination and sorting")
    @GetMapping
    public Page<BookingResponse> getAllBookings(Pageable pageable){
        return bookingService.getAllBookings(pageable);
    }

    @Operation(summary = "Create booking", description =  "Creates booking")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "booking created"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid booking data"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Student or training session not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Student is already booked or training session is full"
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request,  @AuthenticationPrincipal Jwt jwt){
        return bookingService.createBooking(request, jwt.getSubject());
    }

    @Operation(summary = "Get booking by ID", description = "Returns a booking by identifier")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Booking found"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Student or training session not found"
            )
    })
    @GetMapping("/{bookingId}")
    public BookingResponse getBookingById(@PathVariable Long bookingId){
        return bookingService.getBookingById(bookingId);
    }

    @Operation(summary = "Get booking by ID", description = "Returns a booking by identifier")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Booking found"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Student or training session not found"
            )
    })
    @GetMapping("/me")
    public Page<BookingResponse> getCurrentBookings(@AuthenticationPrincipal Jwt jwt, Pageable pageable){
        return bookingService.getCurrentBookings(jwt.getSubject(), pageable);
    }

    @Operation(summary = "Cancel booking by ID", description = "Cancel booking by identifier")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Booking successfully cancelled"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Student or training session not found"
            )
    })
    @PatchMapping("/cancel")
    public BookingResponse cancelBooking(@Valid @RequestBody CreateBookingRequest request, @AuthenticationPrincipal Jwt jwt){
        return bookingService.cancelBooking(request, jwt.getSubject());
    }

}
