package com.iattend.app.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Fixed palette subjects auto-cycle through on creation; user can override per subject later. */
val SubjectColorPalette = listOf(
    0xFF6750A4, 0xFF7D5260, 0xFF386A20, 0xFF9C4146, 0xFF006874,
    0xFF984061, 0xFF4A6363, 0xFF8B5000, 0xFF4B607C, 0xFF6E5677
).map { Color(it.toInt()) }

fun nextSubjectColor(existingCount: Int): Int =
    SubjectColorPalette[existingCount % SubjectColorPalette.size].toArgb()
