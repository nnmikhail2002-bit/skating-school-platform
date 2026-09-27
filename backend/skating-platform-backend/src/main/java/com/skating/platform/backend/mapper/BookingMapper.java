package com.skating.platform.backend.mapper;

import com.skating.platform.backend.dto.booking.response.BookingResponse;
import com.skating.platform.backend.entity.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {
    public BookingResponse toResponse(Booking booking){
        return new BookingResponse(
                booking.getId(),
                booking.getStudent().getId(),
                booking.getTrainingSession().getId(),
                booking.getBookedAt(),
                booking.getStatus()
        );
    }
}
