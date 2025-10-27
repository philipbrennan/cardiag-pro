# Claude Context - CarDiag Pro

## Project Overview

CarDiag Pro is a professional OBD2 diagnostic Android application that provides comprehensive vehicle diagnostics with a focus on manufacturer-specific diagnostic trouble codes (DTCs). Unlike generic OBD2 apps that only display standard P0xxx codes, CarDiag Pro includes manufacturer-specific P1xxx codes for BMW, Volkswagen, and Nissan, providing more detailed diagnostic information. The app also extends beyond engine diagnostics to cover Transmission, ABS, and SRS/Airbag systems.

**Unique Selling Points**:
1. **Manufacturer-Specific Codes**: Comprehensive P1xxx code database for BMW, VW, and Nissan
2. **Multi-ECU Support**: Diagnostics for Engine, Transmission, ABS, and SRS systems (not just engine)
3. **Professional Features**: Freeze frame data, diagnostic history, PDF reports
4. **Multiple Adapter Support**: ELM327 Bluetooth, USB (FT232), and BLE

## User Requirements

### Primary Goals
1. Connect to OBD2 adapters (ELM327 Bluetooth, USB FT232-based, BLE)
2. Read and clear diagnostic trouble codes from multiple ECU systems
3. Display manufacturer-specific codes for BMW, VW, and Nissan
4. Provide freeze frame data and diagnostic history
5. Export professional PDF diagnostic reports
6. Implement Material Design 3 with dark mode support

### User Specifications
- **Build System**: Gradle with Kotlin DSL
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Testing**: JUnit 5 + MockK + Espresso
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Dagger Hilt
- **Database**: Room (for DTC codes and history)
- **Must work offline**: Local DTC code database
- **UI**: Material Design 3, dark mode, ViewBinding

### User Preferences
- **Phased Development**: 4 phases with 8 core iterations
- **Feature Priority**: Basic diagnostics first, real-time monitoring in Phase 2
- **Graceful Degradation**: Advanced features should not block basic functionality (e.g., manual manufacturer selection if VIN decode fails)
- **Testing**: 80%+ code coverage goal
- **Approval Gates**: Per-iteration approval after tests pass and build succeeds

## Current Status

### Iteration 0: Planning & Setup
**Status**: In Progress

**What's Being Built**:
- Project structure creation
- Android project initialization
- Documentation setup (this file + implementation-plan.md)
- Git repository initialization

**Deliverable**: Project foundation ready for development

**Blockers**: None

### Next Up: Iteration 1 - Foundation & Architecture
**Goals**:
- Set up Android project with MVVM + Hilt
- Implement Bluetooth and USB connection to ELM327 adapters
- Basic ELM327 protocol communication
- Material Design 3 UI shell with dark mode

**Awaiting**: Complete project setup, then begin Iteration 1

## Key Design Decisions

### Connection Architecture
**Decision**: Adapter pattern for multiple connection types (Bluetooth, USB, BLE)

**Rationale**:
- Different connection types have different APIs and lifecycle management
- Adapter pattern allows unified interface for the diagnostic layer
- Keeps connection-specific code isolated and testable

**Implementation**:
```kotlin
interface ConnectionAdapter {
    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
    suspend fun sendCommand(command: String): Result<String>
    val connectionState: StateFlow<ConnectionState>
}

// Implementations: BluetoothConnectionAdapter, UsbSerialConnectionAdapter, BleConnectionAdapter
```

### ELM327 Protocol Layer
**Decision**: Separate protocol layer from connection layer

**Rationale**:
- ELM327 protocol is the same regardless of connection type (Bluetooth/USB/BLE)
- Separation of concerns improves testability
- Protocol logic can be unit tested without Android dependencies

**Implementation**:
`ELM327Protocol` class that takes a `ConnectionAdapter` as dependency

### DTC Code Database
**Decision**: Local Room database with pre-populated codes

**Rationale**:
- App must work offline
- Faster lookups than network calls
- Predictable behavior and no API dependencies

**Implementation**:
Database shipped with APK, migrations for future code additions. Starting with ~3000 codes (generic + BMW/VW/Nissan specific).

### VIN Decoding
**Decision**: Local VIN decoder with graceful fallback

**Rationale**:
- User specified "advanced features should not block basic features"
- If VIN decode fails, provide manual manufacturer selection
- No external API dependencies

**Implementation**:
Try automatic VIN decode → fallback to manual selection dropdown

### Manufacturer Code Management
**Decision**: Single table with manufacturer field (null = generic)

**Rationale**:
- Simplifies queries and allows easy expansion to more manufacturers
- Avoids complex join queries
- Clear data model

**Implementation**:
```kotlin
@Entity(tableName = "dtc_codes")
data class DtcCodeEntity(
    @PrimaryKey val code: String,
    val system: String, // P, C, B, U
    val manufacturer: String?, // null = generic, "BMW", "VW", "NISSAN"
    val description: String,
    val possibleCauses: String?,
    val severity: String // "LOW", "MEDIUM", "HIGH", "CRITICAL"
)
```

## Important Context for Future Work

### ELM327 Protocol Notes
- ELM327 is ASCII-based serial protocol (commands and responses in ASCII)
- Commands end with `\r` (carriage return)
- Responses end with `>` prompt
- Auto-protocol detection with `ATSP0` command
- Mode commands: `01` = live data, `03` = read DTCs, `04` = clear DTCs, `09` = vehicle info
- DTC format: `43 01 33 02 00` → Parse as `P0133` (1 code) and `P0200`

### Multi-ECU Access
- Engine ECU: Standard OBD2 (works on all vehicles)
- Other ECUs (TCM, ABS, SRS): Require CAN addressing, may vary by manufacturer
- Use `ATSH` command to set CAN header for specific ECU addresses
- Common addresses: Engine=0x7E0, Transmission=0x7E1, ABS=0x7B0, SRS=0x7E5 (vary by make/model)

### Android USB Serial
- Use `com.hoho.android:usb-serial-for-android` library for FT232 support
- Requires USB permission dialogs
- Device must support USB Host mode (most phones API 21+ do)

### Battery Optimization
- Bluetooth scanning is battery-intensive
- Disable scanning when app is backgrounded
- Use WorkManager for background tasks (future: monitoring service)

## Common Patterns Used

### Repository Pattern with Result Wrapper
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

class DiagnosticRepository @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val dtcCodeDao: DtcCodeDao
) {
    suspend fun readEngineCodes(): Result<List<DtcCode>> = try {
        val response = connectionManager.sendCommand("03")
        val codes = ELM327Parser.parseDtcResponse(response)
        val enrichedCodes = codes.map { code ->
            val definition = dtcCodeDao.getCodeDefinition(code)
            DtcCode(code, definition?.description, definition?.severity)
        }
        Result.Success(enrichedCodes)
    } catch (e: Exception) {
        Timber.e(e, "Failed to read engine codes")
        Result.Error(e)
    }
}
```

### ViewModel with StateFlow
```kotlin
@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val repository: DiagnosticRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiagnosticUiState>(DiagnosticUiState.Idle)
    val uiState: StateFlow<DiagnosticUiState> = _uiState.asStateFlow()

    fun readCodes(system: EcuSystem) {
        viewModelScope.launch {
            _uiState.value = DiagnosticUiState.Loading
            when (val result = repository.readCodes(system)) {
                is Result.Success -> _uiState.value = DiagnosticUiState.Success(result.data)
                is Result.Error -> _uiState.value = DiagnosticUiState.Error(result.exception.message)
            }
        }
    }
}
```

## File Structure Reference

### Target Structure (After Iteration 1)
```
cardiag-pro/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/cardiag/pro/
│       │   │   ├── CarDiagApp.kt
│       │   │   ├── di/
│       │   │   ├── data/
│       │   │   │   ├── connection/
│       │   │   │   ├── local/
│       │   │   │   ├── model/
│       │   │   │   └── repository/
│       │   │   └── ui/
│       │   └── res/
│       ├── test/
│       └── androidTest/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
└── .claude/
```

## Workflow Guidelines

### Before Starting Each Iteration
1. Review iteration plan in `implementation-plan.md`
2. Ask clarifying questions if anything is unclear
3. Create todo list with `TodoWrite` tool
4. Get user confirmation before starting coding

### During Each Iteration
1. Write production code following MVVM + Repository pattern
2. Write unit tests for business logic (80%+ coverage goal)
3. Update todos as tasks complete
4. Log progress and blockers

### After Each Iteration
1. Run `./gradlew test` (verify all tests pass)
2. Run `./gradlew build` (verify compilation and no lint errors)
3. Commit with descriptive message
4. Get user approval before proceeding to next iteration

### Communication Style with User
- Be concise and direct
- Ask questions when requirements are unclear
- Don't make assumptions about business logic
- Confirm major architectural decisions
- Provide progress updates after completing tasks

## Important Notes

### What NOT to Do
❌ Don't hardcode strings (use strings.xml)
❌ Don't skip error handling (always log and handle gracefully)
❌ Don't access database from UI thread
❌ Don't hold Activity/Fragment references in ViewModels
❌ Don't use GlobalScope for coroutines
❌ Don't commit secrets or API keys

### What TO Do
✅ Use Timber for all logging
✅ Use sealed classes for state management
✅ Inject dependencies via constructor
✅ Write defensive code (null checks, validation)
✅ Use proper lifecycle scopes (viewModelScope, lifecycleScope)
✅ Follow Material Design 3 guidelines
✅ Test business logic thoroughly

## Testing Strategy

### Unit Tests (JUnit 5 + MockK)
- Repository layer (business logic)
- ViewModels (state management)
- Parsers (ELM327 response parsing)
- Utilities (VIN decoder, formatters)
- **Target**: 80%+ coverage

### Integration Tests
- Database operations (Room DAOs)
- Connection management (with mocked adapters)
- End-to-end diagnostic flow

### UI Tests (Espresso)
- Connection flow
- Read codes flow
- Clear codes flow
- Navigation between screens

## Known Issues & Limitations

### Current Limitations
1. **Phase 1 Limitation**: Only 3 manufacturers initially (BMW, VW, Nissan)
2. **Real-time monitoring**: Planned for Phase 2
3. **BLE Support**: Planned for Phase 1, but may be deferred if complex
4. **Code Database**: Starting with ~3000 codes, will expand in phases

### Platform Limitations
1. **USB Serial**: Requires USB Host mode (not available on all devices)
2. **Bluetooth**: Location permission required for BLE scanning (Android 10+)
3. **ELM327 Clones**: Wide variation in clone quality and command support
4. **Multi-ECU Access**: Some manufacturers use proprietary protocols beyond OBD2 standard

## Resources

### Official Documentation
- [ELM327 Datasheet](http://elmelectronics.com/DSheets/ELM327DS.pdf)
- [OBD-II PIDs Wikipedia](https://en.wikipedia.org/wiki/OBD-II_PIDs)
- [SAE J1979 Standard](https://www.sae.org/standards/content/j1979_202104/)
- [Android Bluetooth Guide](https://developer.android.com/guide/topics/connectivity/bluetooth)

### Libraries
- [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android) - USB serial communication
- [Dagger Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Timber](https://github.com/JakeWharton/timber) - Logging

### Protocol References
- [OBD Codes Reference](https://www.obd-codes.com/)
- [Total Car Diagnostics DTC List](https://www.totalcardiagnostics.com/support/Knowledgebase/Article/View/21)

## Success Metrics

### Per Iteration
- ✅ All tests pass (`./gradlew test`)
- ✅ Build succeeds (`./gradlew build`)
- ✅ Code meets iteration requirements
- ✅ User approves deliverables

### Phase 1 (MVP - v1.0) Goals
- ✅ Connect to ELM327 Bluetooth and USB adapters
- ✅ Read and clear engine DTCs (P0xxx + P1xxx)
- ✅ Auto-detect manufacturer via VIN (with manual fallback)
- ✅ Local database with 500+ DTCs (generic + 3 manufacturers)
- ✅ Material Design 3 UI with dark mode
- ✅ 80%+ test coverage
- ✅ No crashes or ANRs

### Overall Project Goals
- Professional-grade OBD2 diagnostic tool
- Best-in-class manufacturer code coverage
- Multi-ECU diagnostic capabilities
- Intuitive, modern Material Design 3 UI
- Reliable, well-tested codebase
- Positive user reviews for accuracy and usability

## Current Blockers

None - ready to begin Iteration 1.

## Next Actions

**Immediate**:
1. ✅ Complete project setup (in progress)
2. Initialize Android Gradle project
3. Set up basic app structure
4. Initialize git repository

**Then proceed with**:
- Iteration 1: Foundation & Architecture (Bluetooth/USB connection, ELM327 protocol basics)

---

**Workflow Reference**: See `.claude/workflow/README.md` for detailed development guidelines
