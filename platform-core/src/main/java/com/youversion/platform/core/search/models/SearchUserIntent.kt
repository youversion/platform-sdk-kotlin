package com.youversion.platform.core.search.models

/**
 * What a reader appears to be looking for, used to rank results.
 *
 * The set is open: the platform may name an intent this version does not, and it arrives intact rather than failing
 * the search. Compare against the companion's intents and fall through on the rest.
 *
 * @property rawValue The intent as the platform spells it.
 */
data class SearchUserIntent(
    val rawValue: String,
) {
    companion object {
        /** A specific address in scripture, such as `John 3:16`. */
        val reference = SearchUserIntent("reference")

        /** Scripture containing the words the reader typed. */
        val text = SearchUserIntent("text")

        /** A subject scripture speaks to, such as anxiety. */
        val topical = SearchUserIntent("topical")

        /** Not known. */
        val unknown = SearchUserIntent("unknown")
    }
}
