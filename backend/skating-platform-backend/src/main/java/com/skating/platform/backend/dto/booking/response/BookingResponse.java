package com.skating.platform.backend.dto.booking.response;

import com.skating.platform.backend.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private Long studentId;
    private Long trainingSessionId;
    private LocalDateTime bookedAt;
    private BookingStatus status;
}
