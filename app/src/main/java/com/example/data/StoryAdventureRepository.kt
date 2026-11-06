package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.AvatarEmotion
import com.example.model.SavedStorySession
import com.example.model.StoryEndingType
import com.example.model.StoryItem
import com.example.model.StoryNode
import com.example.model.StoryTheme
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists interactive storytelling sessions ("Dongeng Petualangan") so users can save,
 * resume unfinished adventures, or review completed stories and collected magic items.
 */
class StoryAdventureRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSavedSessions(): List<SavedStorySession> {
        val raw = prefs.getString(KEY_SESSIONS_JSON, null) ?: return emptyList()
        val result = mutableListOf<SavedStorySession>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                parseSession(obj)?.let { result.add(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse saved story sessions: ${e.message}")
        }
        return result.sortedByDescending { it.updatedAtEpochMs }
    }

    fun saveOrUpdateSession(session: SavedStorySession): List<SavedStorySession> {
        val existing = getSavedSessions().toMutableList()
        val idx = existing.indexOfFirst { it.sessionId == session.sessionId }
        if (idx >= 0) {
            existing[idx] = session
        } else {
            existing.add(0, session)
        }
        val trimmed = existing.sortedByDescending { it.updatedAtEpochMs }.take(MAX_SAVED_SESSIONS)
        persistSessions(trimmed)
        return trimmed
    }

    fun deleteSession(sessionId: String): List<SavedStorySession> {
        val updated = getSavedSessions().filterNot { it.sessionId == sessionId }
        persistSessions(updated)
        return updated
    }

    private fun persistSessions(sessions: List<SavedStorySession>) {
        try {
            val arr = JSONArray()
            for (s in sessions) {
                arr.put(serializeSession(s))
            }
            prefs.edit().putString(KEY_SESSIONS_JSON, arr.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save story sessions: ${e.message}")
        }
    }

    private fun serializeSession(session: SavedStorySession): JSONObject {
        return JSONObject().apply {
            put("sessionId", session.sessionId)
            put("theme", session.theme.name)
            put("characterId", session.characterId)
            put("updatedAtEpochMs", session.updatedAtEpochMs)
            put("currentNode", serializeNode(session.currentNode))
            put("storyHistoryLog", JSONArray().apply {
                session.storyHistoryLog.forEach { put(it) }
            })
            put("inventory", JSONArray().apply {
                session.inventory.forEach { item ->
                    put(serializeItem(item))
                }
            })
        }
    }

    private fun serializeNode(node: StoryNode): JSONObject {
        return JSONObject().apply {
            put("chapterNumber", node.chapterNumber)
            put("maxChapters", node.maxChapters)
            put("title", node.title)
            put("narrative", node.narrative)
            put("petReactionComment", node.petReactionComment)
            put("sceneEmotion", node.sceneEmotion.name)
            put("choiceA", node.choiceA)
            put("choiceB", node.choiceB)
            if (node.acquiredItem != null) {
                put("acquiredItem", serializeItem(node.acquiredItem))
            }
            put("isEnding", node.isEnding)
            put("endingTitle", node.endingTitle)
            put("endingType", node.endingType.name)
            put("isFromAi", node.isFromAi)
        }
    }

    private fun serializeItem(item: StoryItem): JSONObject {
        return JSONObject().apply {
            put("id", item.id)
            put("name", item.name)
            put("emoji", item.emoji)
            put("description", item.description)
            put("chapterAcquired", item.chapterAcquired)
        }
    }

    private fun parseSession(obj: JSONObject): SavedStorySession? {
        val sessionId = obj.optString("sessionId", "")
        if (sessionId.isBlank()) return null
        val themeName = obj.optString("theme", StoryTheme.CRYSTAL_FOREST.name)
        val theme = StoryTheme.entries.find { it.name == themeName } ?: StoryTheme.CRYSTAL_FOREST
        val characterId = obj.optString("characterId", "petto")
        val updatedAt = obj.optLong("updatedAtEpochMs", System.currentTimeMillis())
        val nodeObj = obj.optJSONObject("currentNode") ?: return null
        val currentNode = parseNode(nodeObj) ?: return null

        val historyList = mutableListOf<String>()
        val histArr = obj.optJSONArray("storyHistoryLog")
        if (histArr != null) {
            for (i in 0 until histArr.length()) {
                val entry = histArr.optString(i, "")
                if (entry.isNotBlank()) historyList.add(entry)
            }
        }

        val invList = mutableListOf<StoryItem>()
        val invArr = obj.optJSONArray("inventory")
        if (invArr != null) {
            for (i in 0 until invArr.length()) {
                val itemObj = invArr.optJSONObject(i) ?: continue
                parseItem(itemObj)?.let { invList.add(it) }
            }
        }

        return SavedStorySession(
            sessionId = sessionId,
            theme = theme,
            characterId = characterId,
            currentNode = currentNode,
            storyHistoryLog = historyList,
            inventory = invList,
            updatedAtEpochMs = updatedAt
        )
    }

    private fun parseNode(obj: JSONObject): StoryNode? {
        val narrative = obj.optString("narrative", "")
        if (narrative.isBlank()) return null
        val emotionName = obj.optString("sceneEmotion", AvatarEmotion.HAPPY.name)
        val emotion = AvatarEmotion.entries.find { it.name == emotionName } ?: AvatarEmotion.HAPPY
        val endingName = obj.optString("endingType", StoryEndingType.HEROIC_LEGEND.name)
        val endingType = StoryEndingType.entries.find { it.name == endingName } ?: StoryEndingType.HEROIC_LEGEND
        val itemObj = obj.optJSONObject("acquiredItem")
        val acquiredItem = if (itemObj != null) parseItem(itemObj) else null

        return StoryNode(
            chapterNumber = obj.optInt("chapterNumber", 1),
            maxChapters = obj.optInt("maxChapters", 5),
            title = obj.optString("title", "Babak Petualangan"),
            narrative = narrative,
            petReactionComment = obj.optString("petReactionComment", ""),
            sceneEmotion = emotion,
            choiceA = obj.optString("choiceA", "Lanjutkan penjelajahan"),
            choiceB = obj.optString("choiceB", "Periksa sekitar bersama Petto"),
            acquiredItem = acquiredItem,
            isEnding = obj.optBoolean("isEnding", false),
            endingTitle = obj.optString("endingTitle", ""),
            endingType = endingType,
            isFromAi = obj.optBoolean("isFromAi", true)
        )
    }

    private fun parseItem(obj: JSONObject): StoryItem? {
        val name = obj.optString("name", "")
        if (name.isBlank()) return null
        return StoryItem(
            id = obj.optString("id", name.lowercase().replace(" ", "_")),
            name = name,
            emoji = obj.optString("emoji", "✨"),
            description = obj.optString("description", "Item ajaib dari petualangan"),
            chapterAcquired = obj.optInt("chapterAcquired", 1)
        )
    }

    companion object {
        private const val TAG = "StoryAdventureRepo"
        private const val PREFS_NAME = "pawpal_story_adventures_prefs"
        private const val KEY_SESSIONS_JSON = "saved_story_sessions_v1"
        private const val MAX_SAVED_SESSIONS = 12
    }
}
