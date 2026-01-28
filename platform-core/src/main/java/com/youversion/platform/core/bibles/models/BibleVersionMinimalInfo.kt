package com.youversion.platform.core.bibles.models

import kotlinx.serialization.Serializable

@Serializable
data class BibleVersionMinimalInfo(
    val id: Int,
    val languageTag: String?,
) {
    private object CodingKey {
        const val ID = "id"
        const val LANGUAGE_TAG = "abbreviation"
    }
}
