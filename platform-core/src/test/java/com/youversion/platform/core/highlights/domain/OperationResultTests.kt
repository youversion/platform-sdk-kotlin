package com.youversion.platform.core.highlights.domain

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class OperationResultTests {
    private val id = UUID(0L, 1L)
    private val failure = IllegalStateException("failure")

    private fun result(
        operationId: UUID = id,
        isSuccess: Boolean = true,
        error: Throwable? = failure,
        retryCount: Int = 1,
    ) = OperationResult(operationId = operationId, isSuccess = isSuccess, error = error, retryCount = retryCount)

    @Test
    fun `test values with the same fields are equal and hash alike`() {
        assertEquals(result(), result())
        assertEquals(result().hashCode(), result().hashCode())
    }

    @Test
    fun `test a difference in any field makes values unequal`() {
        listOf(
            result(operationId = UUID(0L, 2L)),
            result(isSuccess = false),
            result(error = null),
            result(retryCount = 2),
        ).forEach {
            assertNotEquals(result(), it)
            assertNotEquals(result().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test errors with the same message are different errors`() {
        assertNotEquals(result(), result(error = IllegalStateException("failure")))
    }

    @Test
    fun `test toString matches the existing format`() {
        assertEquals(
            "OperationResult(operationId=$id, isSuccess=true, " +
                "error=java.lang.IllegalStateException: failure, retryCount=1)",
            result().toString(),
        )
    }
}
