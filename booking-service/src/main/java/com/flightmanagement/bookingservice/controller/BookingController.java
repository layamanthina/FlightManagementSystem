package com.flightmanagement.bookingservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flightmanagement.bookingservice.dto.BookingRequestDto;
import com.flightmanagement.bookingservice.dto.BookingResponseDto;
import com.flightmanagement.bookingservice.service.BookingService;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponseDto> createBooking(
            @Valid @RequestBody BookingRequestDto request,
            @Parameter(hidden = true) @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(request, userEmail));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDto> getBookingById(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(bookingService.getBookingById(id, userEmail));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookingResponseDto>> getMyBookings(
            @Parameter(hidden = true) @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(bookingService.getMyBookings(userEmail));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader("X-User-Email") String userEmail) {
        bookingService.cancelBooking(id, userEmail);
        return ResponseEntity.noContent().build();
    }
}
