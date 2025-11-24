// fileName: RetrofitClient.kt

package com.example.myapplication.data.remote

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"

    // 🔥 수정: Gson 객체들을 먼저 명시적으로 정의/초기화합니다.

    // 💡 1. 날짜 형식을 지정한 Gson 생성 (Problem API용)
    private val problemGson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        // 만약 서버가 UTC로 보낸다면 아래 주석 해제
        // .setTimeZone(TimeZone.getTimeZone("UTC"))
        .create()

    // 💡 2. Auth API를 위한 Gson 객체 생성 (일반적인 JSON 처리를 위해)
    private val authGson = GsonBuilder().create()

    val problemApiService: ProblemApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            // 🔥 problemGson 사용
            .addConverterFactory(GsonConverterFactory.create(problemGson))
            .build()
            .create(ProblemApiService::class.java)
    }

    val authApiService: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            // 🔥 authGson 사용
            .addConverterFactory(GsonConverterFactory.create(authGson))
            .build()
            .create(AuthApiService::class.java)
    }
}