package com.youversion.platform.core.api

import io.ktor.http.ContentType
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class FailureExceptionTests {
    @Test
    fun `reads the message field`() {
        val body = ErrorBody.parse("""{"message":"Bible not found"}""", ContentType.Application.Json)
        assertEquals("Bible not found", body.message)
    }

    @Test
    fun `reads a gateway fault string`() {
        val body =
            ErrorBody.parse(
                """{"fault":{"faultstring":"Quota violation","detail":{"errorcode":"QuotaViolation"}}}""",
                ContentType.Application.Json,
            )
        assertEquals("Quota violation", body.message)
    }

    @Test
    fun `reads a string detail`() {
        val body = ErrorBody.parse("""{"detail":"Not Found"}""", ContentType.Application.Json)
        assertEquals("Not Found", body.message)
    }

    @Test
    fun `reads validation issues and uses the first as the message`() {
        val body =
            ErrorBody.parse(
                """{"detail":[{"loc":["query","language_ranges",0],"msg":"invalid tag"},{"loc":["query"]}]}""",
                ContentType.Application.Json,
            )
        assertEquals(1, body.validationIssues.size)
        assertEquals(listOf("query", "language_ranges", "0"), body.validationIssues[0].location)
        assertEquals("invalid tag", body.validationIssues[0].message)
        assertEquals("invalid tag", body.message)
    }

    @Test
    fun `skips null locations`() {
        val body =
            ErrorBody.parse(
                """{"detail":[{"loc":["query",null,"tag"],"msg":"invalid tag"}]}""",
                ContentType.Application.Json,
            )
        assertEquals(listOf("query", "tag"), body.validationIssues[0].location)
        assertEquals("invalid tag", body.message)
    }

    @Test
    fun `treats a blank message as missing`() {
        val body = ErrorBody.parse("""{"message":"  ","detail":"Not Found"}""", ContentType.Application.Json)
        assertEquals("Not Found", body.message)
    }

    @Test
    fun `caps the message length`() {
        val body = ErrorBody.parse("x".repeat(5_000), ContentType.Text.Plain)
        assertEquals(1_000, body.message?.length)
    }

    @Test
    fun `reads a plain text body`() {
        val body = ErrorBody.parse("  Too many requests \n", ContentType.Text.Plain)
        assertEquals("Too many requests", body.message)
    }

    @Test
    fun `ignores an unrecognized body`() {
        listOf(
            "<html>Bad Gateway</html>" to ContentType.Text.Html,
            "" to ContentType.Text.Plain,
            """{"message":42}""" to ContentType.Application.Json,
        ).forEach { (text, type) ->
            val body = ErrorBody.parse(text, type)
            assertNull(body.message, text)
            assertEquals(emptyList(), body.validationIssues)
        }
    }

    @Test
    fun `reads retry after as seconds`() {
        assertEquals(30.seconds, retryAfter("30"))
        assertEquals(Duration.ZERO, retryAfter("-5"))
    }

    @Test
    fun `reads retry after as an http date`() {
        val now = Instant.parse("2015-10-21T07:28:00Z").toEpochMilli()
        assertEquals(120.seconds, retryAfter("Wed, 21 Oct 2015 07:30:00 GMT", nowMillis = now))
        assertEquals(Duration.ZERO, retryAfter("Wed, 21 Oct 2015 07:00:00 GMT", nowMillis = now))
    }

    @Test
    fun `ignores a missing or malformed retry after`() {
        listOf(null, "", "soon", "1.5").forEach { assertNull(retryAfter(it), it) }
    }
}
