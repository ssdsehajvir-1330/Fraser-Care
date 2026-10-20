# FraserCare: Pharmacy Service Management Platform

**CMPT 276 Project Proposal**  
**Fall 2026**  
**Simon Fraser University**  
**Introduction to Software Engineering (CMPT 276)**

## Group Members

- Sehajvir Dhillon
- Alfonse Teruel
- Hassaan Nasim
- Enoch Liu
- Mohammed Ananda Anwar

## Project Abstract

FraserCare is a browser-based pharmacy service management application designed to simplify how customers and pharmacy staff manage everyday pharmacy services. The platform will allow customers to create accounts, browse available services, book appointments, join waitlists, and track simulated pickup requests. Pharmacy staff will have access to a separate dashboard where they can manage appointments, waitlists, and customer requests. The application will also integrate Health Canada's Drug Product Database API, allowing users to search for publicly available information about Canadian drug products. The goal is to develop a convenient, centralized platform that improves communication, organization, and accessibility for both customers and pharmacy staff.

## Problem and Existing Systems

Customers often need to contact pharmacies directly to ask about available services, appointment times, or the status of requests. Some pharmacies offer online booking or informational websites, but these features are not always available together in one system. This can create inconvenience for customers and additional administrative work for staff.

FraserCare aims to address these problems by bringing several common pharmacy-related tasks into one application. Instead of relying entirely on phone calls or separate services, customers will be able to view appointment availability, submit requests, and check updates online. Pharmacy staff will be able to manage the same information through a centralized dashboard.

## Target Audience and Benefits

The main target audience is pharmacy customers and pharmacy staff. Customers will benefit from a more convenient way to access pharmacy service information, schedule appointments, and follow the progress of their requests. Pharmacy staff will benefit from better organization of appointment schedules, waitlists, and customer requests.

The application is intended to provide a practical service rather than entertainment or medical advice. For the purposes of this university project, all bookings and pickup requests will be simulated, and the system will not store real patient health records, process prescriptions, or make medical decisions.

## Project Scope and Major Features

The project will consist of five major feature areas, allowing each group member to take responsibility for a substantial part of the application.

### 1. User Authentication and Account Management

The first feature will be user authentication and account management, allowing customers and pharmacy staff to create accounts, log in securely, and access features based on their assigned roles.

### 2. Appointment Booking System

The second feature will be an appointment booking system, where customers can browse pharmacy services, view available appointment slots, make bookings, and cancel appointments. The system will prevent conflicting reservations.

### 3. Waitlist Management System

The third feature will be a waitlist management system, allowing customers to join a waitlist when appointment slots are unavailable. Pharmacy staff will be able to manage waitlist entries and make newly available slots visible to waiting customers.

### 4. Pharmacy Staff Dashboard and Pickup Request Tracker

The fourth feature will be a pharmacy staff dashboard and pickup request tracker. Staff will be able to view and manage appointments, monitor simulated customer requests, and update request statuses. Customers will be able to check these updates through their accounts.

### 5. Customer Self-Check and Drug-Product Information Search

The fifth feature will be a customer self-check and drug-product information search tool. Customers will be able to complete a simple, non-diagnostic questionnaire that directs them toward relevant pharmacy services. The application will also integrate Health Canada's Drug Product Database REST API, allowing users to search for publicly available drug-product information, including brand names and Drug Identification Numbers. This feature will demonstrate how the application retrieves and displays information from an external web API.

## Proposed Technologies and Development Plan

The application will be developed using Java and Spring Boot for backend functionality, HTML, CSS, and JavaScript for the browser-based interface, and PostgreSQL for storing application data. GitHub will be used for collaboration and version control, and the application will be deployed to Render.

Development will take place over multiple iterations. The first iteration will establish the application structure, deployment, and login functionality. Subsequent iterations will expand the appointment, waitlist, and staff management features. By Iteration 3, the application will integrate the external REST API for drug-product information.

By the end of the project, the group aims to deliver a functional demonstration of a pharmacy management platform that combines multiple useful features into one accessible website.

## Group Members' Backgrounds and Experience

Our group consists of five students with different levels of experience in software development, programming, and collaborative projects. We plan to use each member's strengths while giving everyone opportunities to develop new technical skills.

**Alfonse Teruel:** Has experience with Java, C, C++, RISC-V assembly, VHDL, object-oriented programming, Git, GitHub, databases, and software testing through university coursework and previous work-related software engineering projects.

**Mohammed Ananda Anwar:** Has experience with Java, C, C++, React, Python, and R. His web development experience comes from previous coursework and web development assignments. He has limited experience with databases.

**Sehajvir Dhillon:** Has experience with Java, C, C++, RISC-V assembly, VHDL, object-oriented programming, Git, GitHub, databases, and software testing through university coursework and programming assignments. He is interested in gaining more practical experience with Spring Boot and collaborative software development.

**Hassaan Nasim:** Has experience with Java, C, C++, RISC-V assembly, VHDL, object-oriented programming, Git, GitHub, databases, and software testing through university coursework and programming assignments. He is interested in further developing his skills in full-stack web application development.

**Enoch Liu:** Has experience with Java, C, C++, RISC-V assembly, VHDL, object-oriented programming, Git, GitHub, databases, and software testing through university coursework and programming assignments. He is interested in expanding his experience with web development and database integration.

## Individual Roles and Responsibilities

To ensure that each group member has a substantial contribution, we propose dividing FraserCare into five major features.

### 1. User Authentication and Account Management
**Assigned Member: Mohammed Ananda Anwar**

Responsible for implementing customer and pharmacy staff accounts, login functionality, session management, and role-based access to different features.

### 2. Appointment Booking System
**Assigned Member: Alfonse Teruel**

Responsible for allowing customers to browse pharmacy services, view available appointment slots, create bookings, and cancel appointments. This feature will also prevent conflicting reservations.

### 3. Waitlist Management
**Assigned Member: Enoch Liu**

Responsible for implementing appointment waitlists, allowing customers to join when slots are unavailable and helping staff manage waitlist entries when appointments become available.

### 4. Pharmacy Staff Dashboard and Pickup Tracking
**Assigned Member: Sehajvir Dhillon**

Responsible for creating a staff interface to manage appointments, monitor waitlists, and update the statuses of simulated pickup requests.

### 5. Customer Self-Check and External API Integration
**Assigned Member: Hassaan Nasim**

Responsible for developing a simple, non-diagnostic self-check questionnaire that directs customers toward relevant pharmacy services. This member will also lead integration of the proposed Health Canada Drug Product Database REST API.

Although responsibilities will be divided into these five areas, all members will participate in testing, debugging, integration, documentation, and reviewing contributions.

## First Group Meeting

**Date:** October 16, 2026  
**Location/Platform:** Discord  
**Attendees:** Sehajvir Dhillon, Alfonse Teruel, Hassaan Nasim, Enoch Liu, Mohammed Ananda Anwar

### Meeting Discussion

- Discussed various project ideas as a group, considering their feasibility, complexity, and how well they would meet the course requirements. After comparing different options, we decided to develop FraserCare, a pharmacy service management platform inspired by real-world pharmacy workflows.
- Discussed each group member's programming background, previous experience, and areas of interest.
- Reviewed the CMPT 276 project requirements, including Java, Spring Boot, PostgreSQL, Git, Render, user authentication, and external REST API integration.
- Discussed the main features of FraserCare, including customer and staff accounts, appointment booking, waitlist management, a pharmacy staff dashboard, and a customer self-check feature.
- Considered how the five major features could be divided among group members to ensure everyone has a substantial contribution.
- Discussed GitHub collaboration, deployment, development priorities, and communication methods.

### Decisions Made

- **Project Selection:** Chose FraserCare, a browser-based pharmacy service management platform designed to simplify appointment scheduling, waitlist management, and communication between customers and pharmacy staff.
- **Technology Stack:** Agreed to use Java and Spring Boot for backend development, PostgreSQL for database management, HTML/CSS/JavaScript for the frontend, GitHub for version control, and Render for deployment.
- **Major Features:** Identified five main features: user authentication and account management, appointment booking, waitlist management, pharmacy staff dashboard and pickup tracking, and customer self-check with external REST API integration.
- **Responsibilities:** Agreed to divide the five major features among group members, with each member responsible for a substantial portion of the application while collaborating on integration and testing.
- **Development Approach:** Decided to develop the application incrementally, beginning with the initial Spring Boot setup, GitHub repository, Render deployment, and basic authentication before expanding the remaining features.
- **Collaboration:** Agreed to use GitHub to track individual contributions and a group communication platform to coordinate tasks, share progress, and discuss any issues during development.

## Next Meeting and Initial Tasks

**Next Meeting:** October 23, 2026

Before the next meeting, group members will work toward the following objectives:

- Research the requirements and implementation approach for their assigned features.
- Begin planning the database structure for customer accounts, appointments, and waitlists.
- Create initial designs for the customer and pharmacy staff interfaces.
- Evaluate possible external REST APIs and determine which would be most suitable for FraserCare.
- Familiarize themselves with the project's Spring Boot structure and GitHub workflow.

During the next meeting, we will review everyone's progress, discuss technical challenges, finalize our development priorities, and establish tasks for the first iteration.

We plan to meet approximately once or twice per week through Discord to coordinate development, review progress, and address any issues.

