package com.flightmanagement.bookingservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingRequestDto {

    @NotNull
    private Long flightId;

    @NotNull
    @Min(1)
    private Integer numberOfSeats;
}
