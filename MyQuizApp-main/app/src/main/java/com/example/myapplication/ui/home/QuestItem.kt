package com.example.myapplication.ui.home

data class QuestItem(
    val title: String,
    val current: Int, // 현재 달성 수치 (예: 10분, 5문제)
    val goal: Int,    // 목표 수치 (예: 30분, 20문제)
    val unit: String  // 단위 (분, 개)
) {
    // 달성 여부 확인 (현재 >= 목표)
    val isAchieved: Boolean
        get() = current >= goal
}