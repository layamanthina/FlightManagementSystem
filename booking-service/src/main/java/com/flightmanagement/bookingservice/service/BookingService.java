package com.flightmanagement.bookingservice.service;

import java.util.List;

import com.flightmanagement.bookingservice.dto.BookingRequestDto;
import com.flightmanagement.bookingservice.dto.BookingResponseDto;

public interface BookingService {

    BookingResponseDto createBooking(BookingRequestDto request, String userEmail);

    BookingResponseDto getBookingById(Long id, String userEmail);

    List<BookingResponseDto> getMyBookings(String userEmail);

    void cancelBooking(Long id, String userEmail);
}
