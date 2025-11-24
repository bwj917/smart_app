package com.example.myapplication.ui.home

data class CourseItem(
    val title: String,
    val progressPercent: Int, // 진행률 (0~100)
    val solvedCount: Int = 0, // 🔥 [추가] 실제 푼 문제 개수
    val goal: Int = 60        // 🔥 [추가] 목표 개수 (기본 60)
)