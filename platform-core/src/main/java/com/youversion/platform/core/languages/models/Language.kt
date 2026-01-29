package com.youversion.platform.core.languages.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Language(
    @SerialName(CodingKey.ID) val id: String? = null,
    @SerialName(CodingKey.LANGUAGE) val language: String? = null,
    @SerialName(CodingKey.SCRIPT) val script: String? = null,
    @SerialName(CodingKey.SCRIPT_NAME) val scriptName: String? = null,
    @SerialName(CodingKey.ALIASES) val aliases: List<String>? = null,
    @SerialName(CodingKey.DISPLAY_NAMES) val displayNames: Map<String, String>? = null,
    @SerialName(CodingKey.SCRIPTS) val scripts: List<String>? = null,
    @SerialName(CodingKey.VARIANTS) val variants: List<String>? = null,
    @SerialName(CodingKey.COUNTRIES) val countries: List<String>? = null,
    @SerialName(CodingKey.TEXT_DIRECTION) val textDirection: String = "ltr",
    @SerialName(CodingKey.DEFAULT_BIBLE_VERSION_ID) val defaultBibleVersionId: Int? = null,
) {
    object CodingKey {
        const val ID = "id"
        const val LANGUAGE = "language"
        const val SCRIPT = "script"
        const val SCRIPT_NAME = "script_name"
        const val ALIASES = "aliases"
        const val DISPLAY_NAMES = "display_names"
        const val SCRIPTS = "scripts"
        const val VARIANTS = "variants"
        const val COUNTRIES = "countries"
        const val TEXT_DIRECTION = "text_direction"
        const val DEFAULT_BIBLE_VERSION_ID = "default_bible_version_id"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Language) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
