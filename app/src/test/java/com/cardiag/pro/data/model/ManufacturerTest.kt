package com.cardiag.pro.data.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Unit tests for Manufacturer enum and VIN decoding.
 */
class ManufacturerTest {

    // ========== BMW VIN Tests ==========

    @ParameterizedTest
    @CsvSource(
        "WBADT43452G123456",  // BMW 3-series
        "WBA1234567890123",   // BMW WBA prefix
        "WBS1234567890123",   // BMW WBS prefix
        "WBY1234567890123",   // BMW WBY prefix
        "4US1234567890123",   // BMW 4US prefix (US production)
        "5UX1234567890123"    // BMW 5UX prefix (X-series)
    )
    fun `fromVin should identify BMW VINs correctly`(vin: String) {
        // When
        val manufacturer = Manufacturer.fromVin(vin)

        // Then
        assertEquals(Manufacturer.BMW, manufacturer)
    }

    // ========== Volkswagen VIN Tests ==========

    @ParameterizedTest
    @CsvSource(
        "WVWZZZ1KZ1W123456",  // VW Golf
        "WVW1234567890123",   // VW WVW prefix
        "WV11234567890123",   // VW WV1 prefix
        "WV21234567890123",   // VW WV2 prefix
        "3VW1234567890123"    // VW 3VW prefix (Mexico production)
    )
    fun `fromVin should identify Volkswagen VINs correctly`(vin: String) {
        // When
        val manufacturer = Manufacturer.fromVin(vin)

        // Then
        assertEquals(Manufacturer.VOLKSWAGEN, manufacturer)
    }

    // ========== Nissan VIN Tests ==========

    @ParameterizedTest
    @CsvSource(
        "JN1AB1234CD567890",  // Nissan Japan
        "JN11234567890123",   // Nissan JN1 prefix
        "JN81234567890123",   // Nissan JN8 prefix
        "1N41234567890123",   // Nissan 1N4 prefix (US)
        "1N61234567890123"    // Nissan 1N6 prefix (US trucks)
    )
    fun `fromVin should identify Nissan VINs correctly`(vin: String) {
        // When
        val manufacturer = Manufacturer.fromVin(vin)

        // Then
        assertEquals(Manufacturer.NISSAN, manufacturer)
    }

    // ========== Unknown Manufacturer Tests ==========

    @ParameterizedTest
    @CsvSource(
        "1HGBH41JXMN109186",  // Honda
        "1G1ZT53826F109149",  // Chevrolet
        "JT2BG22K1X0123456",  // Toyota
        "KMHCT4AE0CU123456",  // Hyundai
        "5YJSA1E14EF123456",  // Tesla
        "WAUEF78E57A123456"   // Audi (not explicitly handled)
    )
    fun `fromVin should return UNKNOWN for unsupported manufacturers`(vin: String) {
        // When
        val manufacturer = Manufacturer.fromVin(vin)

        // Then
        assertEquals(Manufacturer.UNKNOWN, manufacturer)
    }

    // ========== Edge Cases ==========

    @Test
    fun `fromVin should return UNKNOWN for VIN shorter than 3 characters`() {
        // When
        val result1 = Manufacturer.fromVin("")
        val result2 = Manufacturer.fromVin("AB")
        val result3 = Manufacturer.fromVin("W")

        // Then
        assertEquals(Manufacturer.UNKNOWN, result1)
        assertEquals(Manufacturer.UNKNOWN, result2)
        assertEquals(Manufacturer.UNKNOWN, result3)
    }

    @Test
    fun `fromVin should handle lowercase VINs correctly`() {
        // Given - lowercase VIN
        val vin = "wbadt43452g123456"

        // When
        val manufacturer = Manufacturer.fromVin(vin)

        // Then
        assertEquals(Manufacturer.BMW, manufacturer)
    }

    @Test
    fun `fromVin should handle mixed case VINs correctly`() {
        // Given - mixed case VIN
        val vin = "WbAdT43452g123456"

        // When
        val manufacturer = Manufacturer.fromVin(vin)

        // Then
        assertEquals(Manufacturer.BMW, manufacturer)
    }

    // ========== Display Name Tests ==========

    @Test
    fun `display names should be user-friendly`() {
        assertEquals("BMW", Manufacturer.BMW.displayName)
        assertEquals("Volkswagen", Manufacturer.VOLKSWAGEN.displayName)
        assertEquals("Nissan", Manufacturer.NISSAN.displayName)
        assertEquals("Unknown", Manufacturer.UNKNOWN.displayName)
    }

    // ========== All Values Test ==========

    @Test
    fun `all manufacturers should be accessible`() {
        // Given
        val manufacturers = Manufacturer.entries

        // Then
        assertEquals(4, manufacturers.size)
        assertTrue(manufacturers.contains(Manufacturer.BMW))
        assertTrue(manufacturers.contains(Manufacturer.VOLKSWAGEN))
        assertTrue(manufacturers.contains(Manufacturer.NISSAN))
        assertTrue(manufacturers.contains(Manufacturer.UNKNOWN))
    }

    // ========== VIN Format Validation Context ==========

    @Test
    fun `fromVin should work with standard 17-character VINs`() {
        // Given - exactly 17 characters
        val validVins = listOf(
            "WBADT4345G2123456",
            "WVW12345678901234",
            "JN1AB1234CD567890"
        )

        // When & Then
        validVins.forEach { vin ->
            assertEquals(17, vin.length)
            assertNotEquals(Manufacturer.UNKNOWN, Manufacturer.fromVin(vin))
        }
    }
}
