package com.youversion.platform.core.search.models

/**
 * What a reader appears to be looking for, used to rank results and returned as the intent the platform inferred.
 *
 * The set of intents is open: the platform may name one this version of the SDK does not, and such a value arrives
 * intact rather than failing the search. Compare against the named intents on the companion and fall through on the
 * rest.
 *
 * @property rawValue The intent as the platform spells it.
 */
data class SearchUserIntent(
    val rawValue: String,
) {
    companion object {
        /** The reader is looking for a specific address in scripture, such as `John 3:16`. */
        val reference = SearchUserIntent("reference")

        /** The reader is looking for scripture containing the words they typed. */
        val text = SearchUserIntent("text")

        /** The reader is looking for a subject scripture speaks to, such as anxiety. */
        val topical = SearchUserIntent("topical")

        /** The kind of thing the reader is looking for is not known. */
        val unknown = SearchUserIntent("unknown")
    }
}
