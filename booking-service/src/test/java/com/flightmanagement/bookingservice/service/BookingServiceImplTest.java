package com.flightmanagement.bookingservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.flightmanagement.bookingservice.service.impl.BookingServiceImpl;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FlightClient flightClient;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private UserResponseDto user;
    private FlightResponseDto flight;
    private BookingRequestDto bookingRequest;
    private Booking savedBooking;

    @BeforeEach
    void setUp() {
        user = new UserResponseDto();
        user.setUserId(1L);
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setRole("CUSTOMER");

        flight = new FlightResponseDto();
        flight.setFlightId(10L);
        flight.setFlightNumber("AI101");
        flight.setStatus("SCHEDULED");
        flight.setAvailableSeats(50);
        flight.setDepartureTime(LocalDateTime.now().plusDays(1));

        bookingRequest = new BookingRequestDto();
        bookingRequest.setFlightId(10L);
        bookingRequest.setNumberOfSeats(2);

        savedBooking = new Booking();
        savedBooking.setBookingId(100L);
        savedBooking.setFlightId(10L);
        savedBooking.setUserId(1L);
        savedBooking.setNumberOfSeats(2);
        savedBooking.setBookingStatus(BookingStatus.CONFIRMED);
        savedBooking.setBookingDate(LocalDateTime.now());
    }

    @Test
    void createBooking_success() {
        when(userClient.getUserByEmail("john@example.com")).thenReturn(user);
        when(flightClient.getFlightById(10L)).thenReturn(flight);
        when(bookingRepository.save(any())).thenReturn(savedBooking);

        BookingResponseDto result = bookingService.createBooking(bookingRequest, "john@example.com");

        assertEquals(100L, result.getBookingId());
        assertEquals("CONFIRMED", result.getBookingStatus());
        verify(flightClient).decreaseSeats(10L, 2);
    }

    @Test
    void createBooking_cancelledFlight_throwsNotAllowed() {
        flight.setStatus("CANCELLED");
        when(userClient.getUserByEmail("john@example.com")).thenReturn(user);
        when(flightClient.getFlightById(10L)).thenReturn(flight);

        assertThrows(BookingNotAllowedException.class,
                () -> bookingService.createBooking(bookingRequest, "john@example.com"));
    }

    @Test
    void createBooking_completedFlight_throwsNotAllowed() {
        flight.setStatus("COMPLETED");
        when(userClient.getUserByEmail("john@example.com")).thenReturn(user);
        when(flightClient.getFlightById(10L)).thenReturn(flight);

        assertThrows(BookingNotAllowedException.class,
                () -> bookingService.createBooking(bookingRequest, "john@example.com"));
    }

    @Test
    void createBooking_insufficientSeats_throwsNotAllowed() {
        flight.setAvailableSeats(1);
        bookingRequest.setNumberOfSeats(5);
        when(userClient.getUserByEmail("john@example.com")).thenReturn(user);
        when(flightClient.getFlightById(10L)).thenReturn(flight);

        assertThrows(BookingNotAllowedException.class,
                () -> bookingService.createBooking(bookingRequest, "john@example.com"));
    }

    @Test
    void createBooking_departureInPast_throwsNotAllowed() {
        flight.setDepartureTime(LocalDateTime.now().minusHours(1));
        when(userClient.getUserByEmail("john@example.com")).thenReturn(user);
        when(flightClient.getFlightById(10L)).thenReturn(flight);

        assertThrows(BookingNotAllowedException.class,
                () -> bookingService.createBooking(bookingRequest, "john@example.com"));
    }

    @Test
    void cancelBooking_success_seatsRestored() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(savedBooking));
        when(userClient.getUserByEmail("john@example.com")).thenReturn(user);
        when(bookingRepository.save(any())).thenReturn(savedBooking);

        bookingService.cancelBooking(100L, "john@example.com");

        assertEquals(BookingStatus.CANCELLED, savedBooking.getBookingStatus());
        verify(flightClient).increaseSeats(10L, 2);
    }

    @Test
    void cancelBooking_alreadyCancelled_throwsNotAllowed() {
        savedBooking.setBookingStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(savedBooking));
        when(userClient.getUserByEmail("john@example.com")).thenReturn(user);

        assertThrows(BookingNotAllowedException.class,
                () -> bookingService.cancelBooking(100L, "john@example.com"));
    }

    @Test
    void cancelBooking_differentUser_throwsUnauthorized() {
        UserResponseDto otherUser = new UserResponseDto();
        otherUser.setUserId(99L);
        otherUser.setEmail("other@example.com");

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(savedBooking));
        when(userClient.getUserByEmail("other@example.com")).thenReturn(otherUser);

        assertThrows(UnauthorizedException.class,
                () -> bookingService.cancelBooking(100L, "other@example.com"));
    }

    @Test
    void createBooking_zeroSeats_throwsNotAllowed() {
        bookingRequest.setNumberOfSeats(0);
        when(userClient.getUserByEmail("john@example.com")).thenReturn(user);
        when(flightClient.getFlightById(10L)).thenReturn(flight);

        assertThrows(BookingNotAllowedException.class,
                () -> bookingService.createBooking(bookingRequest, "john@example.com"));
    }

    @Test
    void getBookingById_differentUser_throwsUnauthorized() {
        UserResponseDto otherUser = new UserResponseDto();
        otherUser.setUserId(99L);
        otherUser.setEmail("other@example.com");

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(savedBooking));
        when(userClient.getUserByEmail("other@example.com")).thenReturn(otherUser);

        assertThrows(UnauthorizedException.class,
                () -> bookingService.getBookingById(100L, "other@example.com"));
    }

    @Test
    void getBookingById_notFound_throwsException() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class,
                () -> bookingService.getBookingById(999L, "john@example.com"));
    }

    @Test
    void getMyBookings_returnsUserBookings() {
        when(userClient.getUserByEmail("john@example.com")).thenReturn(user);
        when(bookingRepository.findByUserId(1L)).thenReturn(List.of(savedBooking));
        when(flightClient.getFlightById(10L)).thenReturn(flight);

        List<BookingResponseDto> result = bookingService.getMyBookings("john@example.com");

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getBookingId());
    }
}
