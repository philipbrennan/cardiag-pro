# CarDiag Pro - Complete Implementation Plan

## Overview
Professional OBD2 diagnostic Android app with manufacturer-specific code databases for BMW, VW, and Nissan, supporting Engine, Transmission, ABS, and SRS system diagnostics.

## Technology Stack
- **Language**: Kotlin
- **Architecture**: MVVM with Repository pattern
- **Build System**: Gradle with Kotlin DSL
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **DI**: Dagger Hilt
- **Async**: Kotlin Coroutines + Flow
- **Database**: Room (DTC codes + diagnostic history)
- **Bluetooth**: Android Bluetooth Classic + BLE
- **USB Serial**: usb-serial-for-android (FT232 support)
- **Testing**: JUnit 5, MockK, Espresso
- **Logging**: Timber
- **PDF Generation**: Android PrintManager / iText
- **UI**: Material Design 3, ViewBinding, Dark Mode

---

## Phase 1: Foundation & Basic Engine Diagnostics

## Iteration 1: Project Foundation & Architecture ⏸️

### Goals
- Set up Android project with MVVM + Hilt architecture
- Implement Bluetooth and USB connection to ELM327 adapters
- Basic ELM327 protocol communication
- Material Design 3 UI shell with dark mode

### Tasks
- [ ] Create Android project with Gradle Kotlin DSL
- [ ] Configure dependencies (Hilt, Room, Coroutines, Timber, usb-serial-for-android)
- [ ] Set up MVVM architecture with Hilt
- [ ] Implement ConnectionAdapter interface (abstraction for Bluetooth/USB/BLE)
- [ ] Create BluetoothConnectionAdapter for ELM327 Bluetooth adapters
- [ ] Create UsbSerialConnectionAdapter for FT232-based USB adapters
- [ ] Implement ConnectionManager to manage adapter lifecycle
- [ ] Build basic ELM327Protocol class (send/receive commands)
- [ ] Create Material Design 3 UI shell with dark mode support
- [ ] Implement runtime permissions (Bluetooth, Location, USB)
- [ ] Build connection screen with adapter scanning/selection
- [ ] Write unit tests for connection adapters and protocol
- [ ] Initialize git repository with .gitignore
- [ ] Verify build succeeds and all tests pass

### Deliverables
- App can scan for and display available Bluetooth/USB adapters
- Can connect to ELM327 adapter (Bluetooth or USB)
- Can send basic AT commands and receive responses
- Material Design 3 UI with dark mode support
- Connection state management working

### Files Created
```
app/src/main/java/com/cardiag/pro/
├── CarDiagApp.kt
├── di/
│   ├── AppModule.kt
│   └── ConnectionModule.kt
├── data/
│   ├── connection/
│   │   ├── ConnectionAdapter.kt (interface)
│   │   ├── BluetoothConnectionAdapter.kt
│   │   ├── UsbSerialConnectionAdapter.kt
│   │   ├── ConnectionManager.kt
│   │   └── ELM327Protocol.kt
│   └── model/
│       ├── ConnectionState.kt
│       └── AdapterInfo.kt
└── ui/
    ├── MainActivity.kt
    ├── MainViewModel.kt
    └── connection/
        ├── ConnectionFragment.kt
        └── ConnectionViewModel.kt

app/src/test/java/com/cardiag/pro/
└── data/connection/
    ├── ELM327ProtocolTest.kt
    └── ConnectionManagerTest.kt
```

### Technical Considerations
- Use Adapter pattern for Bluetooth/USB/BLE to allow unified interface
- ELM327 protocol is ASCII-based (commands end with \r, responses end with >)
- USB permissions require user dialogs - handle gracefully
- Bluetooth requires location permission on Android 6+
- Test with mock adapters to avoid hardware dependency

---

## Iteration 2: ELM327 Communication & Basic Engine Codes 🔄

### Goals
- Implement full ELM327 protocol with auto-detection
- Read and clear P0xxx generic engine DTCs
- Implement VIN decoding with manual fallback
- Create Room database for DTC code definitions

### Tasks
- [ ] Implement ELM327 initialization sequence (ATZ, ATE0, ATL0, ATSP0)
- [ ] Add protocol auto-detection (ISO 9141-2, ISO 14230-4 KWP, ISO 15765-4 CAN)
- [ ] Implement Mode 03 (read DTCs) and Mode 04 (clear DTCs)
- [ ] Implement Mode 09 (vehicle info - VIN request PID 02)
- [ ] Create VINDecoder utility to extract manufacturer from VIN
- [ ] Design Room database schema for DTC codes
- [ ] Populate database with ~500 generic P0xxx codes
- [ ] Create DtcCodeDao for code lookups
- [ ] Build ELM327Parser to parse DTC responses (e.g., "43 01 33" → "P0133")
- [ ] Create DiagnosticRepository with readCodes() and clearCodes() methods
- [ ] Build diagnostics UI to display code list
- [ ] Add manual manufacturer selection fallback
- [ ] Write unit tests for parser, VIN decoder, repository
- [ ] Verify build succeeds and all tests pass

### Deliverables
- Can read engine DTCs and display with descriptions
- Can clear engine DTCs
- Auto-detects vehicle manufacturer via VIN
- Manual manufacturer selection fallback if VIN fails
- Local database with 500+ generic P0xxx code definitions

### Key Components
```
app/src/main/java/com/cardiag/pro/
├── data/
│   ├── local/
│   │   ├── CarDiagDatabase.kt
│   │   ├── DtcCodeDao.kt
│   │   └── DtcCodeEntity.kt
│   ├── model/
│   │   ├── DtcCode.kt
│   │   ├── VehicleInfo.kt
│   │   └── EcuSystem.kt (enum: ENGINE, TRANSMISSION, ABS, SRS)
│   ├── parser/
│   │   ├── ELM327Parser.kt
│   │   └── VINDecoder.kt
│   └── repository/
│       ├── DiagnosticRepository.kt
│       └── VehicleRepository.kt
└── ui/
    └── diagnostics/
        ├── DiagnosticsFragment.kt
        ├── DiagnosticsViewModel.kt
        └── DtcCodeAdapter.kt (RecyclerView adapter)
```

### Database Schema
```kotlin
@Entity(tableName = "dtc_codes")
data class DtcCodeEntity(
    @PrimaryKey val code: String,           // e.g., "P0133"
    val system: String,                     // P, C, B, U
    val manufacturer: String?,              // null = generic, "BMW", "VW", "NISSAN"
    val description: String,                // Human-readable description
    val possibleCauses: String?,            // Comma-separated causes
    val severity: String                    // "LOW", "MEDIUM", "HIGH", "CRITICAL"
)
```

### Technical Considerations
- ELM327 DTC format: "43 01 33 02 00" means 1 code (01), codes are P0133, P0200
- First byte after command: number of codes
- Each code is 2 bytes: first byte bits 6-7 = system (00=P, 01=C, 10=B, 11=U)
- VIN is 17 characters, manufacturer code is positions 1-3
- Fallback to manual selection if VIN read fails or manufacturer unknown

---

## Iteration 3: Manufacturer-Specific Engine Codes (BMW, VW, Nissan) 🔄

### Goals
- Add P1xxx manufacturer-specific engine codes for BMW, VW, Nissan
- Expand database to 1000+ codes
- Implement manufacturer detection and filtering

### Tasks
- [ ] Research and compile P1xxx code databases:
  - BMW: 150-200 codes
  - Volkswagen/Audi: 150-200 codes
  - Nissan: 100-150 codes
- [ ] Create database migration to add manufacturer-specific codes
- [ ] Update VINDecoder to support BMW, VW, Nissan detection
- [ ] Add manufacturer badge/indicator in UI
- [ ] Implement manufacturer filter in DtcCodeDao queries
- [ ] Create manufacturer selection dialog for manual override
- [ ] Update diagnostics UI to show manufacturer-specific info
- [ ] Add unit tests for manufacturer detection
- [ ] Write tests for manufacturer-specific code lookups
- [ ] Verify build succeeds and all tests pass

### Deliverables
- Database contains 1000+ codes (500 generic + 500 manufacturer-specific)
- Displays manufacturer-specific P1xxx codes with descriptions
- Automatic manufacturer detection from VIN
- Manual manufacturer selection with dropdown
- Manufacturer badge shown in UI

### Database Update
```kotlin
// Database populated with entries like:
DtcCodeEntity(
    code = "P1234",
    system = "P",
    manufacturer = "BMW",
    description = "Fuel Pump Relay Circuit Malfunction",
    possibleCauses = "Faulty relay, wiring, ECU",
    severity = "MEDIUM"
)
```

### Technical Considerations
- VIN manufacturer codes: BMW starts with "WBA", VW with "WVW", Nissan with "JN1"
- Store manufacturer preference in SharedPreferences
- Query database with: `WHERE (manufacturer IS NULL OR manufacturer = ?)` to get both generic and manufacturer-specific codes
- Show manufacturer badge prominently in UI (icon + text)

---

## Phase 2: Multi-ECU Support & Enhanced Features

## Iteration 4: Multi-ECU Support (Transmission, ABS, SRS) 🔄

### Goals
- Extend diagnostics beyond engine to TCM, ABS, SRS systems
- Support C-codes (chassis) and B-codes (body)
- Expand database to 2000-3000 codes

### Tasks
- [ ] Research CAN addressing for non-engine ECUs
- [ ] Implement ATSH command to set CAN header for specific ECUs
- [ ] Add support for reading Transmission (TCM) codes
- [ ] Add support for reading ABS codes (C-codes)
- [ ] Add support for reading SRS/Airbag codes (B-codes)
- [ ] Expand database with C0xxx, C1xxx (ABS), B0xxx, B1xxx (SRS) codes
- [ ] Create ECU system selector UI (chips or tabs)
- [ ] Add system-specific icons and color coding
- [ ] Update ELM327Protocol to support multi-ECU communication
- [ ] Update DiagnosticRepository to handle multiple ECU systems
- [ ] Write integration tests for multi-ECU communication
- [ ] Verify build succeeds and all tests pass

### Deliverables
- Can read codes from Engine, Transmission, ABS, SRS systems
- System selector UI with visual distinction
- Database expanded to 2000-3000 codes
- Clear indication of which system each code belongs to

### Key Components
```
app/src/main/java/com/cardiag/pro/
└── data/
    ├── model/
    │   └── EcuSystem.kt (enum with ENGINE, TRANSMISSION, ABS, SRS)
    └── parser/
        └── EcuAddressMapper.kt (maps ECU system to CAN addresses)
```

### UI Enhancement
```
┌─────────────────────────────────────────┐
│  [Engine] [Trans] [ABS] [SRS]           │ ← System chips
├─────────────────────────────────────────┤
│ 🔧 P0301 - Cylinder 1 Misfire Detected  │
│    Severity: HIGH                        │
│    └─ Possible: Spark plug, ignition    │
├─────────────────────────────────────────┤
│ ⚠️  P1234 - BMW Fuel System Fault       │
│    Severity: MEDIUM                      │
└─────────────────────────────────────────┘
```

### Technical Considerations
- Common CAN addresses (manufacturer-specific):
  - Engine: 0x7E0/0x7E8
  - Transmission: 0x7E1/0x7E9
  - ABS: 0x7B0/0x7B8
  - SRS: 0x7E5/0x7ED
- Not all vehicles support all ECUs via OBD2
- Handle "NO DATA" responses gracefully
- Use different colors for each system (Engine=blue, Trans=purple, ABS=orange, SRS=red)

---

## Iteration 5: Freeze Frame Data & Code History 🔄

### Goals
- Capture freeze frame snapshots at time of fault
- Store diagnostic history with timestamps
- Display historical diagnostic sessions

### Tasks
- [ ] Implement Mode 02 (freeze frame data) requests
- [ ] Parse freeze frame sensor values (RPM, speed, coolant temp, etc.)
- [ ] Create DiagnosticSession entity for history tracking
- [ ] Create DiagnosticHistoryEntry entity for per-code records
- [ ] Create DiagnosticSessionDao and HistoryRepository
- [ ] Build history UI with RecyclerView
- [ ] Add freeze frame detail screen
- [ ] Implement history filtering (by date, system, severity)
- [ ] Add "Clear History" functionality
- [ ] Export freeze frame data as text
- [ ] Write unit tests for freeze frame parser
- [ ] Write tests for history repository
- [ ] Verify build succeeds and all tests pass

### Deliverables
- View freeze frame data for each DTC
- Diagnostic history log with session tracking
- Filter history by date, system, or severity
- Export freeze frame data

### Data Models
```kotlin
@Entity(tableName = "diagnostic_sessions")
data class DiagnosticSession(
    @PrimaryKey val sessionId: String,
    val timestamp: Long,
    val vehicleVin: String?,
    val manufacturer: String?,
    val totalCodesFound: Int
)

@Entity(tableName = "diagnostic_history")
data class DiagnosticHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val dtcCode: String,
    val system: String,
    val description: String,
    val freezeFrameData: String?,  // JSON string
    val timestamp: Long
)

data class FreezeFrameData(
    val rpm: Int?,
    val speed: Int?,
    val coolantTemp: Int?,
    val fuelTrim: Double?,
    val engineLoad: Int?,
    val throttlePosition: Int?
)
```

### Technical Considerations
- Mode 02 PIDs match Mode 01 PIDs for live data
- Freeze frame 0 contains data from most recent DTC
- Not all vehicles support freeze frames for all codes
- Store freeze frame as JSON for flexibility
- Limit history to last 100 sessions to avoid database bloat

---

## Phase 3: Real-Time Monitoring & Advanced Features

## Iteration 6: Real-Time Sensor Monitoring (Basic) 🔄

### Goals
- Display live OBD2 sensor data (RPM, speed, coolant temp, etc.)
- Implement efficient polling mechanism
- Basic data recording

### Tasks
- [ ] Implement Mode 01 PID requests for live data
- [ ] Support common PIDs (RPM, speed, coolant, throttle, fuel level, MAF, etc.)
- [ ] Create LiveDataManager for efficient polling
- [ ] Build monitoring UI with gauges/text displays
- [ ] Implement configurable refresh rate (1-2 Hz)
- [ ] Add units toggle (metric/imperial)
- [ ] Add data recording capability
- [ ] Write unit tests for PID parsing
- [ ] Write UI tests for monitoring screen
- [ ] Verify build succeeds and all tests pass

### Deliverables
- Live sensor dashboard with 8-12 common parameters
- Smooth real-time updates (1-2 Hz)
- Metric/imperial unit toggle
- Basic recording functionality

### Key Components
```
app/src/main/java/com/cardiag/pro/
├── data/
│   ├── model/
│   │   └── LiveSensorData.kt
│   └── parser/
│       └── PidParser.kt
├── monitoring/
│   └── LiveDataManager.kt
└── ui/
    └── monitoring/
        ├── MonitoringFragment.kt
        ├── MonitoringViewModel.kt
        └── SensorGaugeView.kt (custom view)
```

### Technical Considerations
- Mode 01 polling: don't poll too fast (max 10 Hz, recommend 1-2 Hz)
- Use coroutine delays between requests
- Cancel polling when screen is not visible
- Common PIDs: 0C (RPM), 0D (speed), 05 (coolant), 11 (throttle), 04 (engine load)
- Parse multi-byte values correctly (big-endian)

---

## Iteration 7: PDF Reports & Data Export 🔄

### Goals
- Export diagnostic reports as professional PDF
- Support CSV export for raw data
- Implement share functionality

### Tasks
- [ ] Implement PDF generation with vehicle info header
- [ ] Include all active DTCs with descriptions in report
- [ ] Add freeze frame data section to PDF
- [ ] Format PDF with branding and timestamps
- [ ] Implement CSV export for diagnostic history
- [ ] Add share functionality (email, cloud storage)
- [ ] Implement print report option via Android PrintManager
- [ ] Write unit tests for PDF/CSV generation
- [ ] Verify build succeeds and all tests pass

### Deliverables
- Professional PDF diagnostic reports
- CSV export for data analysis
- Share/print functionality
- Branded report format

### Report Structure
```
┌────────────────────────────────────────┐
│     CarDiag Pro - Diagnostic Report    │
│                                         │
│ Date: 2025-10-27 15:30:00              │
│ VIN: WBADT43452G123456                 │
│ Manufacturer: BMW                       │
│ ───────────────────────────────────────│
│                                         │
│ Engine (ECU)                            │
│   ✓ No codes found                     │
│                                         │
│ Transmission (TCM)                      │
│   ! P0730 - Incorrect Gear Ratio       │
│     Severity: Medium                    │
│     Freeze Frame:                       │
│       RPM: 2500, Speed: 45 km/h        │
│       Coolant: 85°C, Throttle: 30%     │
│                                         │
│ ABS System                              │
│   ✓ No codes found                     │
│                                         │
│ SRS System                              │
│   ⚠ B1234 - Driver Airbag Circuit      │
│     Severity: High                      │
│ ───────────────────────────────────────│
│ Generated by CarDiag Pro v1.0          │
└────────────────────────────────────────┘
```

### Technical Considerations
- Use Android PrintManager for PDF generation (simpler than iText)
- Create HTML template, convert to PDF via WebView
- Include timestamp, VIN, manufacturer in header
- Color-code severity levels
- Save PDFs to Pictures/CarDiagPro/ directory
- Request storage permissions properly (scoped storage on Android 10+)

---

## Phase 4: Polish & Optimization

## Iteration 8: Polish, Settings & Production Ready 🔄

### Goals
- Final polish and optimization
- Implement settings screen
- Prepare for release

### Tasks
- [ ] Create settings screen with preferences:
  - Unit system (metric/imperial)
  - Refresh rate for monitoring
  - Default manufacturer
  - Dark/light theme toggle
- [ ] Add onboarding/tutorial screens for first-time users
- [ ] Design and implement app icon
- [ ] Create splash screen
- [ ] Optimize battery usage (disable scanning when backgrounded)
- [ ] Implement connection timeout handling (30s timeout)
- [ ] Add helpful error messages and troubleshooting tips
- [ ] Create "About" screen with version info and links
- [ ] Add help/FAQ section
- [ ] Perform full QA testing (manual + automated)
- [ ] Code cleanup and documentation
- [ ] Write comprehensive UI tests
- [ ] Verify build succeeds and all tests pass
- [ ] Prepare for Play Store release (screenshots, description)

### Deliverables
- Polished, production-ready app
- Settings screen with all preferences
- Onboarding experience
- App icon and branding
- Optimized for battery and performance
- Ready for Google Play Store submission

### Key Components
```
app/src/main/java/com/cardiag/pro/
├── data/preferences/
│   └── UserPreferences.kt (DataStore)
└── ui/
    ├── settings/
    │   ├── SettingsFragment.kt
    │   └── SettingsViewModel.kt
    ├── onboarding/
    │   └── OnboardingActivity.kt
    └── about/
        └── AboutFragment.kt
```

### Technical Considerations
- Use Jetpack DataStore for preferences (modern replacement for SharedPreferences)
- Implement proper lifecycle management for Bluetooth scanning
- Use WorkManager for any background tasks
- Add ProGuard rules for production build
- Test on multiple devices and Android versions (API 24-34)
- Ensure graceful degradation on devices without USB host support

---

## Project Structure (Final)

```
cardiag-pro/
├── .claude/
│   ├── claude.md
│   ├── implementation-plan.md
│   ├── workflow/
│   └── templates/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/cardiag/pro/
│   │   │   │   ├── CarDiagApp.kt
│   │   │   │   ├── di/
│   │   │   │   │   ├── AppModule.kt
│   │   │   │   │   ├── DatabaseModule.kt
│   │   │   │   │   └── ConnectionModule.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── connection/
│   │   │   │   │   │   ├── ConnectionAdapter.kt
│   │   │   │   │   │   ├── BluetoothConnectionAdapter.kt
│   │   │   │   │   │   ├── UsbSerialConnectionAdapter.kt
│   │   │   │   │   │   ├── ConnectionManager.kt
│   │   │   │   │   │   └── ELM327Protocol.kt
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── CarDiagDatabase.kt
│   │   │   │   │   │   ├── DtcCodeDao.kt
│   │   │   │   │   │   ├── DiagnosticSessionDao.kt
│   │   │   │   │   │   └── entities/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── DtcCode.kt
│   │   │   │   │   │   ├── DiagnosticSession.kt
│   │   │   │   │   │   ├── FreezeFrame.kt
│   │   │   │   │   │   ├── VehicleInfo.kt
│   │   │   │   │   │   └── EcuSystem.kt
│   │   │   │   │   ├── parser/
│   │   │   │   │   │   ├── ELM327Parser.kt
│   │   │   │   │   │   ├── VINDecoder.kt
│   │   │   │   │   │   ├── PidParser.kt
│   │   │   │   │   │   └── EcuAddressMapper.kt
│   │   │   │   │   ├── preferences/
│   │   │   │   │   │   └── UserPreferences.kt
│   │   │   │   │   └── repository/
│   │   │   │   │       ├── DiagnosticRepository.kt
│   │   │   │   │       ├── VehicleRepository.kt
│   │   │   │   │       └── HistoryRepository.kt
│   │   │   │   ├── monitoring/
│   │   │   │   │   └── LiveDataManager.kt
│   │   │   │   ├── export/
│   │   │   │   │   ├── PdfReportGenerator.kt
│   │   │   │   │   └── CsvExporter.kt
│   │   │   │   └── ui/
│   │   │   │       ├── MainActivity.kt
│   │   │   │       ├── MainViewModel.kt
│   │   │   │       ├── connection/
│   │   │   │       ├── diagnostics/
│   │   │   │       ├── monitoring/
│   │   │   │       ├── history/
│   │   │   │       ├── settings/
│   │   │   │       ├── about/
│   │   │   │       └── common/
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── values/
│   │   │   │   ├── values-night/ (dark mode)
│   │   │   │   ├── drawable/
│   │   │   │   └── mipmap/
│   │   │   └── AndroidManifest.xml
│   │   ├── test/
│   │   │   └── java/com/cardiag/pro/
│   │   └── androidTest/
│   │       └── java/com/cardiag/pro/
│   └── build.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
├── .gitignore
└── README.md
```

---

## Dependencies (Complete)

```kotlin
dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore (Preferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // USB Serial (for FT232 adapters)
    implementation("com.github.mik3y:usb-serial-for-android:3.7.3")

    // Timber (Logging)
    implementation("com.jakewharton.timber:timber:5.0.1")

    // WorkManager (background tasks)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    // Android Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("io.mockk:mockk-android:1.13.8")
}
```

---

## Success Criteria

### Phase 1 (MVP - v1.0)
✅ Connect to ELM327 Bluetooth and USB (FT232) adapters
✅ Read and clear engine DTCs (P0xxx generic + P1xxx for BMW/VW/Nissan)
✅ Auto-detect manufacturer via VIN with manual fallback
✅ Local database with 1000+ DTCs (generic + manufacturer-specific)
✅ Material Design 3 UI with dark mode
✅ Display code descriptions and severity
✅ 80%+ test coverage
✅ No crashes or ANRs

### Phase 2 (v1.5)
✅ Multi-ECU support (Engine, Transmission, ABS, SRS)
✅ Freeze frame data capture and display
✅ Diagnostic history with session tracking
✅ PDF report export
✅ 2000-3000 DTC codes in database
✅ CSV export for data analysis

### Phase 3 (v2.0)
✅ Real-time sensor monitoring (8-12 parameters)
✅ Data recording functionality
✅ BLE adapter support (if not in Phase 1)

### Non-Functional Requirements
✅ 80%+ test coverage
✅ No memory leaks
✅ Battery-efficient (scanning disabled when backgrounded)
✅ Smooth UI (60fps)
✅ Handles poor connections gracefully
✅ Comprehensive error messages

### Performance Targets
- Connection time: < 10 seconds
- Code read time: < 5 seconds
- Database query time: < 100 ms
- App launch time: < 2 seconds

---

## Risk Mitigation

### Technical Risks
1. **ELM327 Clone Variations**: Many clone adapters don't implement full protocol
   - **Mitigation**: Test with multiple adapters, implement robust error handling, provide troubleshooting guide

2. **Vehicle Compatibility**: Not all vehicles support all ECUs via OBD2
   - **Mitigation**: Gracefully handle "NO DATA" responses, maintain compatibility list, provide feedback mechanism

3. **USB Permissions**: Complex permission flow on Android
   - **Mitigation**: Clear user instructions, handle permission denial gracefully, provide troubleshooting steps

4. **Manufacturer Code Accuracy**: Difficult to source official manufacturer codes
   - **Mitigation**: Cross-reference multiple sources, allow user feedback on incorrect codes, plan for database updates

### Testing Risks
1. **Hardware Dependency**: Testing requires real vehicles and adapters
   - **Mitigation**: Create mock adapters for unit tests, use ELM327 simulators, test with real hardware when possible

2. **Multi-device Testing**: Wide variety of Android devices
   - **Mitigation**: Use Firebase Test Lab, test on representative devices (Samsung, Google Pixel, budget phones)

### Data Risks
1. **Database Size**: 3000+ codes will increase APK size
   - **Mitigation**: Use Room pre-populated database, compress data, consider on-demand downloads for rare codes in future

---

## Timeline Estimate

| Phase | Iterations | Estimated Time | Cumulative |
|-------|-----------|----------------|------------|
| **Phase 1: Foundation & Basic Diagnostics** |
| 1. Foundation & Architecture | 2-3 days | 2-3 days | 2-3 days |
| 2. ELM327 & Basic Engine Codes | 2-3 days | 4-6 days | 4-6 days |
| 3. Manufacturer-Specific Codes | 1-2 days | 5-8 days | 5-8 days |
| **Phase 2: Multi-ECU & Enhanced** |
| 4. Multi-ECU Support | 2-3 days | 7-11 days | 7-11 days |
| 5. Freeze Frame & History | 2-3 days | 9-14 days | 9-14 days |
| **Phase 3: Monitoring & Export** |
| 6. Real-Time Monitoring | 2-3 days | 11-17 days | 11-17 days |
| 7. PDF Reports & Export | 1-2 days | 12-19 days | 12-19 days |
| **Phase 4: Polish** |
| 8. Polish & Production Ready | 2-3 days | 14-22 days | 14-22 days |

**Total MVP (Phases 1 + 4)**: 7-11 days
**Total v1.5 (Phases 1-2 + 4)**: 12-17 days
**Total v2.0 (All Phases)**: 14-22 days

*Note: Timeline includes testing, approval gates, and iteration buffers*

---

## Notes

- Each iteration must pass all tests before proceeding
- User approval required before moving to next iteration
- Focus on defensive programming and comprehensive error handling
- ELM327 adapters vary widely - implement robust timeout and retry logic
- Graceful degradation: advanced features should not block basic functionality
- Battery optimization is critical - Bluetooth scanning is power-intensive
- Consider creating a demo mode for testing without hardware

---

## Future Enhancements (Post-v2.0)

### Phase 5: Extended Coverage
- Add Honda, Toyota, Ford, Mercedes manufacturer codes (5000+ total codes)
- Expand to 10+ ECU systems (BCM, HVAC, instrument cluster)
- Support ISO 14229 (UDS) protocol
- Support SAE J1939 (heavy-duty vehicles)

### Phase 6: Advanced Features
- Graph recording and playback
- Performance metrics (0-60, quarter mile)
- Fuel economy tracking
- Trip computer functionality
- Cloud backup of diagnostic history

### Phase 7: Professional Features
- Bi-directional controls (actuator tests)
- Advanced coding and programming
- Live parameter adjustments
- Custom PID definitions
