package com.example.model

import androidx.compose.ui.graphics.Color

enum class FoodItem(
    val id: String,
    val displayName: String,
    val emoji: String,
    val hungerRestore: Float,
    val coinCost: Int
) {
    STRAWBERRY("strawberry", "Strawberry Manis", "🍓", 0.25f, 5),
    CHEESE("cheese", "Keju Lezat", "🧀", 0.35f, 10),
    FISH("fish", "Ikan Segar", "🐟", 0.50f, 15),
    MILK("milk", "Susu Hangat", "🥛", 0.30f, 8),
    BURGER("burger", "Burger Lezat", "🍔", 0.60f, 25),
    CAKE("cake", "Kue Ulang Tahun", "🍰", 0.45f, 20)
}

enum class CareCategory(
    val id: String,
    val displayName: String,
    val emoji: String,
    val baseColor: Color,
    val ringColor: Color
) {
    MAKAN("makan", "Makan", "🍴", Color(0xFF2EC4B6), Color(0xFF22C55E)),
    TOILET("toilet", "Toilet", "🚽", Color(0xFF3B82F6), Color(0xFF00B4D8)),
    TIDUR("tidur", "Tidur", "🌙", Color(0xFFF59E0B), Color(0xFFFFD166)),
    HIBURAN("hiburan", "Bermain/Hiburan", "🏐", Color(0xFF10B981), Color(0xFF4ADE80))
}

enum class CareOverlayType {
    NONE,
    FOOD_TRAY,
    TOILET_ROOM,
    SLEEP_MODE,
    BALL_GAME,
    WARDROBE
}
