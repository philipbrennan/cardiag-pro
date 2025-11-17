package com.cardiag.pro.ui.diagnostic

import com.cardiag.pro.data.model.DiagnosticTroubleCode
import com.cardiag.pro.data.model.ECUSystem
import com.cardiag.pro.data.model.Manufacturer
import com.cardiag.pro.data.model.Result
import com.cardiag.pro.data.model.Severity
import com.cardiag.pro.data.model.VehicleInfo
import com.cardiag.pro.data.repository.DtcRepository
import com.cardiag.pro.data.repository.VinRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import timber.log.Timber

/**
 * Unit tests for DiagnosticViewModel.
 * Tests state management, repository interactions, and business logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: DiagnosticViewModel
    private lateinit var vinRepository: VinRepository
    private lateinit var dtcRepository: DtcRepository

    private val testVehicleInfo = VehicleInfo(
        vin = "WBADT4345G2123456",
        manufacturer = Manufacturer.BMW,
        year = 2016,
        model = null,
        isManuallyEntered = false
    )

    private val testDtcCodes = listOf(
        DiagnosticTroubleCode(
            code = "P0300",
            description = "Random Misfire",
            system = ECUSystem.ENGINE,
            severity = Severity.HIGH
        ),
        DiagnosticTroubleCode(
            code = "P0133",
            description = "O2 Sensor Slow Response",
            system = ECUSystem.ENGINE,
            severity = Severity.MEDIUM
        )
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Plant silent test tree
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {}
        })

        vinRepository = mockk()
        dtcRepository = mockk()
        
        viewModel = DiagnosticViewModel(vinRepository, dtcRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        Timber.uprootAll()
    }

    // ========== Initial State Tests ==========

    @Test
    fun `initial state should be Idle`() = runTest {
        // Then
        assertEquals(DiagnosticUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `initial vehicleInfo should be null`() = runTest {
        // Then
        assertNull(viewModel.vehicleInfo.value)
    }

    @Test
    fun `initial dtcCodes should be empty`() = runTest {
        // Then
        assertTrue(viewModel.dtcCodes.value.isEmpty())
    }

    // ========== readVin Success Tests ==========

    @Test
    fun `readVin should update state to ReadingVin then VinReadSuccess`() = runTest {
        // Given
        coEvery { vinRepository.readVin() } returns Result.Success(testVehicleInfo)

        // When
        viewModel.readVin()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertTrue(finalState is DiagnosticUiState.VinReadSuccess)
        assertEquals(testVehicleInfo, (finalState as DiagnosticUiState.VinReadSuccess).vehicleInfo)
    }

    @Test
    fun `readVin should store vehicleInfo on success`() = runTest {
        // Given
        coEvery { vinRepository.readVin() } returns Result.Success(testVehicleInfo)

        // When
        viewModel.readVin()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(testVehicleInfo, viewModel.vehicleInfo.value)
    }

    @Test
    fun `readVin should call vinRepository readVin`() = runTest {
        // Given
        coEvery { vinRepository.readVin() } returns Result.Success(testVehicleInfo)

        // When
        viewModel.readVin()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { vinRepository.readVin() }
    }

    // ========== readVin Error Tests ==========

    @Test
    fun `readVin should update state to Error on failure`() = runTest {
        // Given
        val errorMessage = "Connection failed"
        coEvery { vinRepository.readVin() } returns Result.Error(Exception(errorMessage))

        // When
        viewModel.readVin()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertTrue(finalState is DiagnosticUiState.Error)
        assertEquals(errorMessage, (finalState as DiagnosticUiState.Error).message)
    }

    @Test
    fun `readVin should not store vehicleInfo on error`() = runTest {
        // Given
        coEvery { vinRepository.readVin() } returns Result.Error(Exception("Failed"))

        // When
        viewModel.readVin()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNull(viewModel.vehicleInfo.value)
    }

    @Test
    fun `readVin should handle exception with null message`() = runTest {
        // Given
        coEvery { vinRepository.readVin() } returns Result.Error(Exception())

        // When
        viewModel.readVin()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertTrue(finalState is DiagnosticUiState.Error)
        assertEquals("Failed to read VIN", (finalState as DiagnosticUiState.Error).message)
    }

    // ========== setManualVehicleInfo Tests ==========

    @Test
    fun `setManualVehicleInfo should create manual vehicle info`() = runTest {
        // Given
        val manualVehicleInfo = testVehicleInfo.copy(isManuallyEntered = true)
        every { vinRepository.createManualVehicleInfo(any(), any(), any(), any()) } returns manualVehicleInfo

        // When
        viewModel.setManualVehicleInfo(Manufacturer.BMW, "TESTVIN123")

        // Then
        assertEquals(manualVehicleInfo, viewModel.vehicleInfo.value)
    }

    @Test
    fun `setManualVehicleInfo should update state to VinReadSuccess`() = runTest {
        // Given
        val manualVehicleInfo = testVehicleInfo.copy(isManuallyEntered = true)
        every { vinRepository.createManualVehicleInfo(any(), any(), any(), any()) } returns manualVehicleInfo

        // When
        viewModel.setManualVehicleInfo(Manufacturer.BMW)

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is DiagnosticUiState.VinReadSuccess)
    }

    @Test
    fun `setManualVehicleInfo should handle null VIN`() = runTest {
        // Given
        val manualVehicleInfo = testVehicleInfo.copy(vin = "MANUAL", isManuallyEntered = true)
        every { vinRepository.createManualVehicleInfo(null, Manufacturer.VOLKSWAGEN, any(), any()) } returns manualVehicleInfo

        // When
        viewModel.setManualVehicleInfo(Manufacturer.VOLKSWAGEN, null)

        // Then
        verify { vinRepository.createManualVehicleInfo(null, Manufacturer.VOLKSWAGEN, any(), any()) }
    }

    // ========== readDtcCodes Success Tests ==========

    @Test
    fun `readDtcCodes should update state to ReadingCodes then CodesReadSuccess`() = runTest {
        // Given
        coEvery { dtcRepository.readDtcCodes(any(), any()) } returns Result.Success(testDtcCodes)

        // When
        viewModel.readDtcCodes(ECUSystem.ENGINE)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertTrue(finalState is DiagnosticUiState.CodesReadSuccess)
        assertEquals(2, (finalState as DiagnosticUiState.CodesReadSuccess).count)
    }

    @Test
    fun `readDtcCodes should store codes on success`() = runTest {
        // Given
        coEvery { dtcRepository.readDtcCodes(any(), any()) } returns Result.Success(testDtcCodes)

        // When
        viewModel.readDtcCodes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(testDtcCodes, viewModel.dtcCodes.value)
    }

    @Test
    fun `readDtcCodes should call dtcRepository with correct system`() = runTest {
        // Given
        coEvery { dtcRepository.readDtcCodes(ECUSystem.TRANSMISSION, any()) } returns Result.Success(emptyList())

        // When
        viewModel.readDtcCodes(ECUSystem.TRANSMISSION)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { dtcRepository.readDtcCodes(ECUSystem.TRANSMISSION, any()) }
    }

    @Test
    fun `readDtcCodes should handle empty list with NoCodesFound state`() = runTest {
        // Given
        coEvery { dtcRepository.readDtcCodes(any(), any()) } returns Result.Success(emptyList())

        // When
        viewModel.readDtcCodes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(DiagnosticUiState.NoCodesFound, viewModel.uiState.value)
        assertTrue(viewModel.dtcCodes.value.isEmpty())
    }

    // ========== readDtcCodes Error Tests ==========

    @Test
    fun `readDtcCodes should update state to Error on failure`() = runTest {
        // Given
        val errorMessage = "OBD connection lost"
        coEvery { dtcRepository.readDtcCodes(any(), any()) } returns Result.Error(Exception(errorMessage))

        // When
        viewModel.readDtcCodes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertTrue(finalState is DiagnosticUiState.Error)
        assertEquals(errorMessage, (finalState as DiagnosticUiState.Error).message)
    }

    @Test
    fun `readDtcCodes should not modify codes on error`() = runTest {
        // Given
        coEvery { dtcRepository.readDtcCodes(any(), any()) } returns Result.Error(Exception("Failed"))

        // When
        viewModel.readDtcCodes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.dtcCodes.value.isEmpty())
    }

    // ========== clearDtcCodes Tests ==========

    @Test
    fun `clearDtcCodes should update state to ClearingCodes then CodesCleared`() = runTest {
        // Given
        coEvery { dtcRepository.clearDtcCodes() } returns Result.Success(Unit)

        // When
        viewModel.clearDtcCodes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(DiagnosticUiState.CodesCleared, viewModel.uiState.value)
    }

    @Test
    fun `clearDtcCodes should clear stored codes on success`() = runTest {
        // Given - Set some codes first
        coEvery { dtcRepository.readDtcCodes(any(), any()) } returns Result.Success(testDtcCodes)
        viewModel.readDtcCodes()
        testDispatcher.scheduler.advanceUntilIdle()
        
        coEvery { dtcRepository.clearDtcCodes() } returns Result.Success(Unit)

        // When
        viewModel.clearDtcCodes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.dtcCodes.value.isEmpty())
    }

    @Test
    fun `clearDtcCodes should call dtcRepository clearDtcCodes`() = runTest {
        // Given
        coEvery { dtcRepository.clearDtcCodes() } returns Result.Success(Unit)

        // When
        viewModel.clearDtcCodes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { dtcRepository.clearDtcCodes() }
    }

    @Test
    fun `clearDtcCodes should update state to Error on failure`() = runTest {
        // Given
        val errorMessage = "Clear command failed"
        coEvery { dtcRepository.clearDtcCodes() } returns Result.Error(Exception(errorMessage))

        // When
        viewModel.clearDtcCodes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertTrue(finalState is DiagnosticUiState.Error)
        assertEquals(errorMessage, (finalState as DiagnosticUiState.Error).message)
    }

    @Test
    fun `clearDtcCodes should not clear codes on error`() = runTest {
        // Given - Set some codes first
        coEvery { dtcRepository.readDtcCodes(any(), any()) } returns Result.Success(testDtcCodes)
        viewModel.readDtcCodes()
        testDispatcher.scheduler.advanceUntilIdle()
        
        coEvery { dtcRepository.clearDtcCodes() } returns Result.Error(Exception("Failed"))

        // When
        viewModel.clearDtcCodes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(testDtcCodes, viewModel.dtcCodes.value)
    }

    // ========== saveDiagnosticSession Tests ==========

    @Test
    fun `saveDiagnosticSession should save with vehicle info and codes`() = runTest {
        // Given - Set vehicle info and codes
        coEvery { vinRepository.readVin() } returns Result.Success(testVehicleInfo)
        viewModel.readVin()
        testDispatcher.scheduler.advanceUntilIdle()
        
        coEvery { dtcRepository.readDtcCodes(any(), any()) } returns Result.Success(testDtcCodes)
        viewModel.readDtcCodes()
        testDispatcher.scheduler.advanceUntilIdle()
        
        coEvery { dtcRepository.saveDiagnosticSession(any(), any(), any(), any(), any(), any()) } returns Result.Success(1L)

        // When
        viewModel.saveDiagnosticSession("Test notes")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { 
            dtcRepository.saveDiagnosticSession(
                vin = testVehicleInfo.vin,
                manufacturer = testVehicleInfo.manufacturer,
                codes = testDtcCodes,
                freezeFrames = any(), // Now accepts freeze frames (may be empty map)
                notes = "Test notes",
                systemScanned = any()
            )
        }
    }

    @Test
    fun `saveDiagnosticSession should update state to SessionSaved on success`() = runTest {
        // Given - Set codes
        coEvery { dtcRepository.readDtcCodes(any(), any()) } returns Result.Success(testDtcCodes)
        viewModel.readDtcCodes()
        testDispatcher.scheduler.advanceUntilIdle()
        
        coEvery { dtcRepository.saveDiagnosticSession(any(), any(), any(), any(), any(), any()) } returns Result.Success(1L)

        // When
        viewModel.saveDiagnosticSession()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(DiagnosticUiState.SessionSaved, viewModel.uiState.value)
    }

    @Test
    fun `saveDiagnosticSession should return error when no codes`() = runTest {
        // Given - No codes
        
        // When
        viewModel.saveDiagnosticSession()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertTrue(finalState is DiagnosticUiState.Error)
        assertEquals("No codes to save", (finalState as DiagnosticUiState.Error).message)
    }

    @Test
    fun `saveDiagnosticSession should handle save error`() = runTest {
        // Given - Set codes
        coEvery { dtcRepository.readDtcCodes(any(), any()) } returns Result.Success(testDtcCodes)
        viewModel.readDtcCodes()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val errorMessage = "Database error"
        coEvery { dtcRepository.saveDiagnosticSession(any(), any(), any(), any(), any(), any()) } returns Result.Error(Exception(errorMessage))

        // When
        viewModel.saveDiagnosticSession()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.value
        assertTrue(finalState is DiagnosticUiState.Error)
        assertEquals(errorMessage, (finalState as DiagnosticUiState.Error).message)
    }

    @Test
    fun `saveDiagnosticSession should handle null VIN`() = runTest {
        // Given - Set codes but no vehicle info
        coEvery { dtcRepository.readDtcCodes(any(), any()) } returns Result.Success(testDtcCodes)
        viewModel.readDtcCodes()
        testDispatcher.scheduler.advanceUntilIdle()
        
        coEvery { dtcRepository.saveDiagnosticSession(any(), any(), any(), any(), any(), any()) } returns Result.Success(1L)

        // When
        viewModel.saveDiagnosticSession()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { 
            dtcRepository.saveDiagnosticSession(
                vin = null,
                manufacturer = null,
                codes = testDtcCodes,
                freezeFrames = any(),
                notes = any(),
                systemScanned = any()
            )
        }
    }

    // ========== resetState Tests ==========

    @Test
    fun `resetState should set state to Idle`() = runTest {
        // Given - Set state to something else
        coEvery { vinRepository.readVin() } returns Result.Error(Exception("Error"))
        viewModel.readVin()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.resetState()

        // Then
        assertEquals(DiagnosticUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `resetState should not clear vehicleInfo`() = runTest {
        // Given - Set vehicle info
        coEvery { vinRepository.readVin() } returns Result.Success(testVehicleInfo)
        viewModel.readVin()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.resetState()

        // Then
        assertEquals(testVehicleInfo, viewModel.vehicleInfo.value)
    }

    @Test
    fun `resetState should not clear dtcCodes`() = runTest {
        // Given - Set codes
        coEvery { dtcRepository.readDtcCodes(any(), any()) } returns Result.Success(testDtcCodes)
        viewModel.readDtcCodes()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.resetState()

        // Then
        assertEquals(testDtcCodes, viewModel.dtcCodes.value)
    }

    // ========== State Flow Tests ==========

    @Test
    fun `state flows should be observable`() = runTest {
        // Given
        coEvery { vinRepository.readVin() } returns Result.Success(testVehicleInfo)

        // When
        viewModel.readVin()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Flows should emit values
        assertNotNull(viewModel.uiState.first())
        assertNotNull(viewModel.vehicleInfo.first())
        assertNotNull(viewModel.dtcCodes.first())
    }
}
