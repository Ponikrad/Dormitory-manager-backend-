# 🏢 Dormitory Management System - Backend API

## About The Project
This repository contains the backend service for a comprehensive Dormitory Management System, developed as part of an engineering thesis. The goal of the project is to digitize and optimize the daily operations of student housing. 

It provides a secure, robust **RESTful API** that serves as the core logic engine for two client applications: a React.js web panel (for administration) and a native Android mobile app (for students).

## Built With
The application is built using a modern Java ecosystem:
* **Java** * **Spring Boot** (Core framework)
* **Spring Security** (JWT, BCrypt, Role-Based Access Control)
* **Spring Data JPA & Hibernate** (ORM)
* **PostgreSQL** (Relational Database)
* **Maven** (Dependency management & build)

## Key Features
* **🔐 Authentication & Authorization:** Stateless security architecture using JSON Web Tokens (JWT). Includes Role-Based Access Control (RBAC) supporting different user levels: `STUDENT`, `ADMIN`, and `RECEPTIONIST`.
* **💳 Payments & Billing Module:** Tracks financial transactions, calculates fees, and generates downloadable PDF payment receipts.
* **🛠️ Issue Tracking:** A ticketing system that allows students to report technical faults (e.g., plumbing, electrical) and enables staff to manage resolution statuses.
* **📅 Resource Reservation:** A booking system for common areas (e.g., laundry rooms, study rooms) preventing time-slot conflicts.
* **🔑 Key Management:** Digital tracking of physical dormitory keys (issuance, returns, and lost key reporting).
* **📢 Communication:** Internal messaging system and an announcement board for the administration to notify residents.

## Architecture
The source code is organized using a classic **N-Tier (Layered) Architecture** to ensure clean code separation, maintainability, and testability:
* `Controller Layer` (`@RestController`): Entry points for HTTP requests, handling payload validation and routing.
* `Service Layer` (`@Service`): Contains the core business logic and ensures ACID compliance using `@Transactional`.
* `Repository Layer` (`@Repository`): Interfaces extending `JpaRepository` for seamless PostgreSQL database communication using custom `@Query` methods and Lazy Loading optimizations.
* `Entity Layer` (`@Entity`): Domain models mapped to a highly normalized (3NF) relational database schema.
