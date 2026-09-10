package com.umair.smarttodo.ui.home.components

/**
 * Purely presentational keyword lookup: given a shopping-list item's raw text, returns a
 * small emoji to render next to it. Case-insensitive, substring ("whole-word-ish")
 * containment matching against common English and Hinglish/Urdu-Roman spellings —
 * deliberately simpler than [com.umair.smarttodo.domain.TaskCategorizer]'s weighted-variant
 * matching, since this is a visual nicety, not the app's core categorization intelligence.
 *
 * No persistence, no domain/data involvement: a pure `String -> String` lookup only used by
 * [TaskListDetailSheet] and [AddTaskListSheet] when a list's category is
 * [com.umair.smarttodo.domain.Category.SHOPPING].
 */
private val groceryKeywordEmojis: List<Pair<List<String>, String>> = listOf(
    listOf("milk", "doodh") to "\uD83E\uDD5B",
    listOf("egg", "eggs", "anda", "ande") to "\uD83E\uDD5A",
    listOf("bread", "double roti", "naan") to "\uD83C\uDF5E",
    listOf("rice", "chawal") to "\uD83C\uDF5A",
    listOf("sugar", "cheeni", "shakar") to "\uD83E\uDDC2",
    listOf("salt", "namak") to "\uD83E\uDDC2",
    listOf("oil", "tel") to "\uD83E\uDED2",
    listOf("vegetable", "vegetables", "sabzi", "sabzian") to "\uD83E\uDD66",
    listOf("fruit", "fruits", "phal") to "\uD83C\uDF4E",
    listOf("chicken", "murgi") to "\uD83C\uDF57",
    listOf("meat", "gosht") to "\uD83E\uDD69",
    listOf("fish", "machli") to "\uD83D\uDC1F",
    listOf("tea", "chai") to "\uD83C\uDF75",
    listOf("coffee") to "\u2615",
    listOf("water", "pani") to "\uD83D\uDCA7",
    listOf("flour", "atta") to "\uD83C\uDF3E",
    listOf("butter", "makhan") to "\uD83E\uDDC8",
    listOf("cheese", "paneer") to "\uD83E\uDDC0",
    listOf("onion", "pyaz") to "\uD83E\uDDC5",
    listOf("potato", "aloo") to "\uD83E\uDD54",
    listOf("tomato", "tamatar") to "\uD83C\uDF45",
    listOf("garlic", "lehsan") to "\uD83E\uDDC4",
    listOf("ginger", "adrak") to "\uD83E\uDEDA",
    listOf("soap", "sabun") to "\uD83E\uDDFC",
    listOf("shampoo") to "\uD83E\uDDF4",
    listOf("toothpaste", "paste") to "\uD83E\uDEA5",
    listOf("detergent", "surf") to "\uD83E\uDDFA",
    listOf("tissue", "tissues", "napkin", "napkins") to "\uD83E\uDDFB",
    listOf("juice", "ras") to "\uD83E\uDDC3",
    listOf("yogurt", "yoghurt", "dahi") to "\uD83E\uDD63",
    listOf("biscuit", "biscuits") to "\uD83C\uDF6A",
    listOf("chocolate", "chocolates") to "\uD83C\uDF6B",
    listOf("lentil", "lentils", "daal", "dal") to "\uD83E\uDED8",
    listOf("spice", "spices", "masala") to "\uD83C\uDF36\uFE0F",
)

/** Shown when no keyword matches — a generic shopping-cart glyph. */
private const val FallbackEmoji = "\uD83D\uDED2"

/**
 * Returns a small emoji for [itemText], based on case-insensitive substring containment
 * against [groceryKeywordEmojis]. Falls back to [FallbackEmoji] when nothing matches.
 */
fun groceryItemEmoji(itemText: String): String {
    val lower = itemText.lowercase()
    for ((keywords, emoji) in groceryKeywordEmojis) {
        if (keywords.any { lower.contains(it) }) return emoji
    }
    return FallbackEmoji
}
