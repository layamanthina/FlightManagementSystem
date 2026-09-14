package com.flightmanagement.flightservice.service;

import java.time.LocalDate;
import java.util.List;

import com.flightmanagement.flightservice.dto.FlightRequestDto;
import com.flightmanagement.flightservice.dto.FlightResponseDto;

public interface FlightService {

    FlightResponseDto createFlight(FlightRequestDto request);

    FlightResponseDto updateFlight(Long id, FlightRequestDto request);

    FlightResponseDto getFlightById(Long id);

    List<FlightResponseDto> getAllFlights();

    List<FlightResponseDto> searchFlights(String source, String destination, LocalDate date);

    void cancelFlight(Long id);

    void decreaseSeats(Long id, Integer seats);

    void increaseSeats(Long id, Integer seats);
}