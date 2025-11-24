package com.example.myapplication.ui.home

data class CourseItem(
    val title: String,
    val progressPercent: Int, // 0~100
    // 🔥 [수정] 푼 문제 수를 저장할 변수 추가 (기본값 0)
    val solvedCount: Int = 0
)