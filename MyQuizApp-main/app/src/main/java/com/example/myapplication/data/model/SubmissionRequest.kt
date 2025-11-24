package com.example.myapplication.data.model

data class SubmissionRequest(
    val problemId: Long,
    val userId: Long,
    val userAnswer: String,
    val checkCount: Int,

    // 🔥 [신규 추가] 기본값 0 (혹시 모를 에러 방지)
    val studyTime: Int = 0
)