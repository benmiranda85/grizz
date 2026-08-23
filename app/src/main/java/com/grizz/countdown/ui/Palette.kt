package com.grizz.countdown.ui

import androidx.compose.ui.graphics.Color

/** Card colours, chosen to stay distinguishable next to each other. */
val EventColors: List<Long> = listOf(
    0xFF3B6EF3, // blue
    0xFF00A3A3, // teal
    0xFF2FA84F, // green
    0xFFB8A600, // olive
    0xFFE8871E, // amber
    0xFFE0523E, // vermilion
    0xFFD6336C, // raspberry
    0xFF9B51E0, // violet
    0xFF5A6270, // slate
    0xFF1B1B1F  // near black
)

/** Compose's Color(Long) reads the low 32 bits as ARGB, which is exactly how we store it. */
fun Long.toComposeColor(): Color = Color(this)
