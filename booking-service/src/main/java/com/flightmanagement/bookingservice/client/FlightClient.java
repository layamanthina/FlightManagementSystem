package com.flightmanagement.bookingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.flightmanagement.bookingservice.dto.FlightResponseDto;

@FeignClient(name = "flight-service")
public interface FlightClient {

    @GetMapping("/api/flights/{id}")
    FlightResponseDto getFlightById(@PathVariable Long id);

    @PutMapping("/api/flights/internal/{id}/seats/decrease")
    void decreaseSeats(@PathVariable Long id, @RequestParam Integer seats);

    @PutMapping("/api/flights/internal/{id}/seats/increase")
    void increaseSeats(@PathVariable Long id, @RequestParam Integer seats);
}
