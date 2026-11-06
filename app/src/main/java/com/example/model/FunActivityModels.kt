package com.example.model

enum class FunActivityTab(val title: String, val emoji: String) {
    MINI_GAME("Mini-Game", "🎮"),
    STORYTELLING("Dongeng AI", "📖"),
    HEALTHY_ROUTINE("Rutinitas Sehat", "⏰")
}

enum class RpsChoice(val label: String, val emoji: String) {
    ROCK("Batu", "✊"),
    SCISSORS("Gunting", "✌️"),
    PAPER("Kertas", "✋")
}

enum class RpsResult(val title: String, val badgeEmoji: String) {
    WIN("Kamu Menang!", "🏆"),
    DRAW("Seri!", "🤝"),
    LOSE("Petto Menang!", "😼")
}

data class RpsRoundState(
    val userChoice: RpsChoice,
    val petChoice: RpsChoice,
    val result: RpsResult,
    val commentary: String
)

data class QuizQuestion(
    val topic: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val isFromAi: Boolean = true
)

enum class StoryTheme(
    val title: String,
    val emoji: String,
    val subtitle: String,
    val promptHint: String,
    val stageTopColorHex: Long,
    val stageBottomColorHex: Long,
    val accentColorHex: Long,
    val ambientParticles: List<String>
) {
    CRYSTAL_FOREST(
        title = "Misteri Hutan Kristal",
        emoji = "🌲",
        subtitle = "Petualangan mencari permata cahaya yang hilang",
        promptHint = "petualangan fantasi seru di Hutan Kristal Bercahaya penuh pohon permata dan makhluk ajaib",
        stageTopColorHex = 0xFF0F172A,
        stageBottomColorHex = 0xFF064E3B,
        accentColorHex = 0xFF34D399,
        ambientParticles = listOf("✨", "💎", "🍃", "🦋")
    ),
    SPACE_ODYSSEY(
        title = "Ekspedisi Antariksa",
        emoji = "🚀",
        subtitle = "Menjelajahi galaksi baru & stasiun bintang bersama Petto",
        promptHint = "petualangan luar angkasa menjelajahi planet misterius, nebula ungu, dan artefak alien kuno",
        stageTopColorHex = 0xFF090D16,
        stageBottomColorHex = 0xFF1E1B4B,
        accentColorHex = 0xFF818CF8,
        ambientParticles = listOf("🌟", "🪐", "☄️", "🛸")
    ),
    MAGIC_KINGDOM(
        title = "Kastil Langit Naga",
        emoji = "🏰",
        subtitle = "Misi penyelamatan di istana terapung di atas awan",
        promptHint = "petualangan epik di Kerajaan Hewan Ajaib dan Kastil Langit Naga di atas awan emas",
        stageTopColorHex = 0xFF1E1B4B,
        stageBottomColorHex = 0xFF4C1D95,
        accentColorHex = 0xFFC084FC,
        ambientParticles = listOf("👑", "🐉", "☁️", "✨")
    ),
    OCEAN_ATLANTIS(
        title = "Kerajaan Bawah Laut",
        emoji = "🔱",
        subtitle = "Menyelami kota karang bercahaya & kapal harta karun",
        promptHint = "petualangan bawah laut di kota Atlantis yang penuh terumbu karang neon dan penjaga samudra",
        stageTopColorHex = 0xFF082F49,
        stageBottomColorHex = 0xFF0F766E,
        accentColorHex = 0xFF38BDF8,
        ambientParticles = listOf("🫧", "🐚", "🐠", "🔱")
    ),
    DETECTIVE_CITY(
        title = "Detektif Kota Tua",
        emoji = "🕵️",
        subtitle = "Memecahkan teka-teki jam raksasa dan lorong rahasia",
        promptHint = "kisah detektif misteri seru memecahkan teka-teki menara jam kuno dan surat rahasia",
        stageTopColorHex = 0xFF1C1917,
        stageBottomColorHex = 0xFF451A03,
        accentColorHex = 0xFFFBBF24,
        ambientParticles = listOf("🔍", "🗝️", "🕰️", "📜")
    ),
    BEDTIME_TALE(
        title = "Dongeng Pengantar Tidur",
        emoji = "🌙",
        subtitle = "Cerita malam yang hangat, damai, dan menenangkan",
        promptHint = "dongeng pengantar tidur yang tenang, hangat, penuh bintang mimpi dan pesan persahabatan",
        stageTopColorHex = 0xFF0F172A,
        stageBottomColorHex = 0xFF1E293B,
        accentColorHex = 0xFFFDE68A,
        ambientParticles = listOf("🌙", "⭐", "☁️", "💤")
    )
}

data class StoryItem(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val chapterAcquired: Int = 1
)

enum class StoryEndingType(
    val title: String,
    val badgeEmoji: String,
    val badgeColorHex: Long
) {
    HEROIC_LEGEND("Ending Legendaris & Heroik", "🏆", 0xFFF59E0B),
    SECRET_DISCOVERY("Ending Penemuan Rahasia", "💎", 0xFF38BDF8),
    FUNNY_TWIST("Ending Kejutan Kocak", "🎉", 0xFFEC4899),
    COZY_PEACEFUL("Ending Damai & Hangat", "🌟", 0xFF10B981)
}

data class StoryNode(
    val chapterNumber: Int,
    val maxChapters: Int = 5,
    val title: String,
    val narrative: String,
    val petReactionComment: String = "",
    val sceneEmotion: AvatarEmotion = AvatarEmotion.HAPPY,
    val choiceA: String,
    val choiceB: String,
    val acquiredItem: StoryItem? = null,
    val isEnding: Boolean = false,
    val endingTitle: String = "",
    val endingType: StoryEndingType = StoryEndingType.HEROIC_LEGEND,
    val isFromAi: Boolean = true
)

data class SavedStorySession(
    val sessionId: String,
    val theme: StoryTheme,
    val characterId: String,
    val currentNode: StoryNode,
    val storyHistoryLog: List<String>,
    val inventory: List<StoryItem>,
    val updatedAtEpochMs: Long
)

enum class HealthyHabitType(
    val title: String,
    val emoji: String,
    val subtitle: String,
    val buttonLabel: String
) {
    DRINK_WATER(
        title = "Minum Air Putih",
        emoji = "💧",
        subtitle = "Jaga tubuh tetap segar & fokus (Target 8 gelas/hari)",
        buttonLabel = "+1 Gelas Air"
    ),
    BODY_STRETCH(
        title = "Peregangan Badan",
        emoji = "🧘",
        subtitle = "Gerakkan bahu & leher bareng Petto biar gak pegal",
        buttonLabel = "Peregangan Bareng"
    ),
    EYE_REST(
        title = "Istirahat Mata (20-20-20)",
        emoji = "👀",
        subtitle = "Lihat objek jauh selama 20 detik agar mata segar",
        buttonLabel = "Segarkan Mata"
    ),
    DEEP_BREATH(
        title = "Napas Relaksasi",
        emoji = "🌿",
        subtitle = "Tarik napas dalam bersama Petto untuk tenangkan pikiran",
        buttonLabel = "Tarik Napas"
    )
}
