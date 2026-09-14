package com.flightmanagement.bookingservice.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flightmanagement.bookingservice.client.FlightClient;
import com.flightmanagement.bookingservice.client.UserClient;
import com.flightmanagement.bookingservice.dto.BookingRequestDto;
import com.flightmanagement.bookingservice.dto.BookingResponseDto;
import com.flightmanagement.bookingservice.dto.FlightResponseDto;
import com.flightmanagement.bookingservice.dto.UserResponseDto;
import com.flightmanagement.bookingservice.entity.Booking;
import com.flightmanagement.bookingservice.enums.BookingStatus;
import com.flightmanagement.bookingservice.exception.BookingNotAllowedException;
import com.flightmanagement.bookingservice.exception.BookingNotFoundException;
import com.flightmanagement.bookingservice.exception.UnauthorizedException;
import com.flightmanagement.bookingservice.repository.BookingRepository;
import com.flightmanagement.bookingservice.service.BookingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final FlightClient flightClient;
    private final UserClient userClient;

    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto request, String userEmail) {
        UserResponseDto user = userClient.getUserByEmail(userEmail);
        FlightResponseDto flight = flightClient.getFlightById(request.getFlightId());

        if (request.getNumberOfSeats() <= 0) {
            throw new BookingNotAllowedException("Number of seats must be greater than zero");
        }
        if (!"SCHEDULED".equals(flight.getStatus())) {
            log.warn("Booking rejected: flightId={} has status={}", flight.getFlightId(), flight.getStatus());
            throw new BookingNotAllowedException("Cannot book a flight with status: " + flight.getStatus());
        }
        if (flight.getDepartureTime().isBefore(LocalDateTime.now())) {
            log.warn("Booking rejected: flightId={} departure time has passed", flight.getFlightId());
            throw new BookingNotAllowedException("Cannot book a flight whose departure time has passed");
        }
        if (request.getNumberOfSeats() > flight.getAvailableSeats()) {
            log.warn("Booking rejected: flightId={} requested={} available={}",
                    flight.getFlightId(), request.getNumberOfSeats(), flight.getAvailableSeats());
            throw new BookingNotAllowedException("Requested seats exceed available seats: " + flight.getAvailableSeats());
        }

        flightClient.decreaseSeats(flight.getFlightId(), request.getNumberOfSeats());

        Booking booking = new Booking();
        booking.setFlightId(flight.getFlightId());
        booking.setUserId(user.getUserId());
        booking.setNumberOfSeats(request.getNumberOfSeats());
        booking.setBookingStatus(BookingStatus.CONFIRMED);

        Booking saved = bookingRepository.save(booking);
        log.info("Booking created: bookingId={}, flightId={}, userId={}", saved.getBookingId(), saved.getFlightId(), saved.getUserId());

        return mapToDto(saved, flight.getFlightNumber(), user.getName());
    }

    @Override
    public BookingResponseDto getBookingById(Long id, String userEmail) {
        log.info("Fetching booking: bookingId={}", id);
        Booking booking = findOrThrow(id);
        UserResponseDto user = userClient.getUserByEmail(userEmail);
        if (!booking.getUserId().equals(user.getUserId())) {
            log.warn("Unauthorized access: bookingId={} requestedBy={}", id, userEmail);
            throw new UnauthorizedException("Access denied to booking: " + id);
        }
        FlightResponseDto flight = flightClient.getFlightById(booking.getFlightId());
        return mapToDto(booking, flight.getFlightNumber(), user.getName());
    }

    @Override
    public List<BookingResponseDto> getMyBookings(String userEmail) {
        log.info("Fetching bookings for user={}", userEmail);
        UserResponseDto user = userClient.getUserByEmail(userEmail);
        return bookingRepository.findByUserId(user.getUserId()).stream()
                .map(b -> {
                    FlightResponseDto flight = flightClient.getFlightById(b.getFlightId());
                    return mapToDto(b, flight.getFlightNumber(), user.getName());
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelBooking(Long id, String userEmail) {
        Booking booking = findOrThrow(id);
        UserResponseDto user = userClient.getUserByEmail(userEmail);
        if (!booking.getUserId().equals(user.getUserId())) {
            log.warn("Unauthorized access: bookingId={} requestedBy={}", id, userEmail);
            throw new UnauthorizedException("Access denied to booking: " + id);
        }
        if (BookingStatus.CANCELLED.equals(booking.getBookingStatus())) {
            log.warn("Cancel rejected: bookingId={} already cancelled", id);
            throw new BookingNotAllowedException("Booking is already cancelled");
        }
        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        flightClient.increaseSeats(booking.getFlightId(), booking.getNumberOfSeats());
        log.info("Booking cancelled: bookingId={}, seatsRestored={}", id, booking.getNumberOfSeats());
    }

    private Booking findOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + id));
    }

    private BookingResponseDto mapToDto(Booking booking, String flightNumber, String userName) {
        return new BookingResponseDto(
                booking.getBookingId(),
                booking.getFlightId(),
                flightNumber,
                booking.getUserId(),
                userName,
                booking.getNumberOfSeats(),
                booking.getBookingStatus().name(),
                booking.getBookingDate());
    }
}
