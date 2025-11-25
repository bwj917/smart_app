package com.example.myapplication.util

import com.example.myapplication.R

object CharacterManager {

    // 표정 타입 상수 정의
    const val TYPE_DEFAULT = 0   // 기본 (코딩/대기) - quit / quit_rabbit
    const val TYPE_CONFUSED = 1  // 고민/물음표 - quit2 / quit_rabbit2
    const val TYPE_CORRECT = 2   // 정답/인사 - quit3 / quit_rabbit3
    const val TYPE_WRONG = 3     // 오답/울음 - quit4 / quit_rabbit4

    // 🐧 0번 스킨: 펭귄 세트
    private val PENGUIN_SET = listOf(
        R.drawable.quit,   // DEFAULT
        R.drawable.quit2,  // CONFUSED
        R.drawable.quit3,  // CORRECT
        R.drawable.quit4   // WRONG
    )

    // 🐰 1번 스킨: 토끼 세트
    private val RABBIT_SET = listOf(
        R.drawable.quit_rabbit,
        R.drawable.quit_rabbit2,
        R.drawable.quit_rabbit3,
        R.drawable.quit_rabbit4
    )
    // 판다
    private val PANDA_SET = listOf(
        R.drawable.quit_panda,
        R.drawable.quit_panda2,
        R.drawable.quit_panda3,
        R.drawable.quit_panda4,
    )

    // 전체 스킨 리스트 (상점용)
    // [스킨ID] -> [리소스 리스트]
    val SKINS = listOf(PENGUIN_SET, RABBIT_SET, PANDA_SET)

    /**
     * @param skinIndex 현재 장착 중인 스킨 번호 (0: 펭귄, 1: 토끼)
     * @param type 필요한 표정 타입 (TYPE_DEFAULT 등)
     * @return 해당 이미지 리소스 ID
     */
    fun getImageRes(skinIndex: Int, type: Int): Int {
        // 유효하지 않은 스킨 번호면 기본 펭귄(0)으로 처리
        val safeSkinIndex = if (skinIndex in SKINS.indices) skinIndex else 0
        val selectedSet = SKINS[safeSkinIndex]
        
        // 유효하지 않은 타입이면 기본 표정(0)으로 처리
        val safeType = if (type in selectedSet.indices) type else 0
        
        return selectedSet[safeType]
    }
    
    // 상점에서 보여줄 대표 이미지 (기본 표정)
    fun getPreviewImage(skinIndex: Int): Int {
        return getImageRes(skinIndex, TYPE_DEFAULT)
    }
}