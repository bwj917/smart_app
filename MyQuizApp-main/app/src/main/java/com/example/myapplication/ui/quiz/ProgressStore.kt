package com.example.myapplication.ui.quiz

import android.content.Context

object ProgressStore {
    private const val PREF = "quiz_progress"

    fun save(context: Context, courseId: String, currentIndex: Int, solvedCount: Int) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit()
            .putInt("${courseId}_currentIndex", currentIndex)
            .putInt("${courseId}_solvedCount", solvedCount)
            .putLong("${courseId}_savedAt", System.currentTimeMillis())
            .apply()
    }

    // 동기 저장 (완료→닫기 직전처럼 확정 기록이 필요한 경우)
    fun saveSync(context: Context, courseId: String, currentIndex: Int, solvedCount: Int) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit()
            .putInt("${courseId}_currentIndex", currentIndex)
            .putInt("${courseId}_solvedCount", solvedCount)
            .putLong("${courseId}_savedAt", System.currentTimeMillis())
            .commit()
    }

    // (currentIndex, solvedCount) 반환
    fun load(context: Context, courseId: String): Pair<Int, Int> {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val currentIndex = sp.getInt("${courseId}_currentIndex", 1)
        val solvedCount  = sp.getInt("${courseId}_solvedCount", 0)
        return currentIndex to solvedCount
    }
}
