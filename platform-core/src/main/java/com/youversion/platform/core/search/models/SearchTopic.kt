package com.youversion.platform.core.search.models

/**
 * A named subject a reader can search for and that scripture speaks to, such as anxiety or forgiveness.
 *
 * @property id The platform's identifier for the topic, when it names one.
 * @property text The topic as it should be shown to a reader.
 * @property subtopics Narrower subjects within the topic, offered so a reader can narrow a broad one.
 */
data class SearchTopic(
    val id: Int?,
    val text: String,
    val subtopics: List<String>,
)
