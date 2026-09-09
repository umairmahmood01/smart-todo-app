package com.umair.smarttodo.data.categorizer

import com.umair.smarttodo.domain.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavioural tests for [RuleBasedTaskCategorizer].
 *
 * Every case is written the way a user actually types: Roman script, mixed English /
 * Hinglish / Urdulish, no diacritics and inconsistent spelling.
 */
class RuleBasedTaskCategorizerTest {

    private val categorizer = RuleBasedTaskCategorizer()

    private fun assertCategory(expected: Category, input: String) {
        assertEquals("input: \"$input\"", expected, categorizer.categorize(input))
    }

    // -----------------------------------------------------------------------------------
    // One representative case per category (the examples from the product brief)
    // -----------------------------------------------------------------------------------

    @Test
    fun `work example`() = assertCategory(Category.WORK, "kal client ko presentation bhejni hai")

    @Test
    fun `health example`() = assertCategory(Category.HEALTH_FITNESS, "gym jana hai subah")

    @Test
    fun `coding example`() = assertCategory(Category.CODING, "React ka bug fix karna hai")

    @Test
    fun `finance example`() = assertCategory(Category.FINANCE, "bijli ka bill pay karna hai")

    @Test
    fun `shopping example`() = assertCategory(Category.SHOPPING, "sabzi leni hai bazaar se")

    @Test
    fun `study example`() = assertCategory(Category.STUDY, "exam ki tayari karni hai")

    @Test
    fun `plain english work example`() = assertCategory(Category.WORK, "Prepare quarterly report")

    @Test
    fun `personal example`() = assertCategory(Category.PERSONAL, "bhai ki shaadi hai")

    // -----------------------------------------------------------------------------------
    // Roman Urdu / Hinglish spelling variants of the same concept
    // -----------------------------------------------------------------------------------

    @Test
    fun `gym spelling variants all resolve to health`() {
        listOf(
            "gym jana hai",
            "gym jaana hai",
            "jim jaanaa hai subah",
            "subah jogging karni hai",
            "warzish karni hai",
            "varzish karni hai",
        ).forEach { assertCategory(Category.HEALTH_FITNESS, it) }
    }

    @Test
    fun `medicine spelling variants all resolve to health`() {
        listOf(
            "dawai khatam ho gayi",
            "dawaai khatam ho gayi",
            "davai khatam ho gayi",
            "dawa khatam ho gayi",
        ).forEach { assertCategory(Category.HEALTH_FITNESS, it) }
    }

    @Test
    fun `job spelling variants all resolve to work`() {
        listOf(
            "naukri ki application bhejni hai",
            "nokri ki application bhejni hai",
            "daftar jana hai",
            "daftr jana hai",
            "office jana hai",
        ).forEach { assertCategory(Category.WORK, it) }
    }

    @Test
    fun `money spelling variants all resolve to finance`() {
        listOf(
            "paise bank mein jama karwane hain",
            "paisay bank mein jama karwane hain",
            "qarza wapas karna hai",
            "karz wapas karna hai",
            "kiraya dena hai",
            "kiraaya dena hai",
        ).forEach { assertCategory(Category.FINANCE, it) }
    }

    @Test
    fun `buying spelling variants all resolve to shopping`() {
        listOf(
            "bazaar se sabzi kharidni hai",
            "bazar se sabzee khareedni hai",
            "doodh aur anday lene hain",
            "dudh aur ande lane hain",
        ).forEach { assertCategory(Category.SHOPPING, it) }
    }

    @Test
    fun `study spelling variants all resolve to study`() {
        listOf(
            "imtihan ki tayari karni hai",
            "imtehaan ki tyari karni hai",
            "padhai karni hai raat ko",
            "parhai karni hai raat ko",
            "kitaab parhni hai",
        ).forEach { assertCategory(Category.STUDY, it) }
    }

    @Test
    fun `coding spelling variants all resolve to coding`() {
        listOf(
            "React ka bug fix karna hai",
            "react ka bug fix krna hai",
            "kotlin ka code review karna hai",
            "api ka unit test likhna hai",
            "git par commit push karna hai",
        ).forEach { assertCategory(Category.CODING, it) }
    }

    @Test
    fun `personal spelling variants all resolve to personal`() {
        listOf(
            "ammi ko call karna hai",
            "ami ko call karna hai",
            "ghar ki safai karni hai",
            "ghar ki safaai karni hai",
            "bachon ke saath picnic",
        ).forEach { assertCategory(Category.PERSONAL, it) }
    }

    // -----------------------------------------------------------------------------------
    // Mixed language input
    // -----------------------------------------------------------------------------------

    @Test
    fun `english urdu code switching still classifies`() {
        assertCategory(Category.WORK, "kal office mein client ke saath meeting hai")
        assertCategory(Category.CODING, "react native ka crash debug karna hai urgently")
        assertCategory(Category.FINANCE, "electricity ka bill online pay karna hai")
        assertCategory(Category.HEALTH_FITNESS, "morning walk ke baad protein shake")
    }

    @Test
    fun `casing and punctuation are irrelevant`() {
        val expected = categorizer.categorize("gym jana hai")
        assertEquals(expected, categorizer.categorize("GYM JANA HAI"))
        assertEquals(expected, categorizer.categorize("  Gym... JANA, hai!!!  "))
    }

    // -----------------------------------------------------------------------------------
    // Documented tie-breaks
    // -----------------------------------------------------------------------------------

    @Test
    fun `tie between finance and health resolves to finance by precedence`() {
        // "gym" and "bill" are both STRONG, so the totals are equal (3 vs 3) and the
        // precedence list decides: FINANCE sits above HEALTH_FITNESS.
        assertCategory(Category.FINANCE, "gym aur bill")
    }

    @Test
    fun `tie between coding and work resolves to coding by precedence`() {
        // "office" (WORK, STRONG) vs "code" (CODING, STRONG): CODING leads the precedence list.
        assertCategory(Category.CODING, "office code")
    }

    @Test
    fun `precedence list covers every category except OTHER`() {
        assertEquals(Category.entries.size - 1, CategoryKeywords.PRECEDENCE.size)
        assertEquals(CategoryKeywords.PRECEDENCE.size, CategoryKeywords.PRECEDENCE.toSet().size)
        assertTrue(Category.OTHER !in CategoryKeywords.PRECEDENCE)
    }

    @Test
    fun `buying medicine is shopping not health or personal`() {
        // Documented decision: an acquisition verb applied to a purchasable object wins over
        // the object's own topic, because the user will look for it in the Shopping bucket.
        assertCategory(Category.SHOPPING, "ammi ke liye dawai leni hai")
        assertCategory(Category.SHOPPING, "dawa lani hai")
        // Without the acquisition verb the topical reading stands.
        assertCategory(Category.HEALTH_FITNESS, "dawai khatam ho gayi hai")
    }

    // -----------------------------------------------------------------------------------
    // Degenerate input
    // -----------------------------------------------------------------------------------

    @Test
    fun `empty input is OTHER`() = assertCategory(Category.OTHER, "")

    @Test
    fun `whitespace only input is OTHER`() {
        listOf(" ", "     ", "\t", "\n", " \t \n  ").forEach { assertCategory(Category.OTHER, it) }
    }

    @Test
    fun `emoji only input is OTHER`() {
        assertCategory(Category.OTHER, "\uD83D\uDE00\uD83D\uDE80\u2764")
        assertCategory(Category.OTHER, "\uD83D\uDE00 \uD83C\uDF89 \uD83D\uDCAF")
    }

    @Test
    fun `punctuation only input is OTHER`() = assertCategory(Category.OTHER, "!!! ??? --- ...")

    @Test
    fun `unrecognised words are OTHER`() =
        assertCategory(Category.OTHER, "asdkjh qwoieu zxcvbnm plmokn")

    @Test
    fun `very long input is handled without throwing`() {
        val filler = "lorem ipsum dolor sit amet ".repeat(4_000)
        assertCategory(Category.OTHER, filler)
        assertCategory(Category.HEALTH_FITNESS, "gym jana hai $filler")
    }

    @Test
    fun `repeating a keyword does not inflate its score`() {
        // Each distinct keyword is counted once, so five "bug"s still score 3 for CODING
        // and lose to "office" + "meeting" scoring 6 for WORK. Reversing the repetition
        // reverses the winner, which is only possible because repeats are deduplicated.
        assertCategory(Category.WORK, "bug bug bug bug bug office meeting")
        assertCategory(Category.CODING, "office office office office bug debug")
    }

    @Test
    fun `matching is on whole tokens not substrings`() {
        // "gym" inside "gymkhana", "pr" inside "prepare", "api" inside "rapid".
        assertCategory(Category.OTHER, "gymkhana rapid prepare")
    }

    @Test
    fun `categorization is deterministic across repeated calls`() {
        val input = "kal client ko presentation bhejni hai aur gym bhi jana hai"
        val first = categorizer.categorize(input)
        repeat(50) { assertSame(first, categorizer.categorize(input)) }
    }

    @Test
    fun `never throws for adversarial input`() {
        listOf(
            "\u0000\u0001\u0002",
            "'; DROP TABLE tasks; --",
            "%%%___%%%",
            "\uD83D\uDE00".repeat(5_000),
            "a".repeat(50_000),
        ).forEach { input ->
            // The contract is only "does not throw"; the resulting value is unconstrained.
            categorizer.categorize(input)
        }
    }

    @Test
    fun `keyword index is substantial and free of cross category collisions`() {
        assertTrue("expected a substantial keyword table", CategoryKeywords.INDEX.size > 500)
        val collisions = CategoryKeywords.INDEX.filterValues { it.size > 1 }
        assertTrue(
            "keywords voting for more than one category: ${collisions.keys}",
            collisions.isEmpty(),
        )
    }

    @Test
    fun `every category except OTHER has keywords`() {
        val covered = CategoryKeywords.RULES.map { it.category }.toSet()
        assertEquals(CategoryKeywords.PRECEDENCE.toSet(), covered)
    }
}
