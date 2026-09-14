package com.flightmanagement.flightservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.flightmanagement.flightservice.dto.FlightRequestDto;
import com.flightmanagement.flightservice.dto.FlightResponseDto;
import com.flightmanagement.flightservice.entity.Flight;
import com.flightmanagement.flightservice.enums.FlightStatus;
import com.flightmanagement.flightservice.exception.FlightAlreadyExistsException;
import com.flightmanagement.flightservice.exception.FlightNotFoundException;
import com.flightmanagement.flightservice.exception.InvalidFlightException;
import com.flightmanagement.flightservice.repository.FlightRepository;
import com.flightmanagement.flightservice.service.impl.FlightServiceImpl;

@ExtendWith(MockitoExtension.class)
class FlightServiceImplTest {

    @Mock
    private FlightRepository flightRepository;

    @InjectMocks
    private FlightServiceImpl flightService;

    private FlightRequestDto validRequest;
    private Flight savedFlight;

    @BeforeEach
    void setUp() {
        validRequest = new FlightRequestDto();
        validRequest.setFlightNumber("AI101");
        validRequest.setAirline("Air India");
        validRequest.setSource("Mumbai");
        validRequest.setDestination("Delhi");
        validRequest.setDepartureTime(LocalDateTime.now().plusDays(1));
        validRequest.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(2));
        validRequest.setTotalSeats(180);

        savedFlight = new Flight();
        savedFlight.setFlightId(1L);
        savedFlight.setFlightNumber("AI101");
        savedFlight.setAirline("Air India");
        savedFlight.setSource("Mumbai");
        savedFlight.setDestination("Delhi");
        savedFlight.setDepartureTime(validRequest.getDepartureTime());
        savedFlight.setArrivalTime(validRequest.getArrivalTime());
        savedFlight.setTotalSeats(180);
        savedFlight.setAvailableSeats(180);
        savedFlight.setStatus(FlightStatus.SCHEDULED);
    }

    @Test
    void createFlight_success() {
        when(flightRepository.existsByFlightNumberAndDepartureTime(any(), any())).thenReturn(false);
        when(flightRepository.save(any())).thenReturn(savedFlight);

        FlightResponseDto result = flightService.createFlight(validRequest);

        assertEquals("AI101", result.getFlightNumber());
        assertEquals(180, result.getAvailableSeats());
        assertEquals("SCHEDULED", result.getStatus());
    }

    @Test
    void createFlight_duplicateFlightNumber_throwsConflict() {
        when(flightRepository.existsByFlightNumberAndDepartureTime(any(), any())).thenReturn(true);

        assertThrows(FlightAlreadyExistsException.class,
                () -> flightService.createFlight(validRequest));
    }

    @Test
    void createFlight_sameSourceDestination_throwsInvalid() {
        validRequest.setDestination("Mumbai");

        assertThrows(InvalidFlightException.class,
                () -> flightService.createFlight(validRequest));
    }

    @Test
    void createFlight_arrivalBeforeDeparture_throwsInvalid() {
        validRequest.setArrivalTime(validRequest.getDepartureTime().minusHours(1));

        assertThrows(InvalidFlightException.class,
                () -> flightService.createFlight(validRequest));
    }

    @Test
    void cancelFlight_success() {
        when(flightRepository.findById(1L)).thenReturn(Optional.of(savedFlight));
        when(flightRepository.save(any())).thenReturn(savedFlight);

        flightService.cancelFlight(1L);

        assertEquals(FlightStatus.CANCELLED, savedFlight.getStatus());
        verify(flightRepository).save(savedFlight);
    }

    @Test
    void cancelFlight_alreadyCancelled_throwsInvalid() {
        savedFlight.setStatus(FlightStatus.CANCELLED);
        when(flightRepository.findById(1L)).thenReturn(Optional.of(savedFlight));

        assertThrows(InvalidFlightException.class,
                () -> flightService.cancelFlight(1L));
    }

    @Test
    void cancelFlight_notFound_throwsException() {
        when(flightRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(FlightNotFoundException.class,
                () -> flightService.cancelFlight(99L));
    }

    @Test
    void updateFlight_totalSeatsBelowBooked_throwsInvalid() {
        savedFlight.setTotalSeats(180);
        savedFlight.setAvailableSeats(150); // 30 seats already booked
        validRequest.setTotalSeats(20);     // less than 30 booked
        when(flightRepository.findById(1L)).thenReturn(Optional.of(savedFlight));

        assertThrows(InvalidFlightException.class,
                () -> flightService.updateFlight(1L, validRequest));
    }

    @Test
    void updateFlight_success_adjustsAvailableSeats() {
        savedFlight.setTotalSeats(180);
        savedFlight.setAvailableSeats(150); // 30 booked
        validRequest.setTotalSeats(200);    // increase by 20
        when(flightRepository.findById(1L)).thenReturn(Optional.of(savedFlight));
        when(flightRepository.save(any())).thenReturn(savedFlight);

        flightService.updateFlight(1L, validRequest);

        assertEquals(170, savedFlight.getAvailableSeats()); // 150 + 20
    }

    @Test
    void decreaseSeats_zeroSeats_throwsInvalid() {
        assertThrows(InvalidFlightException.class,
                () -> flightService.decreaseSeats(1L, 0));
    }

    @Test
    void increaseSeats_zeroSeats_throwsInvalid() {
        assertThrows(InvalidFlightException.class,
                () -> flightService.increaseSeats(1L, 0));
    }

    @Test
    void increaseSeats_exceedsTotalSeats_throwsInvalid() {
        savedFlight.setTotalSeats(180);
        savedFlight.setAvailableSeats(175);
        when(flightRepository.findByIdWithLock(1L)).thenReturn(Optional.of(savedFlight));

        assertThrows(InvalidFlightException.class,
                () -> flightService.increaseSeats(1L, 10)); // 175 + 10 = 185 > 180
    }

    @Test
    void decreaseSeats_insufficientSeats_throwsInvalid() {
        savedFlight.setAvailableSeats(2);
        when(flightRepository.findByIdWithLock(1L)).thenReturn(Optional.of(savedFlight));

        assertThrows(InvalidFlightException.class,
                () -> flightService.decreaseSeats(1L, 5));
    }

    @Test
    void decreaseSeats_success() {
        savedFlight.setAvailableSeats(10);
        when(flightRepository.findByIdWithLock(1L)).thenReturn(Optional.of(savedFlight));
        when(flightRepository.save(any())).thenReturn(savedFlight);

        flightService.decreaseSeats(1L, 3);

        assertEquals(7, savedFlight.getAvailableSeats());
    }

    @Test
    void increaseSeats_success() {
        savedFlight.setAvailableSeats(5);
        when(flightRepository.findByIdWithLock(1L)).thenReturn(Optional.of(savedFlight));
        when(flightRepository.save(any())).thenReturn(savedFlight);

        flightService.increaseSeats(1L, 3);

        assertEquals(8, savedFlight.getAvailableSeats());
    }
}
