package com.example.model

/**
 * Autonomous idle behaviors performed by the 3D pet when not actively in a conversation.
 */
enum class IdlePetBehavior(
    val displayLabel: String,
    val particleSymbol: String
) {
    NONE("", ""),
    STRETCHING("Meregangkan badan...", "✨"),
    LOOKING_AROUND("Melihat sekeliling...", "👀"),
    SNEEZING("Hachoo! Bersin...", "💨"),
    YAWNING("Menguap santai...", "💤"),
    EAR_SCRATCH("Menggaruk telinga...", "🐾"),
    HAPPY_BOUNCE("Melompat kecil...", "🎵");

    companion object {
        val activeBehaviors: List<IdlePetBehavior> = listOf(
            STRETCHING,
            LOOKING_AROUND,
            SNEEZING,
            YAWNING,
            EAR_SCRATCH,
            HAPPY_BOUNCE
        )
    }
}
