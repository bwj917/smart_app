package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

data class SubmissionResponse(
    // 🔥 서버에서는 isCorrect로 보내지만, 앱에서는 correct로 쓰고 싶다면 @SerializedName 사용
    @SerializedName("correct")
    val correct: Boolean,

    @SerializedName("problemResponse")
    val problemResponse: ProblemResponseDto?
)

data class ProblemResponseDto(
    val problemId: Long,
    val question: String,
    val answer: String,
    // 🔥 stats 필드 추가 (서버 DTO 변경 사항 반영)
    val stats: UserProblemStats?
)

// 🔥 UserProblemStats 클래스가 없어서 에러가 났으므로 여기에 정의하거나 별도 파일로 분리
data class UserProblemStats(
    val problemLevel: Int,
    val nextReviewTime: String?
)