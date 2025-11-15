package com.youversion.platform.core.utilities

/** A protocol for types that need to split abbreviations into letters and trailing numbers. */
interface AbbreviationSplitting {
    /**
     * Splits a text string into its letter prefix and trailing number suffix.
     * @param text The text to split (e.g., "ESV" or "1984")
     * @return A [Split] containing the letters and numbers portions
     */
    fun splitAbbreviation(text: String): Split {
        val pattern = Regex("^(.*?)(\\d+)$")
        val match = pattern.find(text)
        return if (match != null) {
            val (letters, numbers) = match.destructured
            Split(letters, numbers)
        } else {
            Split(text, "")
        }
    }

    data class Split(
        val letters: String,
        val numbers: String,
    )
}
