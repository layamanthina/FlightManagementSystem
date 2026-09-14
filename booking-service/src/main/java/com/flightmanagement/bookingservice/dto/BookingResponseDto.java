package com.flightmanagement.bookingservice.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponseDto {

    private Long bookingId;
    private Long flightId;
    private String flightNumber;
    private Long userId;
    private String userName;
    private Integer numberOfSeats;
    private String bookingStatus;
    private LocalDateTime bookingDate;
}
