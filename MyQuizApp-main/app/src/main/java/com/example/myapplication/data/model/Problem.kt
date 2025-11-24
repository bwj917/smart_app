package com.example.myapplication.data.model

import java.util.Date

data class Problem(
    val problemId: Long,
    val question: String,
    val answer: String,

    val problemLevel: Int?, // 레벨은 없을 수 있으므로 Nullable 처리
    val nextReviewTime: Date?
)

