package com.youversion.platform.core.votd.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YouVersionVerseOfTheDay(
    @SerialName(CodingKey.DAY) val day: Int,
    @SerialName(CodingKey.PASSAGE_ID) val passageUsfm: String,
) {
    object CodingKey {
        const val DAY = "day"
        const val PASSAGE_ID = "passage_id"
    }

    companion object {
        val preview =
            YouVersionVerseOfTheDay(
                day = 1,
                passageUsfm = "ISA.43.19",
            )
    }
}
