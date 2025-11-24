// DateUtils.kt 파일 (기존 String? 함수를 아래 코드로 대체)

package com.example.myapplication.util

import android.util.Log
import com.example.myapplication.data.model.Problem
import java.util.Date
import kotlin.math.abs

/**
 * Date? 객체를 현재 시간과의 차이를 기반으로
 * "N분 후 복습"과 같은 상대 시간 문자열로 변환하는 확장 함수입니다.
 * (Problem.nextReviewTime: Date? 에 바로 사용 가능)
 */
fun Date?.toRelativeReviewTime(): String {
    if (this == null) return "오류"

    try {
        val reviewTime: Date = this
        val diffMillis = reviewTime.time - System.currentTimeMillis()

        if (diffMillis <= 0) return "지금 바로 복습"

        // 반올림 (30초 이상이면 1분 추가)
        val diffMillisAdjusted = diffMillis + 30000L
        val diffMinutes = diffMillisAdjusted / (1000 * 60L)

        return when {
            // 1. 1시간 미만 -> "N분 후 복습"
            diffMinutes < 60 -> "$diffMinutes 분 후 복습"

            // 2. 1일(24시간) 미만 -> "N시간 후 복습"
            diffMinutes < 24 * 60 -> {
                val hours = diffMinutes / 60
                "$hours 시간 후 복습"
            }

            // 3. 1일 이상 -> "N일 후 복습" (여기 추가됨!)
            else -> {
                val days = diffMinutes / (24 * 60)
                "$days 일 후 복습"
            }
        }

    } catch (e: Exception) {
        return "시간 계산 오류"
    }
}


/**
 * Date? 객체를 현재 시간과의 차이를 기반으로
 * "N일 전", "N분 전", "재도전", "새 문제"와 같은 상대 시간 문자열을 반환하는 확장 함수입니다.
 */
fun Problem.toProblemStatusText(): String {
    if (this.nextReviewTime == null) {
        return "새 문제"
    }

    val now = System.currentTimeMillis()
    val reviewTime = this.nextReviewTime.time

    // 1. 미래인 경우 -> 무조건 "재도전"
    if (reviewTime > now) {
        return "재도전"
    }

    // 2. 과거인 경우 -> "N분 전 풂" (역계산)
    val levelMinutes = when (this.problemLevel ?: 1) {
        1 -> 5
        2 -> 24 * 60
        3 -> 24 * 60 * 3 // 3단계부터 시간이 다를 수 있으니 서버 설정 확인 필요
        4 -> 24 * 60 * 7
        5 -> 24 * 60 * 15
        else -> 5
    }

    val levelMillis = levelMinutes * 1000L * 60L
    val solvedTime = reviewTime - levelMillis
    val diffMillis = now - solvedTime

    if (diffMillis < 0) return "방금 전"

    val diffMinutes = diffMillis / (1000 * 60)

    return when {
        diffMinutes < 1 -> "방금 전"
        diffMinutes < 60 -> "${diffMinutes}분 전"
        else -> {
            val diffHours = diffMinutes / 60
            if (diffHours < 24) {
                "${diffHours}시간 전"
            } else {
                val diffDays = diffHours / 24
                "${diffDays}일 전"
            }
        }
    }
}