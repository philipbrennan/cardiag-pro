package com.cardiag.pro.data.repository

import com.cardiag.pro.data.connection.ELM327Protocol
import com.cardiag.pro.data.model.Manufacturer
import com.cardiag.pro.data.model.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import timber.log.Timber

/**
 * Unit tests for VinRepository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VinRepositoryTest {

    private lateinit var repository: VinRepository
    private lateinit var elm327Protocol: ELM327Protocol

    @BeforeEach
    fun setup() {
        elm327Protocol = mockk()
        repository = VinRepository(elm327Protocol)
        
        // Plant a test tree for Timber
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                // Silent test tree
            }
        })
    }

    @AfterEach
    fun tearDown() {
        Timber.uprootAll()
    }

    // ========== Success Cases ==========

    @Test
    fun `readVin should parse valid multi-line response successfully`() = runTest {
        // Given - Multi-line VIN response
        val vinResponse = """
            49 02 01 57 42 41 44
            49 02 02 54 34 33 34
            49 02 03 35 32 47 31
            49 02 04 32 33 34 35
            49 02 05 36
        """.trimIndent()
        
        coEvery { elm327Protocol.sendCommand("0902") } returns vinResponse

        // When
        val result = repository.readVin()

        // Then
        assertTrue(result is Result.Success)
        val vehicleInfo = (result as Result.Success).data
        assertEquals("WBADT43452G123456", vehicleInfo.vin)
        assertEquals(Manufacturer.BMW, vehicleInfo.manufacturer)
        assertFalse(vehicleInfo.isManuallyEntered)
    }

    @Test
    fun `readVin should parse valid single-line response successfully`() = runTest {
        // Given - Single line VIN response
        val vinResponse = "49 02 01 57 42 41 44 54 34 33 34 35 32 47 31 32 33 34 35 36"
        
        coEvery { elm327Protocol.sendCommand("0902") } returns vinResponse

        // When
        val result = repository.readVin()

        // Then
        assertTrue(result is Result.Success)
        val vehicleInfo = (result as Result.Success).data
        assertEquals("WBADT43452G123456", vehicleInfo.vin)
    }

    @Test
    fun `readVin should parse CAN frame format response successfully`() = runTest {
        // Given - CAN frame format
        val vinResponse = """
            0: 49 02 01 57 42 41 44
            1: 54 34 33 34 35 32 47
            2: 31 32 33 34 35 36
        """.trimIndent()
        
        coEvery { elm327Protocol.sendCommand("0902") } returns vinResponse

        // When
        val result = repository.readVin()

        // Then
        assertTrue(result is Result.Success)
        val vehicleInfo = (result as Result.Success).data
        assertEquals("WBADT43452G123456", vehicleInfo.vin)
    }

    @ParameterizedTest
    @CsvSource(
        "WBADT43452G123456, BMW",
        "WVW1234567890123, VOLKSWAGEN",
        "JN1AB1234CD567890, NISSAN",
        "1HGBH41JXMN109186, UNKNOWN"
    )
    fun `readVin should correctly identify manufacturer from VIN`(vin: String, expectedManufacturer: String) = runTest {
        // Given - VIN as hex bytes
        val hexVin = vin.map { it.code.toString(16).padStart(2, '0') }.joinToString(" ")
        val vinResponse = "49 02 01 $hexVin"
        
        coEvery { elm327Protocol.sendCommand("0902") } returns vinResponse

        // When
        val result = repository.readVin()

        // Then
        assertTrue(result is Result.Success)
        val vehicleInfo = (result as Result.Success).data
        assertEquals(Manufacturer.valueOf(expectedManufacturer), vehicleInfo.manufacturer)
    }

    // ========== Retry Logic Tests ==========

    @Test
    fun `readVin should retry on failure and succeed on second attempt`() = runTest {
        // Given - First attempt fails, second succeeds
        val validResponse = "49 02 01 57 42 41 44 54 34 33 34 35 32 47 31 32 33 34 35 36"
        
        coEvery { elm327Protocol.sendCommand("0902") } returnsMany listOf(
            null, // First attempt fails
            validResponse // Second attempt succeeds
        )

        // When
        val result = repository.readVin()

        // Then
        assertTrue(result is Result.Success)
        coVerify(exactly = 2) { elm327Protocol.sendCommand("0902") }
    }

    @Test
    fun `readVin should retry up to 3 times before failing`() = runTest {
        // Given - All attempts fail
        coEvery { elm327Protocol.sendCommand("0902") } returns null

        // When
        val result = repository.readVin()

        // Then
        assertTrue(result is Result.Error)
        coVerify(exactly = 3) { elm327Protocol.sendCommand("0902") }
    }

    // ========== Error Cases ==========

    @Test
    fun `readVin should return error when response is NO DATA`() = runTest {
        // Given
        coEvery { elm327Protocol.sendCommand("0902") } returns "NO DATA"

        // When
        val result = repository.readVin()

        // Then
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).exception
        assertTrue(error.message?.contains("not supported", ignoreCase = true) == true)
        
        // Should not retry for "not supported" error
        coVerify(exactly = 1) { elm327Protocol.sendCommand("0902") }
    }

    @Test
    fun `readVin should return error when response contains ERROR`() = runTest {
        // Given
        coEvery { elm327Protocol.sendCommand("0902") } returns "ERROR"

        // When
        val result = repository.readVin()

        // Then
        assertTrue(result is Result.Error)
        coVerify(exactly = 1) { elm327Protocol.sendCommand("0902") }
    }

    @Test
    fun `readVin should return error when VIN is too short`() = runTest {
        // Given - Only 10 characters instead of 17
        val shortVin = "49 02 01 57 42 41 44 54 34 33"
        coEvery { elm327Protocol.sendCommand("0902") } returns shortVin

        // When
        val result = repository.readVin()

        // Then
        assertTrue(result is Result.Error)
    }

    @Test
    fun `readVin should return error when VIN contains invalid characters`() = runTest {
        // Given - VIN with 'I', 'O', 'Q' which are not valid
        val invalidVin = "WBADT43452GI2O45Q"
        val hexVin = invalidVin.map { it.code.toString(16).padStart(2, '0') }.joinToString(" ")
        val vinResponse = "49 02 01 $hexVin"
        
        coEvery { elm327Protocol.sendCommand("0902") } returns vinResponse

        // When
        val result = repository.readVin()

        // Then
        assertTrue(result is Result.Error)
    }

    @Test
    fun `readVin should handle malformed hex response`() = runTest {
        // Given - Invalid hex characters
        val malformedResponse = "49 02 01 ZZ XX YY"
        coEvery { elm327Protocol.sendCommand("0902") } returns malformedResponse

        // When
        val result = repository.readVin()

        // Then
        assertTrue(result is Result.Error)
    }

    @Test
    fun `readVin should handle exception from protocol layer`() = runTest {
        // Given
        coEvery { elm327Protocol.sendCommand("0902") } throws RuntimeException("Connection lost")

        // When
        val result = repository.readVin()

        // Then
        assertTrue(result is Result.Error)
        assertEquals("Connection lost", (result as Result.Error).exception.message)
    }

    // ========== Manual Entry Tests ==========

    @Test
    fun `createManualVehicleInfo should create vehicle info with manual flag`() {
        // When
        val vehicleInfo = repository.createManualVehicleInfo(
            vin = "WBADT43452G123456",
            manufacturer = Manufacturer.BMW,
            year = 2020,
            model = "330i"
        )

        // Then
        assertEquals("WBADT43452G123456", vehicleInfo.vin)
        assertEquals(Manufacturer.BMW, vehicleInfo.manufacturer)
        assertEquals(2020, vehicleInfo.year)
        assertEquals("330i", vehicleInfo.model)
        assertTrue(vehicleInfo.isManuallyEntered)
    }

    @Test
    fun `createManualVehicleInfo should use MANUAL as VIN when not provided`() {
        // When
        val vehicleInfo = repository.createManualVehicleInfo(
            vin = null,
            manufacturer = Manufacturer.VOLKSWAGEN
        )

        // Then
        assertEquals("MANUAL", vehicleInfo.vin)
        assertEquals(Manufacturer.VOLKSWAGEN, vehicleInfo.manufacturer)
        assertTrue(vehicleInfo.isManuallyEntered)
    }

    // ========== Year Extraction Tests ==========

    @Test
    fun `readVin should extract year from VIN correctly`() = runTest {
        // Given - 10th character 'G' = 2016
        val vin = "WBADT43452G123456"
        val hexVin = vin.map { it.code.toString(16).padStart(2, '0') }.joinToString(" ")
        val vinResponse = "49 02 01 $hexVin"
        
        coEvery { elm327Protocol.sendCommand("0902") } returns vinResponse

        // When
        val result = repository.readVin()

        // Then
        assertTrue(result is Result.Success)
        val vehicleInfo = (result as Result.Success).data
        assertEquals(2016, vehicleInfo.year)
    }
}
