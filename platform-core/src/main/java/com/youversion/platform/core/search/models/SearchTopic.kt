package com.youversion.platform.core.search.models

import com.youversion.platform.core.di.PlatformInternalApi

/**
 * A subject scripture speaks to, such as anxiety or forgiveness.
 *
 * @property id The platform's identifier, when it names one.
 * @property text The topic as shown to a reader.
 * @property subtopics Narrower subjects within the topic.
 */
public class SearchTopic
    @PlatformInternalApi
    constructor(
        public val id: Int?,
        public val text: String,
        public val subtopics: List<String>,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SearchTopic) return false
            return id == other.id &&
                text == other.text &&
                subtopics == other.subtopics
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + text.hashCode()
            result = 31 * result + subtopics.hashCode()
            return result
        }

        override fun toString(): String = "SearchTopic(id=$id, text=$text, subtopics=$subtopics)"
    }
