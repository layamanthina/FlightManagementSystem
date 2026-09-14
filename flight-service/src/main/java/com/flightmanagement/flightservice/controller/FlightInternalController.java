package com.flightmanagement.flightservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.flightmanagement.flightservice.service.FlightService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/flights/internal")
@RequiredArgsConstructor
public class FlightInternalController {

    private final FlightService flightService;

    @PutMapping("/{id}/seats/decrease")
    public ResponseEntity<Void> decreaseSeats(@PathVariable Long id, @RequestParam Integer seats) {
        flightService.decreaseSeats(id, seats);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/seats/increase")
    public ResponseEntity<Void> increaseSeats(@PathVariable Long id, @RequestParam Integer seats) {
        flightService.increaseSeats(id, seats);
        return ResponseEntity.ok().build();
    }
}
