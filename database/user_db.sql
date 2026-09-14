-- ============================================================
-- user_db.sql
-- Flight Management System - User Service Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS user_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE user_db;

CREATE TABLE IF NOT EXISTS users (
    user_id       BIGINT          NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100)    NOT NULL,
    email         VARCHAR(150)    NOT NULL,
    password_hash VARCHAR(255)    NOT NULL,
    role          VARCHAR(20)     NOT NULL,
    created_date  DATETIME        NOT NULL,

    CONSTRAINT pk_users         PRIMARY KEY (user_id),
    CONSTRAINT uq_users_email   UNIQUE      (email),
    CONSTRAINT chk_users_role   CHECK       (role IN ('ADMIN', 'CUSTOMER'))
);
