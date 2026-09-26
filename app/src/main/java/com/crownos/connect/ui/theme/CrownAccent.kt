package com.crownos.connect.ui.theme

import androidx.compose.ui.graphics.Color

enum class CrownAccent(val label: String, val gradient: Gradient) {
    Purple("Purple", Gradient(Color(0xFF8F6DFB), Color(0xFF6D48E8))),
    Blue("Blue", Gradient(Color(0xFF5AA2FF), Color(0xFF2563EB))),
    Green("Green", Gradient(Color(0xFF4ADE80), Color(0xFF16A34A))),
    Orange("Orange", Gradient(Color(0xFFFB923C), Color(0xFFEA580C))),
    Pink("Pink", Gradient(Color(0xFFF472B6), Color(0xFFDB2777))),
}
