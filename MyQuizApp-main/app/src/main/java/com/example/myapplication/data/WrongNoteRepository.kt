package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// 오답노트에 저장되는 한 문제 구조
data class WrongNoteItem(
    val id: Int,
    val question: String,
    val correctAnswer: String,
    val userAnswer: String
)

object WrongNoteRepository {

    private const val PREF_NAME = "wrong_note_pref"
    private const val KEY_NOTES = "wrong_notes"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // 🔹 전체 오답노트 가져오기
    fun getWrongNotes(context: Context): MutableList<WrongNoteItem> {
        val json = getPrefs(context).getString(KEY_NOTES, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<WrongNoteItem>>() {}.type
        return Gson().fromJson(json, type)
    }

    // 🔹 오답노트 추가
    fun addWrongNote(context: Context, item: WrongNoteItem) {
        val list = getWrongNotes(context)
        list.add(item)
        saveList(context, list)
    }

    // 🔹 내부 저장
    private fun saveList(context: Context, list: MutableList<WrongNoteItem>) {
        val json = Gson().toJson(list)
        getPrefs(context).edit().putString(KEY_NOTES, json).apply()
    }
}
