-- server/migrations/001_create_tables.sql
CREATE DATABASE IF NOT EXISTS hr_system
  DEFAULT CHARACTER SET = utf8mb4
  DEFAULT COLLATE = utf8mb4_unicode_ci;
USE hr_system;

-- Users
CREATE TABLE IF NOT EXISTS `User` (
  user_id         INT          NOT NULL AUTO_INCREMENT,
  username        VARCHAR(100) NOT NULL,
  password_hash   VARCHAR(255) NOT NULL,
  email           VARCHAR(255),
  full_name       VARCHAR(255),
  user_type       ENUM('ADMIN','HR','MANAGER','EMPLOYEE') NOT NULL DEFAULT 'EMPLOYEE',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login      DATETIME     NULL,
  PRIMARY KEY (user_id),
  UNIQUE KEY ux_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Minimal Employee table to map a user to an employee (optional)
CREATE TABLE IF NOT EXISTS `Employee` (
  employee_id    INT NOT NULL AUTO_INCREMENT,
  user_id        INT NULL,
  employee_code  VARCHAR(50) UNIQUE,
  manager_id     INT NULL,
  PRIMARY KEY (employee_id),
  CONSTRAINT fk_employee_user FOREIGN KEY (user_id) REFERENCES `User` (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
