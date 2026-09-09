package com.umair.smarttodo.data.categorizer

import java.util.Locale

/**
 * Normalises Roman-script input (English, Hinglish, Urdulish) into comparable tokens.
 *
 * Roman Urdu has no fixed orthography: the same word is written `jana` / `jaana` / `jaanaa`,
 * `dawai` / `dawaai` / `davai`, `naukri` / `nokri`. Enumerating every spelling in the keyword
 * tables would be endless, so tokens are additionally passed through a small, deliberately
 * conservative **fold** that erases the differences that carry no meaning in Roman
 * transliteration.
 *
 * The single rule that makes this safe: the *exact same* fold is applied to the keyword
 * tables at construction time and to user input at match time. English words are folded too
 * (`school` -> `schul`, `bill` -> `bil`), which looks odd in isolation but is harmless because
 * both sides of every comparison agree.
 *
 * All functions are pure, allocation-light and never throw.
 */
internal object TextNormalizer {

    /**
     * Splits [raw] into folded tokens.
     *
     * Anything that is not a letter or digit — punctuation, emoji, symbols — is treated as a
     * separator and discarded, so emoji-only or punctuation-only input yields an empty list.
     */
    fun tokenize(raw: String): List<String> {
        if (raw.isEmpty()) return emptyList()
        val tokens = ArrayList<String>()
        val current = StringBuilder()
        for (ch in raw) {
            if (ch.isLetterOrDigit()) {
                current.append(ch)
            } else if (current.isNotEmpty()) {
                tokens.add(fold(current.toString()))
                current.setLength(0)
            }
        }
        if (current.isNotEmpty()) tokens.add(fold(current.toString()))
        return tokens
    }

    /** Folds every whitespace-separated word of [phrase] and rejoins them with single spaces. */
    fun foldPhrase(phrase: String): String =
        phrase.trim()
            .split(WHITESPACE)
            .filter { it.isNotEmpty() }
            .joinToString(separator = " ") { fold(it) }

    /**
     * Canonicalises a single word.
     *
     * In order:
     * 1. lowercase (with [Locale.ROOT], so a Turkish device locale cannot change the result);
     * 2. long-vowel digraphs collapse — `aa`/`ii`/`uu` shorten, `ee` -> `i`, `oo` -> `u`
     *    (`jaana` -> `jana`, `khareedni` -> `kharidni`, `doodh` -> `dudh`);
     * 3. `au` -> `o` (`naukri` -> `nokri`, `daurna` -> `dorna`);
     * 4. `w` -> `v` (`dawai` -> `davai`) and `q` -> `k` (`waqt` -> `vakt`, `qarz` -> `karz`);
     * 5. any remaining run of a repeated character collapses to one (`bill` -> `bil`,
     *    `tayyari` -> `tayari`);
     * 6. a trailing `ay` becomes `e` (`paisay` -> `paise`, `kapray` -> `kapre`);
     * 7. a trailing silent `h` is dropped (`subah` -> `suba`).
     *
     * Steps 6 and 7 only apply to words of four characters or more, which protects short
     * English words such as `pay`, `day` and `the` from being mangled into collisions.
     */
    fun fold(word: String): String {
        if (word.isEmpty()) return word
        var folded = word.lowercase(Locale.ROOT)
        folded = folded
            .replace("aa", "a")
            .replace("ee", "i")
            .replace("oo", "u")
            .replace("ii", "i")
            .replace("uu", "u")
            .replace("au", "o")
        folded = folded.replace('w', 'v').replace('q', 'k')
        folded = collapseRepeats(folded)
        if (folded.length >= 4 && folded.endsWith("ay")) {
            folded = folded.dropLast(2) + "e"
        }
        if (folded.length >= 4 && folded.endsWith("h")) {
            folded = folded.dropLast(1)
        }
        return folded
    }

    /** Reduces every run of an identical character to a single occurrence. */
    private fun collapseRepeats(value: String): String {
        if (value.length < 2) return value
        val out = StringBuilder(value.length)
        for (ch in value) {
            if (out.isEmpty() || out[out.length - 1] != ch) out.append(ch)
        }
        return out.toString()
    }

    private val WHITESPACE = Regex("\\s+")
}
