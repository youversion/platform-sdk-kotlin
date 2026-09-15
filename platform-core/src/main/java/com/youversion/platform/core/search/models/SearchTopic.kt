package com.youversion.platform.core.search.models

/**
 * A subject scripture speaks to, such as anxiety or forgiveness.
 *
 * @property id The platform's identifier, when it names one.
 * @property text The topic as shown to a reader.
 * @property subtopics Narrower subjects within the topic.
 */
data class SearchTopic(
    val id: Int?,
    val text: String,
    val subtopics: List<String>,
)
