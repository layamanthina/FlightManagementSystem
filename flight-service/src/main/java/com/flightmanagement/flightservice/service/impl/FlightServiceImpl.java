package com.flightmanagement.flightservice.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flightmanagement.flightservice.dto.FlightRequestDto;
import com.flightmanagement.flightservice.dto.FlightResponseDto;
import com.flightmanagement.flightservice.entity.Flight;
import com.flightmanagement.flightservice.enums.FlightStatus;
import com.flightmanagement.flightservice.exception.FlightAlreadyExistsException;
import com.flightmanagement.flightservice.exception.FlightNotFoundException;
import com.flightmanagement.flightservice.exception.InvalidFlightException;
import com.flightmanagement.flightservice.repository.FlightRepository;
import com.flightmanagement.flightservice.service.FlightService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FlightServiceImpl implements FlightService {

    private static final Logger log = LoggerFactory.getLogger(FlightServiceImpl.class);

    private final FlightRepository flightRepository;

    @Override
    @Transactional
    public FlightResponseDto createFlight(FlightRequestDto request) {
        validateFlightRequest(request);
        if (flightRepository.existsByFlightNumberAndDepartureTime(
                request.getFlightNumber(), request.getDepartureTime())) {
            log.warn("Flight creation rejected: duplicate flightNumber={} at departureTime={}",
                    request.getFlightNumber(), request.getDepartureTime());
            throw new FlightAlreadyExistsException(
                    "Flight already exists: " + request.getFlightNumber());
        }
        Flight flight = mapToEntity(request);
        Flight saved = flightRepository.save(flight);
        log.info("Flight created: flightId={}, flightNumber={}", saved.getFlightId(), saved.getFlightNumber());
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public FlightResponseDto updateFlight(Long id, FlightRequestDto request) {
        validateFlightRequest(request);
        Flight flight = findOrThrow(id);
        int bookedSeats = flight.getTotalSeats() - flight.getAvailableSeats();
        if (request.getTotalSeats() < bookedSeats) {
            log.warn("Update rejected: flightId={} totalSeats={} already booked={}", id, request.getTotalSeats(), bookedSeats);
            throw new InvalidFlightException("Total seats cannot be less than already booked seats");
        }
        int seatDiff = request.getTotalSeats() - flight.getTotalSeats();
        flight.setFlightNumber(request.getFlightNumber());
        flight.setAirline(request.getAirline());
        flight.setSource(request.getSource());
        flight.setDestination(request.getDestination());
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        flight.setTotalSeats(request.getTotalSeats());
        flight.setAvailableSeats(flight.getAvailableSeats() + seatDiff);
        Flight saved = flightRepository.save(flight);
        log.info("Flight updated: flightId={}", id);
        return mapToDto(saved);
    }

    @Override
    public FlightResponseDto getFlightById(Long id) {
        log.info("Fetching flight: flightId={}", id);
        return mapToDto(findOrThrow(id));
    }

    @Override
    public List<FlightResponseDto> getAllFlights() {
        log.info("Fetching all flights");
        return flightRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<FlightResponseDto> searchFlights(String source, String destination, LocalDate date) {
        log.info("Searching flights: source={}, destination={}, date={}", source, destination, date);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        return flightRepository.searchFlights(source, destination, FlightStatus.SCHEDULED, startOfDay, endOfDay)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelFlight(Long id) {
        Flight flight = findOrThrow(id);
        if (flight.getStatus() == FlightStatus.CANCELLED) {
            log.warn("Cancel rejected: flightId={} already cancelled", id);
            throw new InvalidFlightException("Flight is already cancelled");
        }
        flight.setStatus(FlightStatus.CANCELLED);
        flightRepository.save(flight);
        log.info("Flight cancelled: flightId={}", id);
    }

    @Override
    @Transactional
    public void decreaseSeats(Long id, Integer seats) {
        if (seats <= 0) {
            throw new InvalidFlightException("Seats must be greater than zero");
        }
        Flight flight = flightRepository.findByIdWithLock(id)
                .orElseThrow(() -> new FlightNotFoundException("Flight not found: " + id));
        if (flight.getAvailableSeats() < seats) {
            log.warn("Seat decrease rejected: flightId={} requested={} available={}", id, seats, flight.getAvailableSeats());
            throw new InvalidFlightException("Insufficient available seats");
        }
        flight.setAvailableSeats(flight.getAvailableSeats() - seats);
        flightRepository.save(flight);
        log.info("Seats decreased: flightId={} by={} remaining={}", id, seats, flight.getAvailableSeats());
    }

    @Override
    @Transactional
    public void increaseSeats(Long id, Integer seats) {
        if (seats <= 0) {
            throw new InvalidFlightException("Seats must be greater than zero");
        }
        Flight flight = flightRepository.findByIdWithLock(id)
                .orElseThrow(() -> new FlightNotFoundException("Flight not found: " + id));
        if (flight.getAvailableSeats() + seats > flight.getTotalSeats()) {
            log.warn("Seat increase rejected: flightId={} available={} increase={} total={}",
                    id, flight.getAvailableSeats(), seats, flight.getTotalSeats());
            throw new InvalidFlightException("Available seats cannot exceed total seats");
        }
        flight.setAvailableSeats(flight.getAvailableSeats() + seats);
        flightRepository.save(flight);
        log.info("Seats restored: flightId={} by={} total={}", id, seats, flight.getAvailableSeats());
    }

    private void validateFlightRequest(FlightRequestDto request) {
        if (request.getSource().equalsIgnoreCase(request.getDestination())) {
            throw new InvalidFlightException("Source and destination cannot be the same");
        }
        if (!request.getDepartureTime().isBefore(request.getArrivalTime())) {
            throw new InvalidFlightException("Departure time must be before arrival time");
        }
    }

    private Flight findOrThrow(Long id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new FlightNotFoundException("Flight not found: " + id));
    }

    private Flight mapToEntity(FlightRequestDto request) {
        Flight flight = new Flight();
        flight.setFlightNumber(request.getFlightNumber());
        flight.setAirline(request.getAirline());
        flight.setSource(request.getSource());
        flight.setDestination(request.getDestination());
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        flight.setTotalSeats(request.getTotalSeats());
        flight.setAvailableSeats(request.getTotalSeats());
        flight.setStatus(FlightStatus.SCHEDULED);
        return flight;
    }

    private FlightResponseDto mapToDto(Flight flight) {
        return new FlightResponseDto(
                flight.getFlightId(),
                flight.getFlightNumber(),
                flight.getAirline(),
                flight.getSource(),
                flight.getDestination(),
                flight.getDepartureTime(),
                flight.getArrivalTime(),
                flight.getTotalSeats(),
                flight.getAvailableSeats(),
                flight.getStatus().name());
    }
}