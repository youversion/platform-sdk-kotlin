package com.youversion.platform.core.bibles.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BiblePassage(
    @SerialName(CodingKey.ID) val id: String,
    @SerialName(CodingKey.CONTENT) val content: String,
    @SerialName(CodingKey.BIBLE_ID) val bibleId: Int,
    @SerialName(CodingKey.HUMAN_REFERENCE) val humanReference: String,
) {
    private object CodingKey {
        const val ID = "id"
        const val CONTENT = "content"
        const val BIBLE_ID = "bible_id"
        const val HUMAN_REFERENCE = "human_reference"
    }

    companion object {
        val preview =
            BiblePassage(
                id = "JHN.3.1",
                content =
                    """
                    <div><div class=\"p\"><span class=\"yv-v\" v=\"1\"></span><span class=\"yv-vlbl\">1</span>Now there was a man of the Pharisees named Nicodemus, a ruler of the Jews. </div></div>
                    """.trimIndent(),
                bibleId = 206,
                humanReference = "John 3:1",
            )
    }
}
