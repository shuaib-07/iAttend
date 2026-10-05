package com.iattend.app.core.domain.stats

fun isValidAttendancePercentage(value: Float?): Boolean =
    value != null && value.isFinite() && value in 0f..100f
