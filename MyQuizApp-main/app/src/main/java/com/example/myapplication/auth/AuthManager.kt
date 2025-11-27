package com.example.myapplication.auth

import android.content.Context

object AuthManager {

    private const val PREF_NAME = "auth_pref"
    private const val KEY_USER_ID = "saved_user_id" // 로그인 유지용 (Long)
    private const val KEY_SAVED_ID_TEXT = "saved_id_text" // 🔥 [추가] 아이디 저장용 (String)

    // 앱이 켜져있는 동안 로그인 정보를 담아둘 변수 (세션)
    private var sessionUserId: Long? = null

    /**
     * 로그인 성공 시 호출 (로그인 유지 처리)
     */
    fun setLoggedIn(context: Context, userId: Long, isKeepLogin: Boolean) {
        sessionUserId = userId

        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = pref.edit()

        if (isKeepLogin) {
            editor.putLong(KEY_USER_ID, userId)
        } else {
            editor.remove(KEY_USER_ID)
        }
        editor.apply()
    }

    /**
     * 🔥 [추가] 아이디 저장 기능
     * isSave: true면 저장, false면 삭제
     */
    fun setSavedIdForDisplay(context: Context, id: String, isSave: Boolean) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = pref.edit()
        if (isSave) {
            editor.putString(KEY_SAVED_ID_TEXT, id)
        } else {
            editor.remove(KEY_SAVED_ID_TEXT)
        }
        editor.apply()
    }

    /**
     * 🔥 [추가] 저장된 아이디 문자열 가져오기
     */
    fun getSavedIdForDisplay(context: Context): String? {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pref.getString(KEY_SAVED_ID_TEXT, null)
    }

    fun getUserId(context: Context): Long? {
        if (sessionUserId != null) {
            return sessionUserId
        }
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedId = pref.getLong(KEY_USER_ID, -1L)

        return if (savedId != -1L) {
            sessionUserId = savedId
            savedId
        } else {
            null
        }
    }

    fun isLoggedIn(context: Context): Boolean {
        return getUserId(context) != null
    }

    fun logout(context: Context) {
        sessionUserId = null

        // 1. 로그인 정보 삭제
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pref.edit().remove(KEY_USER_ID).apply()

        // 2. 🔥 [추가] 목표 설정 정보(GoalPrefs)도 싹 비워줍니다.
        val goalPref = context.getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)
        goalPref.edit().clear().apply()

        // (참고) 만약 퀘스트 달성 정보 등 다른 유저 데이터도 Prefs에 저장했다면 여기서 같이 clear 해주세요.
        // val questPref = context.getSharedPreferences("QuestPrefs", Context.MODE_PRIVATE)
        // questPref.edit().clear().apply()
    }
}