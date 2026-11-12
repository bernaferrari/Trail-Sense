package com.kylecorry.trail_sense.shared.text.nlp.tokenizers

/**
 * Splits text into words in any script: letters, combining marks, numbers and apostrophes. Preserved words (which may
 * contain spaces) are kept together when they appear as whole words.
 */
class SimpleWordTokenizer(preservedWords: Set<String> = emptySet()) : Tokenizer {

    private val preservedTokenMap = preservedWords
        .filter { it.isNotEmpty() }
        .mapIndexed { index, string ->
            "${PRESERVED_TOKEN_PREFIX}$index" to string
        }.toMap()

    private val preservedTokens = preservedTokenMap.entries
        .sortedByDescending { it.value.length }

    override fun tokenize(text: String): List<String> {
        var preservedText = text
        for ((token, word) in preservedTokens) {
            preservedText = replaceWholeWord(preservedText, word, token)
        }

        val tokens = split(preservedText)
        if (preservedTokenMap.isEmpty()) {
            return tokens
        }

        return tokens.map { preservedTokenMap[it] ?: it }
    }

    companion object {
        private const val PRESERVED_TOKEN_PREFIX = "PRESERVED00000"

        // Splits on anything that is not a letter, combining mark, number or apostrophe. This is done by hand rather
        // than with a \p{L} character class because Kotlin/Native's regex mishandles literals in such a class.
        private fun split(text: String): List<String> {
            val tokens = mutableListOf<String>()
            var start = -1
            for (i in text.indices) {
                if (text[i].isTokenCharacter()) {
                    if (start == -1) start = i
                } else if (start != -1) {
                    tokens.add(text.substring(start, i))
                    start = -1
                }
            }
            if (start != -1) {
                tokens.add(text.substring(start))
            }
            return tokens
                .map { it.trim('\u0027', '’').trimStart { character -> character.isCombiningMark() } }
                .filter { token -> token.any { character -> character.isLetterOrDigit() } }
        }

        private fun Char.isTokenCharacter(): Boolean {
            return isLetter() || isCombiningMark() || category in NUMBER_CATEGORIES || this == '\u0027' || this == '’'
        }

        private val NUMBER_CATEGORIES = setOf(
            CharCategory.DECIMAL_DIGIT_NUMBER,
            CharCategory.LETTER_NUMBER,
            CharCategory.OTHER_NUMBER
        )

        private fun replaceWholeWord(text: String, word: String, replacement: String): String {
            var searchStart = 0
            var copyStart = 0
            var result: StringBuilder? = null

            while (searchStart < text.length) {
                val index = text.indexOf(word, searchStart, ignoreCase = true)
                if (index == -1) {
                    break
                }

                val endIndex = index + word.length
                val isStartBoundary = index == 0 || !text[index - 1].isWordCharacter()
                val isEndBoundary = endIndex == text.length || !text[endIndex].isWordCharacter()
                if (isStartBoundary && isEndBoundary) {
                    val builder = result ?: StringBuilder(text.length).also { result = it }
                    builder.append(text, copyStart, index)
                    builder.append(' ').append(replacement).append(' ')
                    copyStart = endIndex
                    searchStart = endIndex
                } else {
                    searchStart = index + 1
                }
            }

            return result?.append(text, copyStart, text.length)?.toString() ?: text
        }

        private fun Char.isWordCharacter(): Boolean {
            return isLetterOrDigit() || isCombiningMark()
        }

        private fun Char.isCombiningMark(): Boolean {
            return when (category) {
                CharCategory.NON_SPACING_MARK,
                CharCategory.COMBINING_SPACING_MARK,
                CharCategory.ENCLOSING_MARK -> true

                else -> false
            }
        }
    }
}
