package com.flightmanagement.bookingservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatUpdateRequestDto {

    private Integer seats;

    public SeatUpdateRequestDto(Integer seats) {
        this.seats = seats;
    }
}
