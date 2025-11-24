package com.example.myapplication.auth

import android.content.Context

object LoginPrefs {

    private const val PREF_NAME = "login_pref"
    private const val KEY_LOGIN = "is_login"
    private const val KEY_DEMO = "is_demo"

    fun saveLogin(context: Context, isDemo: Boolean) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit()
            .putBoolean(KEY_LOGIN, true)
            .putBoolean(KEY_DEMO, isDemo)
            .apply()
    }

    fun clear(context: Context) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().clear().apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_LOGIN, false)
    }

    fun isDemo(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_DEMO, false)
    }
}
