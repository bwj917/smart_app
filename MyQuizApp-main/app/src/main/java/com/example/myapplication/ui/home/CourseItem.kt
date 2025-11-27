package com.example.myapplication.ui.home

data class CourseItem(
    val title: String,
    val progressPercent: Int, // 진행률 (0~100)
    val solvedCount: Int = 0, // 실제 푼 문제 개수 (기본값 0)
    val goal: Int = 60        // [추가됨] 목표 개수 (기본값 60)
)