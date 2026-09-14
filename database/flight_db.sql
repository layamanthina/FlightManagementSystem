-- ============================================================
-- flight_db.sql
-- Flight Management System - Flight Service Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS flight_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE flight_db;

CREATE TABLE IF NOT EXISTS flights (
    flight_id       BIGINT          NOT NULL AUTO_INCREMENT,
    flight_number   VARCHAR(20)     NOT NULL,
    airline         VARCHAR(100)    NOT NULL,
    source          VARCHAR(100)    NOT NULL,
    destination     VARCHAR(100)    NOT NULL,
    departure_time  DATETIME        NOT NULL,
    arrival_time    DATETIME        NOT NULL,
    total_seats     INT             NOT NULL,
    available_seats INT             NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    created_date    DATETIME        NOT NULL,
    updated_date    DATETIME,
    version         BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_flights               PRIMARY KEY (flight_id),
    CONSTRAINT uq_flights_number_dep    UNIQUE      (flight_number, departure_time),
    CONSTRAINT chk_flights_status       CHECK       (status IN ('SCHEDULED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT chk_flights_seats        CHECK       (available_seats >= 0 AND available_seats <= total_seats),
    CONSTRAINT chk_flights_total_seats  CHECK       (total_seats > 0),
    CONSTRAINT chk_flights_src_dest     CHECK       (source <> destination)
);
