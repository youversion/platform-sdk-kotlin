package com.youversion.platform.reader.domain

import com.youversion.platform.core.domain.Storage
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserSettingsRepositoryTest {
    // ----- readerThemeId

    @Test
    fun `readerThemeId getter returns null when storage key is absent`() {
        val storage = mockk<Storage>()
        every { storage.getIntOrNull("bible-reader-view--theme") } returns null
        val repository = UserSettingsRepository(storage)

        assertNull(repository.readerThemeId)
    }

    @Test
    fun `readerThemeId getter returns stored int value`() {
        val storage = mockk<Storage>()
        every { storage.getIntOrNull("bible-reader-view--theme") } returns 42
        val repository = UserSettingsRepository(storage)

        assertEquals(42, repository.readerThemeId)
    }

    @Test
    fun `readerThemeId setter writes int value to storage`() {
        val storage = mockk<Storage>()
        every { storage.putInt("bible-reader-view--theme", 7) } just Runs
        val repository = UserSettingsRepository(storage)

        repository.readerThemeId = 7

        verify { storage.putInt("bible-reader-view--theme", 7) }
    }

    @Test
    fun `readerThemeId setter writes null to storage`() {
        val storage = mockk<Storage>()
        every { storage.putInt("bible-reader-view--theme", null) } just Runs
        val repository = UserSettingsRepository(storage)

        repository.readerThemeId = null

        verify { storage.putInt("bible-reader-view--theme", null) }
    }

    // ----- readerFontSize

    @Test
    fun `readerFontSize getter returns null when storage key is absent`() {
        val storage = mockk<Storage>()
        every { storage.getFloatOrNull("bible-reader-view--font-size") } returns null
        val repository = UserSettingsRepository(storage)

        assertNull(repository.readerFontSize)
    }

    @Test
    fun `readerFontSize getter returns stored float value`() {
        val storage = mockk<Storage>()
        every { storage.getFloatOrNull("bible-reader-view--font-size") } returns 18.5f
        val repository = UserSettingsRepository(storage)

        assertEquals(18.5f, repository.readerFontSize)
    }

    @Test
    fun `readerFontSize setter writes float value to storage`() {
        val storage = mockk<Storage>()
        every { storage.putFloat("bible-reader-view--font-size", 18.5f) } just Runs
        val repository = UserSettingsRepository(storage)

        repository.readerFontSize = 18.5f

        verify { storage.putFloat("bible-reader-view--font-size", 18.5f) }
    }

    @Test
    fun `readerFontSize setter writes null to storage`() {
        val storage = mockk<Storage>()
        every { storage.putFloat("bible-reader-view--font-size", null) } just Runs
        val repository = UserSettingsRepository(storage)

        repository.readerFontSize = null

        verify { storage.putFloat("bible-reader-view--font-size", null) }
    }

    // ----- readerFontFamilyName

    @Test
    fun `readerFontFamilyName getter returns null when storage key is absent`() {
        val storage = mockk<Storage>()
        every { storage.getStringOrNull("bible-reader-view--font-family-name") } returns null
        val repository = UserSettingsRepository(storage)

        assertNull(repository.readerFontFamilyName)
    }

    @Test
    fun `readerFontFamilyName getter returns stored string value`() {
        val storage = mockk<Storage>()
        every { storage.getStringOrNull("bible-reader-view--font-family-name") } returns "Roboto"
        val repository = UserSettingsRepository(storage)

        assertEquals("Roboto", repository.readerFontFamilyName)
    }

    @Test
    fun `readerFontFamilyName setter writes string value to storage`() {
        val storage = mockk<Storage>()
        every { storage.putString("bible-reader-view--font-family-name", "Roboto") } just Runs
        val repository = UserSettingsRepository(storage)

        repository.readerFontFamilyName = "Roboto"

        verify { storage.putString("bible-reader-view--font-family-name", "Roboto") }
    }

    @Test
    fun `readerFontFamilyName setter writes null to storage`() {
        val storage = mockk<Storage>()
        every { storage.putString("bible-reader-view--font-family-name", null) } just Runs
        val repository = UserSettingsRepository(storage)

        repository.readerFontFamilyName = null

        verify { storage.putString("bible-reader-view--font-family-name", null) }
    }
}
