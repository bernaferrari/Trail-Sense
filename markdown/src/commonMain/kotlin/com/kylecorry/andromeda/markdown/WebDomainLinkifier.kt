package com.kylecorry.andromeda.markdown

/**
 * Finds bare web addresses without `www.` or a scheme, like Linkify: `example.com`, `sub.example.org/path?q=1`,
 * `example.co.uk:8080`. The top level domain must be a well-known one written in lower case, so file names
 * (`photo.jpg`, `notes.md`), decimals, abbreviations (`e.g.`) and sentences missing a space (`end.It`) are not
 * links. Addresses that are part of an email or another word are skipped, and trailing punctuation is left out.
 */
public object WebDomainLinkifier {

    /** A web address at [range] in the text, opened with [url]. */
    public data class Match(val range: IntRange, val url: String)

    public fun find(text: CharSequence): List<Match> {
        return CANDIDATE.findAll(text).mapNotNull { candidate ->
            val tld = candidate.groupValues[1]
            if (tld !in TOP_LEVEL_DOMAINS) return@mapNotNull null
            val value = trimTrailingPunctuation(candidate.value)
            val range = candidate.range.first until candidate.range.first + value.length
            Match(range, "http://$value")
        }.toList()
    }

    /** Drops sentence punctuation after an address, and a closing parenthesis that has no opening one in it. */
    private fun trimTrailingPunctuation(value: String): String {
        var end = value.length
        while (end > 0) {
            val last = value[end - 1]
            val unbalanced = last == ')' && value.take(end).count { it == '(' } < value.take(end).count { it == ')' }
            if (last in TRAILING_PUNCTUATION || unbalanced) end-- else break
        }
        return value.substring(0, end)
    }

    private const val TRAILING_PUNCTUATION = ".,;:!?'\"*_~"

    private val CANDIDATE = Regex(
        """(?<![\p{L}\p{N}@._%+\-/:])""" +
            """(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+([a-z]{2,24})""" +
            """(?![\p{L}\p{N}_@-])""" +
            """(?::\d{1,5})?""" +
            """(?:[/?#][^\s<>]*)?"""
    )

    /** Generic and country top level domains common in web addresses, leaving out ones that read as file types. */
    private val TOP_LEVEL_DOMAINS = setOf(
        // Generic
        "com", "org", "net", "edu", "gov", "mil", "int", "info", "biz", "name", "pro", "mobi", "app", "dev", "io",
        "ai", "co", "me", "tv", "xyz", "online", "site", "tech", "store", "blog", "cloud", "page", "news", "live",
        "shop", "wiki", "club", "space", "website", "top", "earth", "travel", "museum", "aero", "coop", "eu", "asia",
        // Countries
        "us", "uk", "ca", "au", "nz", "de", "fr", "es", "it", "nl", "be", "ch", "at", "se", "no", "dk", "fi", "is",
        "ie", "pl", "pt", "br", "mx", "ar", "cl", "pe", "uy", "ve", "jp", "cn", "kr", "in", "ru", "ua", "za", "tw",
        "hk", "sg", "my", "id", "ph", "th", "vn", "tr", "gr", "cz", "sk", "hu", "ro", "bg", "hr", "si", "lt", "lv",
        "ee", "il", "ae", "sa", "eg", "ng", "ke", "nu", "ly", "gl", "aq"
    )
}
