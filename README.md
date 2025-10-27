# CarDiag Pro

Professional OBD2 diagnostic Android app with manufacturer-specific code databases for BMW, VW, and Nissan.

## Project Status

**Current Phase**: Phase 1 - Foundation & Basic Engine Diagnostics
**Current Iteration**: Iteration 1 - Project Foundation & Architecture (In Progress)

## Features

### Unique Selling Points
1. **Manufacturer-Specific Codes**: Comprehensive P1xxx code database for BMW, VW, and Nissan
2. **Multi-ECU Support**: Diagnostics for Engine, Transmission, ABS, and SRS systems
3. **Professional Features**: Freeze frame data, diagnostic history, PDF reports
4. **Multiple Adapter Support**: ELM327 Bluetooth, USB (FT232), and BLE

### Planned Features (by Phase)

**Phase 1 (MVP - v1.0)**:
- ✅ Bluetooth adapter connection (ELM327)
- ✅ USB adapter connection (FT232)
- Read and clear engine DTCs (P0xxx + P1xxx)
- Auto-detect manufacturer via VIN
- Local database with 1000+ DTCs

**Phase 2 (v1.5)**:
- Multi-ECU support (Engine, Transmission, ABS, SRS)
- Freeze frame data capture
- Diagnostic history
- PDF report export

**Phase 3 (v2.0)**:
- Real-time sensor monitoring
- Advanced features

## Technology Stack

- **Language**: Kotlin
- **Architecture**: MVVM + Repository Pattern
- **Build System**: Gradle with Kotlin DSL
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **DI**: Dagger Hilt
- **Async**: Kotlin Coroutines + Flow
- **Database**: Room
- **Bluetooth/USB**: Android Bluetooth Classic + usb-serial-for-android
- **Testing**: JUnit 5, MockK, Espresso
- **Logging**: Timber
- **UI**: Material Design 3, ViewBinding, Dark Mode

## Project Structure

```
cardiag-pro/
├── .claude/                    # Development documentation
│   ├── claude.md              # Project context and decisions
│   ├── implementation-plan.md # Detailed implementation roadmap
│   ├── workflow/              # Development workflow guides
│   └── templates/             # Code templates
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/cardiag/pro/
│       │   │   ├── CarDiagApp.kt
│       │   │   ├── data/
│       │   │   │   ├── connection/    # Connection adapters
│       │   │   │   └── model/         # Data models
│       │   │   ├── di/               # Dependency injection
│       │   │   └── ui/               # UI components
│       │   └── res/
│       ├── test/                     # Unit tests
│       └── androidTest/              # UI tests
└── README.md
```

## Building the Project

### Prerequisites
- Android Studio Hedgehog | 2023.1.1 or newer
- JDK 17
- Android SDK with API 24-34
- Gradle 8.4

### Build Instructions

```bash
# Clone the repository
git clone [repository-url]
cd cardiag-pro

# Build the project
./gradlew build

# Run tests
./gradlew test

# Install on device/emulator
./gradlew installDebug
```

## Development Workflow

This project follows an iteration-based development approach with clear phases and milestones. See [`.claude/implementation-plan.md`](.claude/implementation-plan.md) for the complete roadmap.

### Iteration Workflow
1. Review iteration plan
2. Implement features with tests
3. Run tests (`./gradlew test`)
4. Verify build (`./gradlew build`)
5. Commit changes
6. Proceed to next iteration

## Testing

- **Unit Tests**: JUnit 5 + MockK (target: 80%+ coverage)
- **Integration Tests**: Database and connection management
- **UI Tests**: Espresso for user flows

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Generate coverage report
./gradlew jacocoTestReport
```

## Documentation

- **Project Context**: [`.claude/claude.md`](.claude/claude.md)
- **Implementation Plan**: [`.claude/implementation-plan.md`](.claude/implementation-plan.md)
- **Workflow Guides**: [`.claude/workflow/`](.claude/workflow/)

## License

[To be determined]

## Contributing

[To be determined]

## Acknowledgments

- ELM327 protocol documentation
- Android Open Source Project
- Material Design 3 guidelines
