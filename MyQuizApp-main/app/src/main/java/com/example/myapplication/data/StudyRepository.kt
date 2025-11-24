package com.example.myapplication.data

import android.content.Context


data class WrongProblem(
    val title: String,
    val question: String,
    val userAnswer: String,
    val correctAnswer: String
)
object StudyRepository {

    // TODO: 나중에 서버/DB 연결
    // ---------- 임시로 더미 데이터 반환 (나중에 여기만 바꾸면 모든 화면 자동 업데이트됨) ----------
    fun getWeeklyStudyCount(context: Context): List<Int> {
        // 최근 7일 공부량
        return listOf(9, 12, 8, 15, 20, 7, 18)
    }

    fun getMonthlyStudyCount(context: Context): List<Int> {
        // 이번 달 1일~30일 데이터
        return listOf(
            2, 3, 4, 5, 7, 6, 8, 10, 12, 11,
            6, 4, 3, 5, 8, 12, 14, 13, 16, 19,
            13, 12, 10, 8, 6, 4, 5, 7, 8, 9
        )
    }
    fun getWrongProblems(context: Context): List<WrongProblem> {
        // 👉 지금은 더미 데이터
        // 나중에 DB 연동하면 데이터 자동 교체 가능
        return listOf(
            WrongProblem(
                title = "자료구조 1번 문제",
                question = "스택의 특징은?",
                userAnswer = "FIFO",
                correctAnswer = "LIFO"
            ),
            WrongProblem(
                title = "네트워크 3번 문제",
                question = "HTTP 기본 포트는?",
                userAnswer = "21",
                correctAnswer = "80"
            )
        )
    }
}
