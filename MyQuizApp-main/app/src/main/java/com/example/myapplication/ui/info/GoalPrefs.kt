package com.example.myapplication.ui.info

import android.content.Context

object GoalPrefs {
    private const val PREF_NAME = "goal_prefs"

    // 목표 저장하기 (과목 이름, 개수)
    fun saveGoal(context: Context, courseTitle: String, goal: Int) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putInt(courseTitle, goal).apply()
    }

    // 목표 가져오기 (기본값 60개)
    fun getGoal(context: Context, courseTitle: String): Int {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // 저장된 게 없으면 기본 60개 리턴
        return sp.getInt(courseTitle, 60)
    }
}