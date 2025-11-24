package com.example.myapplication.auth

import android.content.Context

object AuthManager {

    private const val PREF_NAME = "auth_pref"
    private const val KEY_LOGIN = "isLoggedIn"
    private const val KEY_USER_ID = "userId"

    /**
     * 로그인 상태와 함께 유저 ID를 저장합니다.
     */
    fun setLoggedIn(context: Context, value: Boolean, userId: Long? = null) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pref.edit().apply {
            putBoolean(KEY_LOGIN, value)
            if (userId != null) {
                putLong(KEY_USER_ID, userId)
            } else {
                remove(KEY_USER_ID)
            }
            apply()
        }
    }

    /**
     * 저장된 유저 ID를 가져옵니다. 로그인되어 있지 않거나 ID가 없으면 null을 반환합니다.
     */
    fun getUserId(context: Context): Long? {
        if (!isLoggedIn(context)) return null

        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val userId = pref.getLong(KEY_USER_ID, -1L)
        return if (userId != -1L) userId else null
    }

    fun isLoggedIn(context: Context): Boolean {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pref.getBoolean(KEY_LOGIN, false)
    }

    /**
     * 로그아웃 시 로그인 상태와 유저 ID를 모두 삭제합니다.
     */
    fun logout(context: Context) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pref.edit().apply {
            putBoolean(KEY_LOGIN, false)
            remove(KEY_USER_ID)
            apply()
        }
    }
}