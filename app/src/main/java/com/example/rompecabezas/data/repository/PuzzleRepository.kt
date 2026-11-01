package com.example.rompecabezas.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.rompecabezas.R
import com.example.rompecabezas.data.model.PuzzleItem
import com.example.rompecabezas.util.ImageUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class PuzzleRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "rompecabezas_prefs"
        private const val KEY_PUZZLES_JSON = "saved_puzzles_json"
        const val DEFAULT_LOGO_ID = "merkadmon_logo"
    }

    init {
        // Ensure the default Merkadmon logo is always available as a playable puzzle
        ensureDefaultLogoExists()
    }

    private fun ensureDefaultLogoExists() {
        val puzzles = getAllPuzzles().toMutableList()
        val exists = puzzles.any { it.id == DEFAULT_LOGO_ID }
        if (!exists) {
            try {
                val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
                if (bitmap != null) {
                    val savedFile = ImageUtils.saveBitmapToInternalStorage(
                        context,
                        bitmap,
                        "puzzle_${DEFAULT_LOGO_ID}.jpg"
                    )
                    if (savedFile != null) {
                        val defaultItem = PuzzleItem(
                            id = DEFAULT_LOGO_ID,
                            filePath = savedFile.absolutePath,
                            title = "Logo Merkadmon",
                            createdAt = System.currentTimeMillis(),
                            maxUnlockedLevel = 1,
                            isCompleted = false
                        )
                        puzzles.add(0, defaultItem)
                        savePuzzlesList(puzzles)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getAllPuzzles(): List<PuzzleItem> {
        val jsonStr = prefs.getString(KEY_PUZZLES_JSON, null) ?: return emptyList()
        val list = mutableListOf<PuzzleItem>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val item = PuzzleItem(
                    id = obj.getString("id"),
                    filePath = obj.getString("filePath"),
                    title = obj.getString("title"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    maxUnlockedLevel = obj.optInt("maxUnlockedLevel", 1),
                    isCompleted = obj.optBoolean("isCompleted", false)
                )
                if (File(item.filePath).exists()) {
                    list.add(item)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getPuzzleById(id: String): PuzzleItem? {
        return getAllPuzzles().find { it.id == id }
    }

    fun saveNewPuzzle(bitmap: Bitmap, title: String = "Mi Rompecabezas"): PuzzleItem? {
        val id = UUID.randomUUID().toString()
        val fileName = "puzzle_${id}.jpg"
        val file = ImageUtils.saveBitmapToInternalStorage(context, bitmap, fileName) ?: return null

        val newItem = PuzzleItem(
            id = id,
            filePath = file.absolutePath,
            title = title,
            createdAt = System.currentTimeMillis(),
            maxUnlockedLevel = 1,
            isCompleted = false
        )

        val currentList = getAllPuzzles().toMutableList()
        currentList.add(0, newItem)
        savePuzzlesList(currentList)
        return newItem
    }

    fun recordLevelCompleted(puzzleId: String, completedLevel: Int) {
        val list = getAllPuzzles().toMutableList()
        val index = list.indexOfFirst { it.id == puzzleId }
        if (index != -1) {
            val current = list[index]
            val nextLevel = (completedLevel + 1).coerceAtMost(10)
            val newMaxLevel = maxOf(current.maxUnlockedLevel, nextLevel)
            list[index] = current.copy(
                maxUnlockedLevel = newMaxLevel,
                isCompleted = true
            )
            savePuzzlesList(list)
        }
    }

    fun deletePuzzle(puzzleId: String): Boolean {
        if (puzzleId == DEFAULT_LOGO_ID) return false // Don't delete default logo
        val list = getAllPuzzles().toMutableList()
        val item = list.find { it.id == puzzleId } ?: return false
        try {
            File(item.filePath).delete()
        } catch (_: Exception) {}
        list.removeAll { it.id == puzzleId }
        savePuzzlesList(list)
        return true
    }

    private fun savePuzzlesList(list: List<PuzzleItem>) {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("filePath", item.filePath)
            obj.put("title", item.title)
            obj.put("createdAt", item.createdAt)
            obj.put("maxUnlockedLevel", item.maxUnlockedLevel)
            obj.put("isCompleted", item.isCompleted)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_PUZZLES_JSON, jsonArray.toString()).apply()
    }
}

