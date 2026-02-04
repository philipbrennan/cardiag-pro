# CarDiag Pro - Professional OBD2 Diagnostic App

## Project Overview

CarDiag Pro is a professional OBD2 diagnostic Android application that provides manufacturer-specific code databases for BMW, VW, and Nissan vehicles. The app enables users to read and clear diagnostic trouble codes (DTCs), monitor real-time sensor data, and generate professional diagnostic reports.

## Technologies & Skills Demonstrated

### Mobile Development
- **Android Development**: Native Android app development targeting API 24-34
- **Kotlin**: Modern, type-safe programming with coroutines and flows
- **MVVM Architecture**: Clean separation of concerns with ViewModel and Repository patterns

### System Integration
- **Bluetooth Communication**: ELM327 protocol implementation for OBD2 adapters
- **USB Communication**: FT232 USB serial adapter support
- **Hardware Protocols**: OBD-II/ELM327 command set implementation

### Data Management
- **Room Database**: Local SQLite database with 1500+ diagnostic trouble codes
- **Coroutines & Flow**: Asynchronous data streams and reactive programming
- **Data Persistence**: Diagnostic history with freeze frame storage

### Dependency Injection
- **Dagger Hilt**: Modern dependency injection framework for Android
- **Modular Architecture**: Scalable and testable code organization

### Testing & Quality
- **JUnit 5**: Unit testing framework
- **MockK**: Mocking library for Kotlin
- **Espresso**: UI testing for Android
- **Test Coverage**: Target of 80%+ code coverage

### UI/UX
- **Material Design 3**: Modern Android design guidelines
- **ViewBinding**: Type-safe view access
- **Dark Mode Support**: Theme-aware UI components
- **Responsive Layouts**: Adaptive UI for different screen sizes

### Build System
- **Gradle with Kotlin DSL**: Modern build configuration
- **Multi-module Setup**: Scalable project structure
- **Continuous Integration**: Automated builds and tests

## Key Features

### Diagnostic Capabilities
- Multi-ECU support (Engine, Transmission, ABS, SRS)
- Real-time sensor monitoring (15+ PIDs)
- Freeze frame data capture and analysis
- VIN reading with automatic manufacturer detection
- Diagnostic trouble code (DTC) reading and clearing

### Professional Features
- Manufacturer-specific code databases (BMW, VW, Nissan)
- Diagnostic history tracking
- PDF report generation (in progress)
- Live data dashboard with engine performance metrics

### Connectivity
- Bluetooth Classic (ELM327)
- USB Serial (FT232)
- BLE support (planned)

## Development Practices

- **Iteration-based Development**: Structured approach with clear milestones
- **Version Control**: Git with feature branching
- **Documentation**: Comprehensive project documentation in `.claude/` directory
- **Code Quality**: Linting, testing, and code review processes
- **Agile Methodology**: Phased development with regular iterations

## Project Links

- **Repository**: https://github.com/philipbrennan/cardiag-pro
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Build Tool**: Gradle 8.4
- **Language**: Kotlin

## Current Status

**Phase**: Phase 2 - Multi-ECU Support & Enhanced Features
**Iteration**: 6 - Real-time Sensor Monitoring (Completed)
**Version**: v1.5 (in development)

## Skills Showcased

- Mobile application architecture and design
- Hardware communication protocols
- Database design and optimization
- Asynchronous programming
- Modern Android development practices
- Testing and quality assurance
- Project management and documentation
- Problem-solving in automotive diagnostics domain

---

*This project demonstrates professional-level Android development skills with a focus on system integration, data management, and user experience.*
