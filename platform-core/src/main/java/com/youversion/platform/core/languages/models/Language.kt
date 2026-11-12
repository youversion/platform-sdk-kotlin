package com.youversion.platform.core.languages.models

import com.youversion.platform.core.di.PlatformInternalApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public class Language
    @PlatformInternalApi
    constructor(
        @SerialName(CodingKey.ID) public val id: String? = null,
        @SerialName(CodingKey.LANGUAGE) public val language: String? = null,
        @SerialName(CodingKey.SCRIPT) public val script: String? = null,
        @SerialName(CodingKey.SCRIPT_NAME) public val scriptName: String? = null,
        @SerialName(CodingKey.ALIASES) public val aliases: List<String>? = null,
        @SerialName(CodingKey.DISPLAY_NAMES) public val displayNames: Map<String, String>? = null,
        @SerialName(CodingKey.SCRIPTS) public val scripts: List<String>? = null,
        @SerialName(CodingKey.VARIANTS) public val variants: List<String>? = null,
        @SerialName(CodingKey.COUNTRIES) public val countries: List<String>? = null,
        @SerialName(CodingKey.TEXT_DIRECTION) public val textDirection: String = "ltr",
        @SerialName(CodingKey.DEFAULT_BIBLE_VERSION_ID) public val defaultBibleVersionId: Int? = null,
    ) {
        public object CodingKey {
            public const val ID: String = "id"
            public const val LANGUAGE: String = "language"
            public const val SCRIPT: String = "script"
            public const val SCRIPT_NAME: String = "script_name"
            public const val ALIASES: String = "aliases"
            public const val DISPLAY_NAMES: String = "display_names"
            public const val SCRIPTS: String = "scripts"
            public const val VARIANTS: String = "variants"
            public const val COUNTRIES: String = "countries"
            public const val TEXT_DIRECTION: String = "text_direction"
            public const val DEFAULT_BIBLE_VERSION_ID: String = "default_bible_version_id"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Language) return false
            return id == other.id
        }

        override fun hashCode(): Int = id.hashCode()

        override fun toString(): String =
            "Language(id=$id, language=$language, script=$script, scriptName=$scriptName, aliases=$aliases, " +
                "displayNames=$displayNames, scripts=$scripts, variants=$variants, countries=$countries, " +
                "textDirection=$textDirection, defaultBibleVersionId=$defaultBibleVersionId)"
    }
