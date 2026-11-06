package com.example.model

import java.util.UUID

enum class GeminiModelTier(
    val label: String,
    val emoji: String,
    val subtitle: String,
    val modelId: String
) {
    AUTO(
        label = "Auto Pintar",
        emoji = "🤖",
        subtitle = "Pilih otomatis sesuai kerumitan pesan",
        modelId = "auto"
    ),
    ARGON_COMPLEX(
        label = "Argon (Kompleks)",
        emoji = "🧠",
        subtitle = "gemini-4-argon • Penalaran mendalam & analisis rumit",
        modelId = "gemini-4-argon"
    ),
    FLASH_GENERAL(
        label = "Flash (Umum)",
        emoji = "⚡",
        subtitle = "gemini-3.8-flash • Obrolan multi-turn sehari-hari",
        modelId = "gemini-3.8-flash"
    ),
    FLASH_LITE_FAST(
        label = "Flash-Lite (Kilat)",
        emoji = "🚀",
        subtitle = "gemini-3.1-flash-lite • Respons kilat & ringan",
        modelId = "gemini-3.1-flash-lite"
    )
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderName: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val emotion: AvatarEmotion = AvatarEmotion.TALKING,
    val modelUsed: String = ""
)
