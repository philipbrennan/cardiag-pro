package com.cardiag.pro.data.parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import timber.log.Timber

/**
 * Unit tests for FreezeFrameParser.
 */
class FreezeFrameParserTest {

    @BeforeEach
    fun setup() {
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                // Silent test tree
            }
        })
    }

    // ========== Success Cases ==========

    @Test
    fun `parseFreezeFrame should parse RPM correctly`() {
        // Given - Frame with RPM PID (0x0C), value 0x1A 0x4F = 6735, /4 = 1683 RPM
        val response = "42 00 0C 1A 4F"
        val dtcCode = "P0300"

        // When
        val result = FreezeFrameParser.parseFreezeFrame(dtcCode, response)

        // Then
        assertNotNull(result)
        assertEquals(dtcCode, result!!.dtcCode)
        assertEquals(0, result.frameNumber)
        assertEquals(1683, result.rpm)
    }

    @Test
    fun `parseFreezeFrame should parse speed correctly`() {
        // Given - Speed PID (0x0D), value 0x50 = 80 km/h
        val response = "42 00 0D 50"
        val dtcCode = "P0301"

        // When
        val result = FreezeFrameParser.parseFreezeFrame(dtcCode, response)

        // Then
        assertNotNull(result)
        assertEquals(80, result!!.speed)
    }

    @Test
    fun `parseFreezeFrame should parse coolant temperature correctly`() {
        // Given - Coolant temp PID (0x05), value 0x64 = 100, minus 40 = 60°C
        val response = "42 00 05 64"
        val dtcCode = "P0101"

        // When
        val result = FreezeFrameParser.parseFreezeFrame(dtcCode, response)

        // Then
        assertNotNull(result)
        assertEquals(60, result!!.coolantTemp)
    }

    @Test
    fun `parseFreezeFrame should parse throttle position correctly`() {
        // Given - Throttle PID (0x11), value 0xFF = 255, * 100 / 255 = 100%
        val response = "42 00 11 FF"
        val dtcCode = "P0102"

        // When
        val result = FreezeFrameParser.parseFreezeFrame(dtcCode, response)

        // Then
        assertNotNull(result)
        assertEquals(100, result!!.throttlePosition)
    }

    @Test
    fun `parseFreezeFrame should parse engine load correctly`() {
        // Given - Engine load PID (0x04), value 0x80 = 128, * 100 / 255 = 50%
        val response = "42 00 04 80"
        val dtcCode = "P0103"

        // When
        val result = FreezeFrameParser.parseFreezeFrame(dtcCode, response)

        // Then
        assertNotNull(result)
        assertEquals(50, result!!.engineLoad)
    }

    @Test
    fun `parseFreezeFrame should parse fuel trim correctly`() {
        // Given - Short term fuel trim PID (0x06), value 0x80 = 128, (128-128) * 100/128 = 0%
        val response = "42 00 06 80"
        val dtcCode = "P0104"

        // When
        val result = FreezeFrameParser.parseFreezeFrame(dtcCode, response)

        // Then
        assertNotNull(result)
        assertEquals(0.0, result!!.shortTermFuelTrim!!, 0.1)
    }

    @ParameterizedTest
    @CsvSource(
        "0x40, -50.0",  // (64-128) * 100/128 = -50%
        "0x80, 0.0",    // (128-128) * 100/128 = 0%
        "0xC0, 50.0",   // (192-128) * 100/128 = 50%
        "0xFF, 99.2"    // (255-128) * 100/128 = 99.21875%
    )
    fun `parseFreezeFrame should calculate fuel trim percentage correctly`(hexValue: String, expected: Double) {
        // Given
        val byteValue = hexValue.removePrefix("0x").toInt(16).toString(16).padStart(2, '0')
        val response = "42 00 06 $byteValue"

        // When
        val result = FreezeFrameParser.parseFreezeFrame("P0100", response)

        // Then
        assertNotNull(result)
        assertEquals(expected, result!!.shortTermFuelTrim!!, 0.5)
    }

    @Test
    fun `parseFreezeFrame should parse intake air temperature correctly`() {
        // Given - Intake temp PID (0x0F), value 0x50 = 80, minus 40 = 40°C
        val response = "42 00 0F 50"
        val dtcCode = "P0105"

        // When
        val result = FreezeFrameParser.parseFreezeFrame(dtcCode, response)

        // Then
        assertNotNull(result)
        assertEquals(40, result!!.intakeAirTemp)
    }

    @Test
    fun `parseFreezeFrame should parse MAF airflow correctly`() {
        // Given - MAF PID (0x10), value 0x12 0x34 = 4660, / 100 = 46.6 g/s
        val response = "42 00 10 12 34"
        val dtcCode = "P0106"

        // When
        val result = FreezeFrameParser.parseFreezeFrame(dtcCode, response)

        // Then
        assertNotNull(result)
        assertEquals(46.6, result!!.mafAirFlow!!, 0.1)
    }

    @Test
    fun `parseFreezeFrame should parse fuel pressure correctly`() {
        // Given - Fuel pressure PID (0x0A), value 0x50 = 80, * 3 = 240 kPa
        val response = "42 00 0A 50"
        val dtcCode = "P0107"

        // When
        val result = FreezeFrameParser.parseFreezeFrame(dtcCode, response)

        // Then
        assertNotNull(result)
        assertEquals(240, result!!.fuelPressure)
    }

    @Test
    fun `parseFreezeFrame should parse multiple PIDs in one response`() {
        // Given - Response with RPM, Speed, Coolant, Throttle
        val response = """
            42 00 0C 1A 4F 0D 50 05 64 11 80
        """.trimIndent()
        val dtcCode = "P0300"

        // When
        val result = FreezeFrameParser.parseFreezeFrame(dtcCode, response)

        // Then
        assertNotNull(result)
        assertEquals(1683, result!!.rpm)
        assertEquals(80, result.speed)
        assertEquals(60, result.coolantTemp)
        assertEquals(50, result.throttlePosition) // 0x80 = 128, * 100 / 255 = 50%
    }

    @Test
    fun `parseFreezeFrame should parse frame number correctly`() {
        // Given - Frame 2
        val response = "42 02 0C 10 00"
        val dtcCode = "P0300"

        // When
        val result = FreezeFrameParser.parseFreezeFrame(dtcCode, response)

        // Then
        assertNotNull(result)
        assertEquals(2, result!!.frameNumber)
    }

    // ========== Edge Cases ==========

    @Test
    fun `parseFreezeFrame should handle response with spaces`() {
        // Given - Response with extra spaces
        val response = "42  00  0C  1A  4F"
        val dtcCode = "P0300"

        // When
        val result = FreezeFrameParser.parseFreezeFrame(dtcCode, response)

        // Then
        assertNotNull(result)
        assertEquals(1683, result!!.rpm)
    }

    @Test
    fun `parseFreezeFrame should handle response with newlines`() {
        // Given - Multi-line response
        val response = """
            42 00 0C 1A 4F
            0D 50 05 64
        """.trimIndent()
        val dtcCode = "P0300"

        // When
        val result = FreezeFrameParser.parseFreezeFrame(dtcCode, response)

        // Then
        assertNotNull(result)
        assertNotNull(result!!.rpm)
    }

    @Test
    fun `parseFreezeFrame should skip unknown PIDs gracefully`() {
        // Given - Response with known and unknown PIDs
        val response = "42 00 0C 1A 4F AA BB CC 0D 50"
        val dtcCode = "P0300"

        // When
        val result = FreezeFrameParser.parseFreezeFrame(dtcCode, response)

        // Then
        assertNotNull(result)
        assertEquals(1683, result!!.rpm)
        assertEquals(80, result.speed)
    }

    @Test
    fun `parseFreezeFrame should return null for non-Mode-02 response`() {
        // Given - Not a Mode 02 response (should start with 42)
        val response = "41 00 0C 1A 4F" // Mode 01 response
        val dtcCode = "P0300"

        // When
        val result = FreezeFrameParser.parseFreezeFrame(dtcCode, response)

        // Then
        assertNull(result)
    }

    @Test
    fun `parseFreezeFrame should return null for empty response`() {
        // Given
        val response = ""
        val dtcCode = "P0300"

        // When
        val result = FreezeFrameParser.parseFreezeFrame(dtcCode, response)

        // Then
        assertNull(result)
    }

    @Test
    fun `parseFreezeFrame should return null for malformed hex`() {
        // Given - Invalid hex characters
        val response = "42 00 0C ZZ YY"
        val dtcCode = "P0300"

        // When
        val result = FreezeFrameParser.parseFreezeFrame(dtcCode, response)

        // Then
        assertNull(result)
    }

    @Test
    fun `parseFreezeFrame should handle incomplete PID data`() {
        // Given - PID 0x0C (2 bytes) but only 1 byte provided
        val response = "42 00 0C 1A"
        val dtcCode = "P0300"

        // When
        val result = FreezeFrameParser.parseFreezeFrame(dtcCode, response)

        // Then
        assertNotNull(result) // Should not crash
        // RPM will be null or incorrect, but shouldn't throw
    }

    // ========== Utility Method Tests ==========

    @Test
    fun `buildFreezeFrameRequest should format frame number correctly`() {
        // When
        val command = FreezeFrameParser.buildFreezeFrameRequest("P0300", frameNumber = 0)

        // Then
        assertEquals("0200", command)
    }

    @Test
    fun `buildFreezeFrameRequestWithPIDs should format PIDs correctly`() {
        // When
        val command = FreezeFrameParser.buildFreezeFrameRequestWithPIDs(
            0,
            0x0C, // RPM
            0x0D, // Speed
            0x05  // Coolant
        )

        // Then
        assertEquals("02000c0d05", command.lowercase())
    }

    @Test
    fun `buildFreezeFrameRequestWithPIDs should pad hex values correctly`() {
        // When
        val command = FreezeFrameParser.buildFreezeFrameRequestWithPIDs(
            0,
            0x01, // Single digit
            0x10  // Two digits
        )

        // Then
        assertEquals("02000110", command.lowercase())
    }

    // ========== getSummary Tests ==========

    @Test
    fun `getSummary should format available data correctly`() {
        // Given
        val response = "42 00 0C 1A 4F 0D 50 05 64 11 80 04 40"
        val result = FreezeFrameParser.parseFreezeFrame("P0300", response)

        // When
        val summary = result!!.getSummary()

        // Then
        assertTrue(summary.contains("RPM"))
        assertTrue(summary.contains("Speed"))
        assertTrue(summary.contains("Coolant"))
        assertTrue(summary.contains("Throttle"))
        assertTrue(summary.contains("Load"))
    }

    @Test
    fun `getSummary should handle missing data gracefully`() {
        // Given - Only RPM available
        val response = "42 00 0C 1A 4F"
        val result = FreezeFrameParser.parseFreezeFrame("P0300", response)

        // When
        val summary = result!!.getSummary()

        // Then
        assertTrue(summary.contains("RPM"))
        assertFalse(summary.contains("Speed"))
    }
}
