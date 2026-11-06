package com.example.model

import androidx.compose.ui.graphics.Color

data class ColorOption(
    val id: String,
    val name: String,
    val color: Color
)

enum class EyeStyle(val id: String, val displayName: String, val icon: String) {
    SPARKLING("sparkling", "Sparkling Anime", "✨"),
    HEARTS("hearts", "Loving Hearts", "💖"),
    PLAYFUL_WINK("wink", "Playful Wink", "😉"),
    COOL_SHADES("cool", "Cool Shaded", "🕶️"),
    SLEEPY("sleepy", "Gentle Sleepy", "😴")
}

enum class EarStyle(val id: String, val displayName: String, val icon: String) {
    CAT_POINTY("cat_pointy", "Pointy Cat Ears", "🐱"),
    DOG_FLOPPY("dog_floppy", "Floppy Dog Ears", "🐶"),
    BEAR_ROUND("bear_round", "Round Bear Ears", "🐻"),
    RIBBON_EARS("ribbon", "Ribbon Headband", "🎀")
}

enum class CostumeStyle(val id: String, val displayName: String, val icon: String, val primaryColor: Color) {
    NONE("none", "Natural Fur", "🐾", Color.Transparent),
    HOODIE("hoodie", "Cozy Hoodie", "🧥", Color(0xFF3F51B5)),
    SUPERHERO("superhero", "Superhero Cape", "🦸", Color(0xFFE53935)),
    CHEF("chef", "Master Chef", "👨‍🍳", Color(0xFFF5F5F5)),
    DETECTIVE("detective", "Detective Coat", "🕵️", Color(0xFF5D4037)),
    PAJAMAS("pajamas", "Star Pajamas", "🌙", Color(0xFF5C6BC0)),
    ROYAL_ROBE("royal_robe", "Jubah Raja", "👑", Color(0xFFB71C1C)),
    CYBER_JACKET("cyber_jacket", "Jaket Cyber", "⚡", Color(0xFF00ACC1)),
    WINTER_COAT("winter_coat", "Mantel Salju", "❄️", Color(0xFF0288D1))
}

enum class AccessoryStyle(val id: String, val displayName: String, val icon: String) {
    NONE("none", "None", "❌"),
    BELL_COLLAR("bell_collar", "Golden Bell Collar", "🔔"),
    STAR_CHOKER("star_choker", "Star Tag Choker", "⭐"),
    SUNGLASSES("sunglasses", "Retro Shades", "🕶️"),
    ROUND_GLASSES("round_glasses", "Professor Glasses", "👓"),
    FLOWER_PIN("flower_pin", "Sakura Flower", "🌸"),
    ROYAL_CROWN("royal_crown", "Mahkota Emas", "👑"),
    WIZARD_HAT("wizard_hat", "Topi Penyihir", "🧙"),
    GAMING_HEADSET("gaming_headset", "Headset Gaming", "🎧"),
    RED_BOWTIE("red_bowtie", "Dasi Kupu-kupu", "🎀")
}

enum class RoomTheme(
    val id: String,
    val displayName: String,
    val icon: String,
    val wallTopColor: Color,
    val wallBottomColor: Color,
    val floorColor: Color
) {
    WARM_LIVING_ROOM(
        "living_room",
        "Cozy Living Room",
        "🛋️",
        Color(0xFFFFECD2),
        Color(0xFFFCB69F),
        Color(0xFFD7CCC8)
    ),
    STARRY_BEDROOM(
        "bedroom",
        "Starry Night",
        "✨",
        Color(0xFF141E30),
        Color(0xFF243B55),
        Color(0xFF37474F)
    ),
    SUNNY_GARDEN(
        "garden",
        "Sunny Garden",
        "🌿",
        Color(0xFFB9FBC0),
        Color(0xFF98EECC),
        Color(0xFF8D6E63)
    ),
    CAFE_CORNER(
        "cafe",
        "Cozy Coffee Shop",
        "☕",
        Color(0xFFEFEBE9),
        Color(0xFFD7CCC8),
        Color(0xFF795548)
    )
}

data class AvatarCustomization(
    val furColor: Color = Color(0xFFFF8A3D),
    val furColorId: String = "orange",
    val eyeStyle: EyeStyle = EyeStyle.SPARKLING,
    val earStyle: EarStyle = EarStyle.CAT_POINTY,
    val costume: CostumeStyle = CostumeStyle.NONE,
    val accessory: AccessoryStyle = AccessoryStyle.BELL_COLLAR,
    val roomTheme: RoomTheme = RoomTheme.WARM_LIVING_ROOM
)

object CustomizationCatalog {
    val furColors = listOf(
        ColorOption("orange", "Orange Ginger", Color(0xFFFF8A3D)),
        ColorOption("golden", "Golden Caramel", Color(0xFFE5A93C)),
        ColorOption("white", "Snow White", Color(0xFFFBFBFA)),
        ColorOption("midnight", "Midnight Charcoal", Color(0xFF37474F)),
        ColorOption("calico", "Calico Cream", Color(0xFFF7EBE1)),
        ColorOption("chocolate", "Choco Brown", Color(0xFF6D4C41)),
        ColorOption("pink", "Pastel Sakura", Color(0xFFFFCDD2)),
        ColorOption("mint", "Misty Teal", Color(0xFF80CBC4)),
        ColorOption("lavender", "Royal Lavender", Color(0xFFD1C4E9)),
        ColorOption("sky", "Frosty Sky", Color(0xFFB3E5FC))
    )
}
