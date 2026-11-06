package com.example.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class MemoryFact(
    val id: String = UUID.randomUUID().toString(),
    val category: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Manages persistent long-term user memories for Petto AI:
 * - Automatically extracts personal facts (name, hobbies, favorite food/drink, school goals, feelings)
 *   from natural Indonesian/English conversation.
 * - Allows manual viewing, editing, adding, and deleting of memories in the UI.
 * - Formats a concise memory prompt block injected into Gemini 3.8 Flash / Gemini Live sessions.
 */
class PetMemoryManager(context: Context) {

    private val prefs = context.getSharedPreferences("pawpal_ai_memory_prefs", Context.MODE_PRIVATE)

    fun getUserNickname(): String {
        return prefs.getString(KEY_NICKNAME, "")?.trim().orEmpty()
    }

    fun setUserNickname(nickname: String) {
        prefs.edit().putString(KEY_NICKNAME, nickname.trim()).apply()
    }

    fun getMemories(): List<MemoryFact> {
        val rawJson = prefs.getString(KEY_FACTS_JSON, "[]") ?: "[]"
        return try {
            val arr = JSONArray(rawJson)
            val list = mutableListOf<MemoryFact>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    MemoryFact(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        category = obj.optString("category", "Catatan"),
                        content = obj.optString("content", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list.filter { it.content.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveMemories(facts: List<MemoryFact>) {
        val arr = JSONArray()
        facts.takeLast(25).forEach { fact ->
            arr.put(
                JSONObject().apply {
                    put("id", fact.id)
                    put("category", fact.category)
                    put("content", fact.content)
                    put("timestamp", fact.timestamp)
                }
            )
        }
        prefs.edit().putString(KEY_FACTS_JSON, arr.toString()).apply()
    }

    fun addOrUpdateFact(category: String, content: String): List<MemoryFact> {
        val cleanContent = content.trim()
        if (cleanContent.isBlank()) return getMemories()

        val current = getMemories().toMutableList()
        // Avoid exact duplicates
        val existingIdx = current.indexOfFirst {
            it.content.equals(cleanContent, ignoreCase = true) ||
                (it.category == category && it.category in SINGLETON_CATEGORIES)
        }
        val newFact = MemoryFact(category = category, content = cleanContent)
        if (existingIdx >= 0) {
            current[existingIdx] = newFact
        } else {
            current.add(0, newFact)
        }
        saveMemories(current)
        return current
    }

    fun removeFact(factId: String): List<MemoryFact> {
        val updated = getMemories().filterNot { it.id == factId }
        saveMemories(updated)
        return updated
    }

    fun clearAllMemories() {
        prefs.edit().remove(KEY_FACTS_JSON).remove(KEY_NICKNAME).apply()
    }

    /**
     * Automatically inspects a user's spoken or typed message for personal facts
     * and persists them into long-term memory.
     * Returns true if at least one new memory or nickname was learned.
     */
    fun extractAndSaveFromUtterance(userMessage: String): Boolean {
        val text = userMessage.trim()
        if (text.length < 4) return false

        var learnedAnything = false

        // 1. Name / Nickname detection
        val namePatterns = listOf(
            Regex("""(?:nama\s+aku|namaku|nama\s+saya|panggil\s+aku|panggil\s+saya|my\s+name\s+is|call\s+me)\s+([a-zA-Z0-9_]+(?:\s+[a-zA-Z0-9_]+)?)""", RegexOption.IGNORE_CASE),
            Regex("""^aku\s+([A-Z][a-zA-Z]{2,12})$""")
        )
        for (regex in namePatterns) {
            val match = regex.find(text)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                    .replace(Regex("""\b(ya|dong|nih|loh|sih|bro|kak)\b""", RegexOption.IGNORE_CASE), "")
                    .trim()
                val stopWords = setOf("lagi", "mau", "sedang", "suka", "bisa", "punya", "ingin", "dari", "anak", "orang")
                if (candidate.isNotBlank() && candidate.lowercase().split(" ").first() !in stopWords) {
                    setUserNickname(candidate)
                    addOrUpdateFact("Panggilan", "Nama panggilan pengguna adalah $candidate")
                    learnedAnything = true
                    break
                }
            }
        }

        // 2. Hobby / Favorite things detection
        val hobbyMatch = Regex(
            """(?:hobiku|hobi\s+aku|hobi\s+saya|aku\s+hobi|aku\s+paling\s+suka|kesukaanku)\s+(?:adalah\s+|itu\s+)?([^,.!?]{3,50})""",
            RegexOption.IGNORE_CASE
        ).find(text)
        if (hobbyMatch != null) {
            val hobby = hobbyMatch.groupValues[1].trim()
            if (hobby.isNotBlank()) {
                addOrUpdateFact("Hobi & Minat", "Menyukai / hobi: $hobby")
                learnedAnything = true
            }
        }

        // 3. Favorite Food / Drink detection
        val foodMatch = Regex(
            """(?:makanan\s+favoritku|minuman\s+favoritku|aku\s+suka\s+makan|aku\s+suka\s+minum)\s+([^,.!?]{3,45})""",
            RegexOption.IGNORE_CASE
        ).find(text)
        if (foodMatch != null) {
            val food = foodMatch.groupValues[1].trim()
            if (food.isNotBlank()) {
                addOrUpdateFact("Makanan Favorit", "Suka makan/minum: $food")
                learnedAnything = true
            }
        }

        // 4. School / Exam / Study Goal detection
        val schoolMatch = Regex(
            """(?:besok\s+ada\s+ujian|aku\s+ada\s+ulangan|lagi\s+belajar|pelajaran\s+favoritku|aku\s+kelas|cita-citaku)\s*([^,.!?]{2,55})""",
            RegexOption.IGNORE_CASE
        ).find(text)
        if (schoolMatch != null) {
            val fullPhrase = schoolMatch.value.trim()
            addOrUpdateFact("Sekolah & Target", fullPhrase)
            learnedAnything = true
        }

        // 5. Explicit "Ingat ya" / "Tolong ingat" command
        val explicitRememberMatch = Regex(
            """(?:ingat\s+ya|tolong\s+ingat|jangan\s+lupa)\s*[,:]?\s*(.{4,80})""",
            RegexOption.IGNORE_CASE
        ).find(text)
        if (explicitRememberMatch != null) {
            val note = explicitRememberMatch.groupValues[1].trim()
            if (note.isNotBlank()) {
                addOrUpdateFact("Catatan Penting", note)
                learnedAnything = true
            }
        }

        return learnedAnything
    }

    /**
     * Builds the prompt block injected into Gemini system instructions so the character
     * naturally recalls the user's name and personal facts.
     */
    fun buildMemoryPromptBlock(): String {
        val nickname = getUserNickname()
        val facts = getMemories()
        if (nickname.isBlank() && facts.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("\nMEMORI JANGKA PANJANG TENTANG PENGGUNA (SANGAT PENTING - GUNAKAN SECARA ALAMI):")
        if (nickname.isNotBlank()) {
            sb.appendLine("- Nama panggilan pengguna: $nickname (Sapalah atau sebut namanya sesekali secara akrab sesuai kepribadianmu).")
        }
        facts.take(12).forEach { fact ->
            sb.appendLine("- [${fact.category}] ${fact.content}")
        }
        return sb.toString()
    }

    companion object {
        private const val KEY_NICKNAME = "user_nickname"
        private const val KEY_FACTS_JSON = "memory_facts_json"
        private val SINGLETON_CATEGORIES = setOf("Panggilan")
    }
}
