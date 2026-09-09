package com.umair.smarttodo.data.categorizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks down the folding rules that let one keyword entry cover many Roman-script spellings.
 *
 * If any of these change, [CategoryKeywords] has to be revisited: the same fold is applied to
 * the keyword table, so a change here silently reshapes every match.
 */
class TextNormalizerTest {

    @Test
    fun `long vowels collapse so jana spellings converge`() {
        val folded = listOf("jana", "jaana", "jaanaa", "JAANA").map(TextNormalizer::fold)
        assertEquals(setOf("jana"), folded.toSet())
    }

    @Test
    fun `medicine spellings converge`() {
        val folded = listOf("dawai", "dawaai", "davai", "DaWaI").map(TextNormalizer::fold)
        assertEquals(setOf("davai"), folded.toSet())
    }

    @Test
    fun `au folds to o so naukri and nokri converge`() {
        assertEquals(TextNormalizer.fold("nokri"), TextNormalizer.fold("naukri"))
        assertEquals(TextNormalizer.fold("dorna"), TextNormalizer.fold("daurna"))
    }

    @Test
    fun `doubled consonants collapse`() {
        assertEquals(TextNormalizer.fold("bil"), TextNormalizer.fold("bill"))
        assertEquals(TextNormalizer.fold("tayari"), TextNormalizer.fold("tayyari"))
        assertEquals(TextNormalizer.fold("ami"), TextNormalizer.fold("ammi"))
    }

    @Test
    fun `trailing ay becomes e but only for longer words`() {
        assertEquals(TextNormalizer.fold("paise"), TextNormalizer.fold("paisay"))
        assertEquals(TextNormalizer.fold("kapre"), TextNormalizer.fold("kapray"))
        // Short English words must survive intact, otherwise "pay" would be mangled.
        assertEquals("pay", TextNormalizer.fold("pay"))
        assertEquals("day", TextNormalizer.fold("day"))
    }

    @Test
    fun `trailing silent h is dropped`() {
        assertEquals("suba", TextNormalizer.fold("subah"))
        assertEquals(TextNormalizer.fold("tankhwa"), TextNormalizer.fold("tankhwah"))
    }

    @Test
    fun `tokenize strips punctuation and emoji`() {
        assertEquals(listOf("gym", "jana", "hai"), TextNormalizer.tokenize("gym, jana... hai!!!"))
        assertTrue(TextNormalizer.tokenize("\uD83D\uDE00 \uD83D\uDE80 \u2764").isEmpty())
        assertTrue(TextNormalizer.tokenize("!!! ??? ---").isEmpty())
        assertTrue(TextNormalizer.tokenize("").isEmpty())
        assertTrue(TextNormalizer.tokenize("   \t\n ").isEmpty())
    }

    @Test
    fun `tokenize collapses runs of whitespace`() {
        assertEquals(listOf("bug", "fix"), TextNormalizer.tokenize("  bug \t\n   fix  "))
    }

    @Test
    fun `digits survive tokenization`() {
        assertEquals(listOf("bil", "3050"), TextNormalizer.tokenize("bill 3050"))
    }

    @Test
    fun `folding is idempotent`() {
        val samples = listOf("khareedni", "bazaar", "presentation", "doodh", "quarterly", "gym")
        for (sample in samples) {
            val once = TextNormalizer.fold(sample)
            assertEquals("folding $sample twice must not change it again", once, TextNormalizer.fold(once))
        }
    }
}
