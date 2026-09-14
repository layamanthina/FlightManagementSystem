-- ============================================================
-- booking_db.sql
-- Flight Management System - Booking Service Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS booking_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE booking_db;

CREATE TABLE IF NOT EXISTS bookings (
    booking_id      BIGINT      NOT NULL AUTO_INCREMENT,
    flight_id       BIGINT      NOT NULL,
    user_id         BIGINT      NOT NULL,
    number_of_seats INT         NOT NULL,
    booking_status  VARCHAR(20) NOT NULL,
    booking_date    DATETIME    NOT NULL,

    CONSTRAINT pk_bookings              PRIMARY KEY (booking_id),
    CONSTRAINT chk_bookings_status      CHECK       (booking_status IN ('CONFIRMED', 'CANCELLED')),
    CONSTRAINT chk_bookings_seats       CHECK       (number_of_seats > 0)
);

CREATE INDEX idx_bookings_user_id   ON bookings (user_id);
CREATE INDEX idx_bookings_flight_id ON bookings (flight_id);
