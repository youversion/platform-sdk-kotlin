package com.youversion.platform.core.bibles.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BiblePassage(
    @SerialName(CodingKey.ID) val id: String,
    @SerialName(CodingKey.CONTENT) val content: String,
    @SerialName(CodingKey.REFERENCE) val reference: String,
) {
    private object CodingKey {
        const val ID = "id"
        const val CONTENT = "content"
        const val REFERENCE = "reference"
    }

    companion object {
        val preview =
            BiblePassage(
                id = "JHN.3.1",
                content =
                    """
                    <div><div class=\"p\"><span class=\"yv-v\" v=\"1\"></span><span class=\"yv-vlbl\">1</span>Now there was a man of the Pharisees named Nicodemus, a ruler of the Jews. </div></div>
                    """.trimIndent(),
                reference = "John 3:1",
            )
    }
}
