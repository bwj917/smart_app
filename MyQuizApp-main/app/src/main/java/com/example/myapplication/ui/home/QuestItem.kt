package com.example.myapplication.ui.home

data class QuestItem(
    val title: String,       // 퀘스트 제목 (예: "일일 학습 30분")
    val current: Int,        // 현재 진행도 (예: 15)
    val goal: Int,           // 목표치 (예: 30)
    val unit: String,        // 단위 (예: "분", "개")
    val isCompleted: Boolean // 완료 여부
)