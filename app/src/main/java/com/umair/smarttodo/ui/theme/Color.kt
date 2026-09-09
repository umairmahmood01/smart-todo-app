package com.umair.smarttodo.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Design tokens taken verbatim from the approved mockup (`mockups/main-screen.html`).
 * The app is dark-theme only, so there is a single palette.
 */

// Backgrounds and surfaces
val AppBackground = Color(0xFF14132B)
val SurfaceElevated = Color(0xFF232244)
val SurfaceMuted = Color(0xFF1E1D3A)
val SurfaceProgressTop = Color(0xFF262551)
val SurfaceProgressBottom = Color(0xFF1F1E3C)

// Accent
val NeonPurple = Color(0xFFA855F7)
val NeonPurpleLight = Color(0xFFC084FC)
val NeonPurpleDeep = Color(0xFF7C3AED)

// Task card gradient
val TaskGradientStart = Color(0xFFA020F0)
val TaskGradientMid = Color(0xFF9526E4)
val TaskGradientEnd = Color(0xFF8B1FD9)

// Text
val TextPrimary = Color(0xFFFFFFFF)
val TextMuted = Color(0xFFA5A3C4)
val TextPlaceholder = Color(0xFF6F6D93)
val TextOnAccent = Color(0xFFFFFFFF)

// Progress ring
val ProgressTrack = Color(0xFF332F5E)

// Status colours
val StatusTodo = Color(0xFFF44336)
val StatusInProgress = Color(0xFFFFC107)
val StatusDone = Color(0xFF4CAF50)

// Hairlines / scrims lifted from the mockup's rgba(255,255,255,0.06 - 0.22) rules
val HairlineBorder = Color(0x0FFFFFFF)
val HairlineBorderStrong = Color(0x1AFFFFFF)
val OnCardScrim = Color(0x33FFFFFF)
val OnCardBorder = Color(0x2EFFFFFF)
val OnCardTextMuted = Color(0xC2FFFFFF)

/** Ambient glow used behind the top of the screen. */
val AmbientTopGlow = Color(0x47A855F7)

/** Ambient glow used behind the bottom-right of the screen. */
val AmbientBottomGlow = Color(0x38604FD6)

/** `linear-gradient(140deg, ...)` of the progress card. */
val ProgressCardBrush: Brush
    get() = Brush.linearGradient(
        colors = listOf(SurfaceProgressTop, SurfaceElevated, SurfaceProgressBottom),
    )

/** `linear-gradient(145deg, ...)` of a task card. */
val TaskCardBrush: Brush
    get() = Brush.linearGradient(
        colors = listOf(TaskGradientStart, TaskGradientMid, TaskGradientEnd),
    )

/** `linear-gradient(145deg, ...)` of the floating action button. */
val FabBrush: Brush
    get() = Brush.linearGradient(
        colors = listOf(Color(0xFFB45BFF), NeonPurple, TaskGradientEnd),
    )

/** `linear-gradient(150deg, ...)` of the avatar. */
val AvatarBrush: Brush
    get() = Brush.linearGradient(
        colors = listOf(NeonPurpleDeep, TaskGradientStart, NeonPurpleLight),
    )

/** The purple → light-purple sweep used on the progress ring arc. */
val ProgressRingColors = listOf(NeonPurpleLight, NeonPurple)
