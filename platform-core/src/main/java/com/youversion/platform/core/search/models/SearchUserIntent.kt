package com.youversion.platform.core.search.models

/**
 * What a reader appears to be looking for, used to rank results.
 *
 * The set is open: the platform may name an intent this version does not, and it arrives intact rather than failing
 * the search. Compare against the companion's intents and fall through on the rest.
 *
 * @property rawValue The intent as the platform spells it.
 */
public class SearchUserIntent(
    public val rawValue: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SearchUserIntent) return false
        return rawValue == other.rawValue
    }

    override fun hashCode(): Int = rawValue.hashCode()

    override fun toString(): String = "SearchUserIntent(rawValue=$rawValue)"

    public companion object {
        /** A specific address in scripture, such as `John 3:16`. */
        public val reference: SearchUserIntent = SearchUserIntent("reference")

        /** Scripture containing the words the reader typed. */
        public val text: SearchUserIntent = SearchUserIntent("text")

        /** A subject scripture speaks to, such as anxiety. */
        public val topical: SearchUserIntent = SearchUserIntent("topical")

        /** Not known. */
        public val unknown: SearchUserIntent = SearchUserIntent("unknown")
    }
}
