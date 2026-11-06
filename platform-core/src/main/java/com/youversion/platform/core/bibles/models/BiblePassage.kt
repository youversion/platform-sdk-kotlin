package com.youversion.platform.core.bibles.models

import com.youversion.platform.core.di.PlatformInternalApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public class BiblePassage
    @PlatformInternalApi
    constructor(
        @SerialName(CodingKey.ID) public val id: String,
        @SerialName(CodingKey.CONTENT) public val content: String,
        @SerialName(CodingKey.REFERENCE) public val reference: String,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BiblePassage) return false
            return id == other.id &&
                content == other.content &&
                reference == other.reference
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + content.hashCode()
            result = 31 * result + reference.hashCode()
            return result
        }

        override fun toString(): String = "BiblePassage(id=$id, content=$content, reference=$reference)"

        private object CodingKey {
            const val ID = "id"
            const val CONTENT = "content"
            const val REFERENCE = "reference"
        }

        public companion object {
            public val preview: BiblePassage =
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
