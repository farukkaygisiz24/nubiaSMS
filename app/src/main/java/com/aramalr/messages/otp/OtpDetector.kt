package com.aramalr.messages.otp

object OtpDetector {
    private val keywords =
        listOf(
            "doğrulama",
            "dogrulama",
            "verification",
            "kodunuz",
            "şifre",
            "sifre",
            "otp",
            "code",
            "kod",
        )

    private val codeRegex = Regex("""(?<![0-9.,])(\d{4,8})(?![0-9])""")

    fun detect(body: String): OtpMatch? {
        if (body.isBlank()) return null
        val normalized = body.lowercase()
        val keywordHits =
            keywords.mapNotNull { kw ->
                val idx = normalized.indexOf(kw)
                if (idx >= 0) idx to kw.length else null
            }
        if (keywordHits.isEmpty()) return null

        val candidates =
            codeRegex.findAll(body).mapNotNull { match ->
                val code = match.groupValues[1]
                if (looksLikeAmountContext(body, match.range.first)) return@mapNotNull null
                val distance =
                    keywordHits.minOf { (kwStart, kwLen) ->
                        val kwEnd = kwStart + kwLen
                        when {
                            match.range.first >= kwEnd -> match.range.first - kwEnd
                            match.range.last < kwStart -> kwStart - match.range.last
                            else -> 0
                        }
                    }
                Triple(code, match.range, distance)
            }.toList()

        if (candidates.isEmpty()) return null

        val best = candidates.minWith(compareBy<Triple<String, IntRange, Int>> { it.third }.thenByDescending { it.first.length })
        val confidence = (1f - (best.third / 80f).coerceIn(0f, 0.9f)).coerceIn(0.1f, 1f)
        return OtpMatch(
            code = best.first,
            startIndex = best.second.first,
            endIndex = best.second.last + 1,
            confidence = confidence,
        )
    }

    private fun looksLikeAmountContext(body: String, start: Int): Boolean {
        val before = body.substring(0, start)
        if (before.endsWith(".") || before.endsWith(",")) return true
        val windowStart = (start - 12).coerceAtLeast(0)
        val window = body.substring(windowStart, (start + 12).coerceAtMost(body.length)).lowercase()
        return window.contains("tl") || window.contains("try") || window.contains("odeme") || window.contains("ödeme")
    }
}
