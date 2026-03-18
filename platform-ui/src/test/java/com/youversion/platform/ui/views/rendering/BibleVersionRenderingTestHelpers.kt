package com.youversion.platform.ui.views.rendering

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.bibles.data.BibleVersionMemoryCache
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.ui.views.BibleTextFonts
import com.youversion.platform.ui.views.BibleTextFootnoteMode
import kotlin.test.assertNotNull

internal val RENDERING_TEST_FONTS = BibleTextFonts(fontFamily = FontFamily.Default, baseSize = 16.sp)

internal val FULL_CHAPTER_REF =
    BibleReference(
        versionId = 111,
        bookUSFM = "GEN",
        chapter = 1,
    )

internal val SIMPLE_VERSE_HTML =
    """
    <div>
        <div class="p">
            <span class="yv-v" v="1"></span>
            <span class="yv-vlbl">1</span>
            First verse text.
        </div>
    </div>
    """.trimIndent()

internal val FOOTNOTE_VERSE_HTML =
    """
    <div>
        <div class="p">
            <span class="yv-v" v="1"></span>
            Some text
            <span class="yv-n f"><span class="fr">1:1</span><span class="ft">footnote body</span></span>
            after footnote.
        </div>
    </div>
    """.trimIndent()

internal val THREE_VERSE_HTML =
    """
    <div>
        <div class="p">
            <span class="yv-v" v="1"></span>
            <span class="yv-vlbl">1</span>
            First.
            <span class="yv-v" v="2"></span>
            <span class="yv-vlbl">2</span>
            Second.
            <span class="yv-v" v="3"></span>
            <span class="yv-vlbl">3</span>
            Third.
        </div>
    </div>
    """.trimIndent()

internal suspend fun renderBlocks(
    html: String,
    reference: BibleReference,
    renderHeadlines: Boolean = true,
    renderVerseNumbers: Boolean = true,
    footnoteMode: BibleTextFootnoteMode = BibleTextFootnoteMode.NONE,
    footnoteMarker: AnnotatedString? = null,
    textColor: Color = Color.Black,
    wocColor: Color = Color.Red,
): List<BibleTextBlock> {
    val cache = BibleVersionMemoryCache()
    val chapterReference =
        BibleReference(
            versionId = reference.versionId,
            bookUSFM = reference.bookUSFM,
            chapter = reference.chapter,
        )
    cache.addChapterContents(html, chapterReference)

    val repository =
        BibleChapterRepository(
            memoryCache = cache,
            temporaryCache = BibleVersionMemoryCache(),
            persistentCache = BibleVersionMemoryCache(),
        )

    val blocks =
        BibleVersionRendering.textBlocks(
            bibleChapterRepository = repository,
            reference = reference,
            renderVerseNumbers = renderVerseNumbers,
            renderHeadlines = renderHeadlines,
            footnoteMode = footnoteMode,
            footnoteMarker = footnoteMarker,
            textColor = textColor,
            wocColor = wocColor,
            fonts = RENDERING_TEST_FONTS,
        )

    assertNotNull(blocks)
    return blocks
}

internal suspend fun renderBlocksNullable(
    html: String,
    reference: BibleReference,
): List<BibleTextBlock>? {
    val cache = BibleVersionMemoryCache()
    val chapterReference =
        BibleReference(
            versionId = reference.versionId,
            bookUSFM = reference.bookUSFM,
            chapter = reference.chapter,
        )
    cache.addChapterContents(html, chapterReference)

    val repository =
        BibleChapterRepository(
            memoryCache = cache,
            temporaryCache = BibleVersionMemoryCache(),
            persistentCache = BibleVersionMemoryCache(),
        )

    return BibleVersionRendering.textBlocks(
        bibleChapterRepository = repository,
        reference = reference,
        footnoteMode = BibleTextFootnoteMode.NONE,
        textColor = Color.Black,
        fonts = RENDERING_TEST_FONTS,
    )
}

internal fun List<BibleTextBlock>.hasHeaderContaining(text: String): Boolean =
    any { block ->
        val annotatedText = block.text
        val annotations =
            annotatedText.getStringAnnotations(
                tag = BibleTextCategoryAttribute.NAME,
                start = 0,
                end = annotatedText.length,
            )
        annotations.any { it.item == BibleTextCategory.HEADER.name } &&
            annotatedText.text.contains(text)
    }

internal fun List<BibleTextBlock>.hasScriptureContaining(text: String): Boolean =
    any { block -> block.text.text.contains(text) }
