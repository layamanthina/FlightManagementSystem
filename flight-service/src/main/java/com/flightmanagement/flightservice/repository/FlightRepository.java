package com.flightmanagement.flightservice.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flightmanagement.flightservice.entity.Flight;
import com.flightmanagement.flightservice.enums.FlightStatus;

import jakarta.persistence.LockModeType;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    boolean existsByFlightNumberAndDepartureTime(String flightNumber, LocalDateTime departureTime);

    @Query("SELECT f FROM Flight f WHERE f.source = :source AND f.destination = :destination AND f.status = :status AND f.departureTime >= :startOfDay AND f.departureTime < :endOfDay")
    List<Flight> searchFlights(@Param("source") String source,
                               @Param("destination") String destination,
                               @Param("status") FlightStatus status,
                               @Param("startOfDay") LocalDateTime startOfDay,
                               @Param("endOfDay") LocalDateTime endOfDay);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Flight f WHERE f.flightId = :id")
    java.util.Optional<Flight> findByIdWithLock(@Param("id") Long id);
}