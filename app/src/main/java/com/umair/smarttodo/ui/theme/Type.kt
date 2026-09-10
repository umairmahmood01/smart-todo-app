package com.umair.smarttodo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.umair.smarttodo.R

/**
 * Inter (static weights 400/500/600/700), bundled from Google Fonts' gstatic CDN — see
 * `app/src/main/res/font/`. A real bundled typeface instead of the ambiguous platform
 * default, chosen for a clean, professional look in a dark productivity app.
 */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/**
 * Type scale mapped from the mockup, refined for a more professional feel: Inter in
 * place of the ambiguous platform default, a clearer weight step between
 * title/label/body (no more blanket ExtraBold everywhere — headlines are Bold, section
 * titles SemiBold, tags/labels Medium/SemiBold, body Normal), and slightly tighter
 * tracking on headlines. Sizes and line-height ratios follow the approved mockup, with
 * one deliberate departure: [Typography.titleLarge] (the card heading) was dialled down
 * from 18sp Bold to 15sp Medium at the user's request — see its comment below.
 */
val SmartTodoTypography = Typography(
    // Greeting
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.4).sp,
    ),
    // Task / task-list card heading. Deliberately small and light: the user found the
    // old 18sp Bold heading "quite big" and wanted something "small and with a subtle
    // font". Hierarchy on a card now comes from colour, weight and breathing room rather
    // than raw size, so this is Medium at 15sp with tight tracking; Bold stays reserved
    // for genuinely emphatic elements (greeting, progress percentage).
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp,
    ),
    // Section headers ("Your tasks")
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp,
    ),
    // Progress card headline
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    // Search field text
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
    ),
    // Task description / secondary lines
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    // Chips, tracker pills, tags
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = (-0.1).sp,
    ),
    // "Pinned" badge, counts
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.2.sp,
    ),
)

/** Italic tagline under the greeting. */
val TaglineTextStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontStyle = FontStyle.Italic,
    fontSize = 12.5.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.1.sp,
)
