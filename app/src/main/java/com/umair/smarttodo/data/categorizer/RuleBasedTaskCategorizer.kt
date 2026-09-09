package com.umair.smarttodo.data.categorizer

import com.umair.smarttodo.domain.Category
import com.umair.smarttodo.domain.TaskCategorizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline, deterministic [TaskCategorizer] driven by the weighted keyword tables in
 * [CategoryKeywords].
 *
 * ### How a decision is made
 * 1. **Normalise.** [TextNormalizer.tokenize] lowercases, drops punctuation and emoji,
 *    collapses whitespace and folds each token so that the many Roman-script spellings of a
 *    word converge (`jaana`/`jana`, `dawaai`/`davai`, `naukri`/`nokri`).
 * 2. **Match whole tokens.** Every 1..[CategoryKeywords.MAX_NGRAM] window of consecutive
 *    tokens is looked up in the keyword index. Matching is never substring-based, so `gym`
 *    cannot fire inside `gymkhana` and `pr` cannot fire inside `prepare`.
 * 3. **Score.** Each *distinct* matched keyword adds its weight to its category. Distinct is
 *    the operative word: repeating "bill bill bill" scores exactly the same as "bill", which
 *    keeps very long or spammy input from skewing the result.
 * 4. **Pick.** Highest total wins. A tie is broken by [CategoryKeywords.PRECEDENCE], an
 *    explicit ordered list, never by map iteration order. A total of zero yields
 *    [Category.OTHER].
 *
 * ### Purchase-intent phrases
 * Some objects are topical in one category but become a shopping errand in context.
 * `ammi ke liye dawai leni hai` mentions medicine, yet the actionable part is "go and buy" -
 * the user will look for it under Shopping, next to the vegetables and the milk, not under
 * Health. The `buy medicine` phrase rule therefore carries [CategoryKeywords.PHRASE] weight
 * so it outranks both the medicine concept (Health) and the family concept (Personal).
 * `dawai khatam ho gayi hai` with no acquisition verb still resolves to Health.
 *
 * ### Guarantees
 * Pure, stateless, thread-safe and allocation-bounded by the input length. It never throws:
 * any unexpected failure degrades to [Category.OTHER].
 *
 * ### Deliberate non-goals
 * No translation and no network access. `normalizedEnglishText` stays `null`; a later
 * enrichment layer owns that field.
 */
@Singleton
class RuleBasedTaskCategorizer @Inject constructor() : TaskCategorizer {

    override fun categorize(rawText: String): Category = runCatching { classify(rawText) }
        .getOrDefault(Category.OTHER)

    private fun classify(rawText: String): Category {
        val tokens = TextNormalizer.tokenize(rawText)
        if (tokens.isEmpty()) return Category.OTHER

        val matchedKeys = collectMatches(tokens)
        if (matchedKeys.isEmpty()) return Category.OTHER

        val scores = IntArray(Category.entries.size)
        for (key in matchedKeys) {
            val votes = CategoryKeywords.INDEX[key] ?: continue
            for (vote in votes) {
                scores[vote.category.ordinal] += vote.weight
            }
        }
        return highestScoring(scores)
    }

    /**
     * Returns every distinct keyword present in [tokens].
     *
     * Longer n-grams are tried alongside shorter ones rather than instead of them: both
     * `dawai leni` and `dawai` match, which is exactly what makes the phrase weight able to
     * override the single-word reading.
     */
    private fun collectMatches(tokens: List<String>): Set<String> {
        val matched = LinkedHashSet<String>()
        val maxWindow = minOf(CategoryKeywords.MAX_NGRAM, tokens.size)
        for (size in 1..maxWindow) {
            for (start in 0..tokens.size - size) {
                val key = if (size == 1) {
                    tokens[start]
                } else {
                    tokens.subList(start, start + size).joinToString(separator = " ")
                }
                if (CategoryKeywords.INDEX.containsKey(key)) matched.add(key)
            }
        }
        return matched
    }

    /**
     * Picks the category with the highest score, walking [CategoryKeywords.PRECEDENCE] in
     * order and only replacing the incumbent on a *strictly* greater score. That makes the
     * earliest category in the precedence list win any tie.
     */
    private fun highestScoring(scores: IntArray): Category {
        var best = Category.OTHER
        var bestScore = 0
        for (category in CategoryKeywords.PRECEDENCE) {
            val score = scores[category.ordinal]
            if (score > bestScore) {
                bestScore = score
                best = category
            }
        }
        return best
    }
}
