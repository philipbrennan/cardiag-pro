package com.cardiag.pro.data.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ResultTest {

    @Test
    fun `success - isSuccess returns true`() {
        val result = Result.Success("test")
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
    }

    @Test
    fun `error - isError returns true`() {
        val result = Result.Error(Exception("test"))
        assertTrue(result.isError)
        assertFalse(result.isSuccess)
    }

    @Test
    fun `getOrNull - success - returns data`() {
        val result = Result.Success("test")
        assertEquals("test", result.getOrNull())
    }

    @Test
    fun `getOrNull - error - returns null`() {
        val result = Result.Error(Exception("test"))
        assertNull(result.getOrNull())
    }

    @Test
    fun `getOrThrow - success - returns data`() {
        val result = Result.Success("test")
        assertEquals("test", result.getOrThrow())
    }

    @Test
    fun `getOrThrow - error - throws exception`() {
        val exception = Exception("test")
        val result = Result.Error(exception)

        val thrown = assertThrows(Exception::class.java) {
            result.getOrThrow()
        }
        assertEquals(exception, thrown)
    }

    @Test
    fun `onSuccess - success - executes action`() {
        var executed = false
        val result = Result.Success("test")

        result.onSuccess { executed = true }

        assertTrue(executed)
    }

    @Test
    fun `onSuccess - error - does not execute action`() {
        var executed = false
        val result = Result.Error(Exception("test"))

        result.onSuccess { executed = true }

        assertFalse(executed)
    }

    @Test
    fun `onError - error - executes action`() {
        var executed = false
        val result = Result.Error(Exception("test"))

        result.onError { executed = true }

        assertTrue(executed)
    }

    @Test
    fun `onError - success - does not execute action`() {
        var executed = false
        val result = Result.Success("test")

        result.onError { executed = true }

        assertFalse(executed)
    }

    @Test
    fun `chaining - onSuccess and onError`() {
        var successCount = 0
        var errorCount = 0

        Result.Success("test")
            .onSuccess { successCount++ }
            .onError { errorCount++ }

        assertEquals(1, successCount)
        assertEquals(0, errorCount)

        Result.Error(Exception("test"))
            .onSuccess { successCount++ }
            .onError { errorCount++ }

        assertEquals(1, successCount)
        assertEquals(1, errorCount)
    }
}
