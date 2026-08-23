package com.iattend.app.core.data.db

import kotlinx.serialization.Serializable

@Serializable
enum class ClassType(val letter: String, val label: String, val pluralLabel: String) {
    LECTURE("L", "Lecture", "Lec"),
    TUTORIAL("T", "Tutorial", "Tut"),
    PRACTICAL("P", "Practical", "Pra")
}
