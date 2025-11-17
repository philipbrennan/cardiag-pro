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

## Iteration 4: VIN Decoding Fixes & Multi-ECU Support ✅

### Goals
- Fix and enhance VIN reading and decoding functionality
- Extend diagnostics beyond engine to TCM, ABS, SRS systems
- Support C-codes (chassis) and B-codes (body)
- Expand database to 2000-3000 codes

### Tasks
- [x] **Fix VIN Decoding Issues:**
  - [x] Debug VIN reading (Mode 09, PID 02) response parsing
  - [x] Test VIN reading with real OBD2 adapter
  - [x] Improve VIN validation and error handling
  - [x] Add VIN display in diagnostics UI
  - [x] Ensure manufacturer detection works correctly from VIN
  - [x] Add manual VIN entry option as fallback
- [x] Research CAN addressing for non-engine ECUs
- [x] Implement ATSH command to set CAN header for specific ECUs
- [x] Add support for reading Transmission (TCM) codes
- [x] Add support for reading ABS codes (C-codes)
- [x] Add support for reading SRS/Airbag codes (B-codes)
- [x] Expand database with C0xxx, C1xxx (ABS), B0xxx, B1xxx (SRS) codes
- [x] Create ECU system selector UI (chips or tabs)
- [x] Add system-specific icons and color coding
- [x] Update ELM327Protocol to support multi-ECU communication
- [x] Update DiagnosticRepository to handle multiple ECU systems
- [x] Write integration tests for multi-ECU communication
- [x] Verify build succeeds and all tests pass

### Completed - November 17, 2025
**Database Stats**: 1,516 total DTC codes
- Generic P-codes: 735
- BMW P1xxx: 212
- VW P1xxx: 246
- Nissan P1xxx: 119
- ABS C-codes: 83
- SRS B-codes: 121

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

## Iteration 5: Freeze Frame Data & Code History ✅

### Goals
- Capture freeze frame snapshots at time of fault
- Store diagnostic history with timestamps
- Display historical diagnostic sessions

### Tasks
- [x] Implement Mode 02 (freeze frame data) requests
- [x] Parse freeze frame sensor values (RPM, speed, coolant temp, etc.)
- [x] Create DiagnosticSession entity for history tracking
- [x] Create DiagnosticHistoryEntry entity for per-code records
- [x] Create DiagnosticSessionDao and HistoryRepository
- [x] Build history UI with RecyclerView
- [x] Add freeze frame detail screen
- [x] Implement history filtering (by date, system, severity)
- [x] Add "Clear History" functionality
- [x] Export freeze frame data as text
- [x] Write unit tests for freeze frame parser
- [x] Write tests for history repository
- [x] Verify build succeeds and all tests pass

### Completed - November 17, 2025
**Freeze Frame Features**:
- Automatic freeze frame reading after DTC scan
- Supports 10+ sensor PIDs (RPM, speed, temps, fuel trim, MAF, etc.)
- Tap-to-view detailed freeze frame data
- Freeze frames saved with diagnostic sessions
- History UI displays sessions with freeze frame count

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

## Iteration 6: Real-Time Sensor Monitoring (Basic) ✅

### Goals
- Display live OBD2 sensor data (RPM, speed, coolant temp, etc.)
- Implement efficient polling mechanism
- Basic data recording

### Tasks
- [x] Implement Mode 01 PID requests for live data
- [x] Support common PIDs (RPM, speed, coolant, throttle, fuel level, MAF, etc.)
- [x] Create LiveDataManager for efficient polling
- [x] Build monitoring UI with gauges/text displays
- [x] Implement configurable refresh rate (0.5-5 Hz)
- [x] Add units toggle (metric/imperial)
- [x] Add optimized batch polling mode
- [x] Write unit tests for PID parsing
- [x] Write UI tests for monitoring screen
- [x] Verify build succeeds and all tests pass

### Completed - November 17, 2025
**Live Monitoring Features**:
- 15+ PIDs supported (RPM, speed, coolant, intake temp, throttle, engine load, MAF, fuel pressure, timing advance, fuel trim, O2 sensors, fuel level, fuel rate, intake pressure)
- Configurable refresh rates: 0.5Hz, 1Hz, 2Hz, 5Hz
- Optimized batch polling mode (3-4 PIDs per request)
- Metric/imperial unit conversion
- Real-time dashboard with 3 sections: Engine Performance, Temperature & Pressure, Fuel System
- Lifecycle-aware (pauses when fragment not visible)

### Deliverables
- Live sensor dashboard with 15+ parameters
- Smooth real-time updates (0.5-5 Hz configurable)
- Metric/imperial unit toggle
- Optimized polling mechanism

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
        └── fragment_monitoring.xml
```

### Technical Considerations
- Mode 01 polling: configurable rates from 0.5-5 Hz
- Use coroutine delays between requests
- Cancel polling when screen is not visible (battery optimization)
- Common PIDs: 0C (RPM), 0D (speed), 05 (coolant), 11 (throttle), 04 (engine load), 10 (MAF), 0F (intake temp), etc.
- Parse multi-byte values correctly (big-endian)
- Batch polling improves efficiency (3-4 PIDs per request)

---

## Iteration 7: PDF Reports & Data Export 🔄

### Goals
- Export diagnostic reports as professional PDF
- Support CSV export for raw data
- Implement share functionality

### Tasks
- [x] Implement PDF generation with vehicle info header
- [x] Include all active DTCs with descriptions in report
- [x] Add freeze frame data section to PDF
- [x] Format PDF with branding and timestamps
- [x] Implement CSV export for diagnostic history
- [ ] Create ReportViewModel for report generation state management
- [ ] Create ReportsFragment UI for report selection and generation
- [ ] Add share functionality (email, cloud storage)
- [ ] Implement print report option via Android PrintManager
- [ ] Add reports tab to bottom navigation
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

---

## Iteration 3.5: Debug Logging & Communication Analysis 🚧 *NEW*

### Goals
- Add comprehensive in-app logging UI for debugging OBD2 communication
- Implement file-based logging for offline analysis
- Debug FT232RL USB adapter connection issues with Nissan Leaf
- Improve user feedback during connection and diagnostic operations
- Create log export functionality for remote debugging

### Problem Statement
During initial hardware testing with FT232RL USB adapter on Nissan Leaf ZE0:
- Adapter detected successfully (USB enumeration working)
- Activity light flashes but connection fails
- No VIN or diagnostic codes retrieved
- Insufficient visibility into ELM327 command/response flow
- No persistent logs for post-mortem analysis

### Tasks
- [ ] Create LogEntry data model with timestamp, level, tag, message
- [ ] Implement FileLogger using coroutines for async file I/O
- [ ] Create LogRepository to manage log entries (in-memory + file)
- [ ] Build LogFragment with RecyclerView for real-time log viewing
- [ ] Add log filtering by level (DEBUG, INFO, WARN, ERROR)
- [ ] Implement log export to file (share via Android ShareSheet)
- [ ] Enhance ELM327Protocol with detailed command/response logging
- [ ] Add connection state change logging
- [ ] Log USB device details (vendor ID, product ID, device class)
- [ ] Add timing information for each command (execution duration)
- [ ] Create custom Timber Tree for file logging
- [ ] Implement log rotation (max file size 10MB, keep last 5 files)
- [ ] Add "Copy Logs" button to share via email/Drive
- [ ] Enhance UI with progress messages during operations
- [ ] Add troubleshooting hints based on error patterns
- [ ] Write unit tests for logging infrastructure
- [ ] Test on physical device with real adapter

### Deliverables
- In-app log viewer accessible via bottom navigation tab
- Detailed logging of all ELM327 commands and responses
- File-based logs stored in app-specific storage
- Log export functionality (ZIP file with all logs)
- Enhanced error messages with actionable suggestions
- Debug mode toggle in settings

### Files Created/Modified
```
app/src/main/java/com/cardiag/pro/
├── data/
│   ├── model/
│   │   ├── LogEntry.kt (new)
│   │   └── LogLevel.kt (new)
│   ├── logging/
│   │   ├── FileLogger.kt (new)
│   │   ├── LogRepository.kt (new)
│   │   └── FileLoggingTree.kt (new)
│   └── connection/
│       ├── ELM327Protocol.kt (modify - add detailed logging)
│       ├── UsbSerialConnectionAdapter.kt (modify - log USB details)
│       └── BluetoothConnectionAdapter.kt (modify - log BT details)
├── ui/
│   ├── logs/
│   │   ├── LogsFragment.kt (new)
│   │   ├── LogsViewModel.kt (new)
│   │   └── LogEntryAdapter.kt (new)
│   └── MainActivity.kt (modify - add logs tab)
└── util/
    └── LogUtils.kt (new - helper functions)

app/src/main/res/
├── layout/
│   ├── fragment_logs.xml (new)
│   └── item_log_entry.xml (new)
└── menu/
    └── bottom_navigation_menu.xml (modify - add logs tab)
```

### Log Entry Model
```kotlin
data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val metadata: Map<String, String> = emptyMap()
)

enum class LogLevel(val priority: Int, val displayName: String) {
    DEBUG(2, "DEBUG"),
    INFO(3, "INFO"),
    WARN(4, "WARN"),
    ERROR(5, "ERROR")
}
```

### Enhanced ELM327 Logging Example
```kotlin
class ELM327Protocol {
    suspend fun sendCommand(command: String): Result<String> {
        val startTime = System.currentTimeMillis()
        
        // Log outgoing command
        LogRepository.log(
            level = LogLevel.DEBUG,
            tag = "ELM327",
            message = "→ Sending: $command",
            metadata = mapOf("command" to command)
        )
        
        val result = connectionManager.sendCommand(command)
        val duration = System.currentTimeMillis() - startTime
        
        when (result) {
            is Result.Success -> {
                LogRepository.log(
                    level = LogLevel.DEBUG,
                    tag = "ELM327",
                    message = "← Received: ${result.data} (${duration}ms)",
                    metadata = mapOf(
                        "command" to command,
                        "response" to result.data,
                        "duration_ms" to duration.toString()
                    )
                )
            }
            is Result.Error -> {
                LogRepository.log(
                    level = LogLevel.ERROR,
                    tag = "ELM327",
                    message = "✗ Command failed: ${result.exception.message} (${duration}ms)",
                    throwable = result.exception,
                    metadata = mapOf(
                        "command" to command,
                        "duration_ms" to duration.toString()
                    )
                )
            }
        }
        
        return result
    }
}
```

### Log Viewer UI Features
- Real-time log streaming (auto-scroll to bottom)
- Filter by log level (chips: ALL, DEBUG, INFO, WARN, ERROR)
- Search/filter by tag or message content
- Color-coded log levels (DEBUG=gray, INFO=blue, WARN=orange, ERROR=red)
- Expandable entries to show metadata and stack traces
- Clear logs button
- Export logs button (creates ZIP with all log files)

### File Logging Implementation
```kotlin
@Singleton
class FileLogger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val logDir = File(context.getExternalFilesDir(null), "logs")
    private val currentLogFile: File
        get() = File(logDir, "cardiag_${SimpleDateFormat("yyyyMMdd").format(Date())}.log")
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        logDir.mkdirs()
        rotateLogsIfNeeded()
    }
    
    fun log(entry: LogEntry) {
        scope.launch {
            val logLine = formatLogEntry(entry)
            currentLogFile.appendText(logLine + "\n")
            
            if (currentLogFile.length() > MAX_FILE_SIZE) {
                rotateLogsIfNeeded()
            }
        }
    }
    
    private fun formatLogEntry(entry: LogEntry): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date(entry.timestamp))
        return "[$timestamp] ${entry.level.displayName}/${entry.tag}: ${entry.message}"
    }
    
    private fun rotateLogsIfNeeded() {
        val logFiles = logDir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        
        // Keep only last 5 log files
        logFiles.drop(5).forEach { it.delete() }
    }
    
    companion object {
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024L // 10MB
    }
}
```

### Nissan Leaf Specific Debugging
Research and implement:
- Nissan-specific CAN protocol quirks
- ELM327 initialization sequence for Nissan vehicles
- Proper baud rate and protocol settings for Leaf ZE0 (2011-2017)
- Common issues with Nissan ISO 15765-4 (CAN) protocol
- Adapter compatibility matrix (some ELM327 clones don't work with Nissan)

### Expected Log Output Example
```
[2025-10-30 20:50:15.123] INFO/USB: Device connected: VID=0x0403 PID=0x6001 (FTDI FT232R)
[2025-10-30 20:50:15.456] DEBUG/USB: Setting baud rate: 38400
[2025-10-30 20:50:15.789] DEBUG/USB: Serial port opened successfully
[2025-10-30 20:50:16.012] DEBUG/ELM327: → Sending: ATZ
[2025-10-30 20:50:17.234] DEBUG/ELM327: ← Received: ELM327 v1.5 (987ms)
[2025-10-30 20:50:17.345] DEBUG/ELM327: → Sending: ATE0
[2025-10-30 20:50:17.456] DEBUG/ELM327: ← Received: OK (111ms)
[2025-10-30 20:50:17.567] DEBUG/ELM327: → Sending: ATL0
[2025-10-30 20:50:17.678] DEBUG/ELM327: ← Received: OK (111ms)
[2025-10-30 20:50:17.789] DEBUG/ELM327: → Sending: ATSP6
[2025-10-30 20:50:17.900] DEBUG/ELM327: ← Received: OK (111ms)
[2025-10-30 20:50:18.012] DEBUG/ELM327: → Sending: 0100
[2025-10-30 20:50:18.543] ERROR/ELM327: ✗ Command failed: Timeout waiting for response (531ms)
[2025-10-30 20:50:18.544] WARN/Diagnostics: Failed to initialize: No response from vehicle
```

### Technical Considerations
- Log rotation to prevent storage exhaustion
- Async logging to avoid UI blocking
- Structured logging with metadata for analysis
- USB-specific logging: device enumeration, permissions, serial settings
- ELM327-specific logging: command echoing, protocol detection, error codes
- Performance impact: minimize overhead in production builds
- Privacy: ensure no VIN or personal data in exported logs (or warn user)

### Testing Strategy
1. Unit tests for FileLogger (log rotation, formatting)
2. Integration tests for ELM327 logging with mock adapter
3. Real hardware testing with FT232RL adapter
4. Test log export and share functionality
5. Verify log file size limits and rotation
6. Test on multiple Android versions (API 24-34)

### Success Criteria
- ✅ All ELM327 commands visible in log viewer
- ✅ Can export logs and email/share for analysis
- ✅ Identify root cause of Nissan Leaf connection failure
- ✅ Logs help diagnose adapter compatibility issues
- ✅ User can understand what's happening during connection
- ✅ No performance impact on normal operation

### Timeline
- Development: 1-2 days
- Testing with real hardware: 1 day
- Bug fixes and refinement: 1 day
- **Total: 3-4 days**

---

