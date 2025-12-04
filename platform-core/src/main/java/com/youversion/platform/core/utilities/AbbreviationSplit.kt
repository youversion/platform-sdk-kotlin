package com.youversion.platform.core.utilities

/**
 * Represents an BibleVersion abbreviation that was split into an alphabetical
 * and numeric pieces. For example, "NIV1984" would split into "NIV" letters
 * and "1984" numbers.
 */
data class AbbreviationSplit(
    val letters: String,
    val numbers: String,
)

/**
 * Splits a text string into its letter prefix and trailing number suffix.
 * @param text The text to split (e.g., "ESV" or "1984")
 * @return A [AbbreviationSplit] containing the letters and numbers portions
 */
fun splitAbbreviation(text: String): AbbreviationSplit {
    val pattern = Regex("^(.*?)(\\d+)$")
    val match = pattern.find(text)
    return if (match != null) {
        val (letters, numbers) = match.destructured
        AbbreviationSplit(letters, numbers)
    } else {
        AbbreviationSplit(text, "")
    }
}
