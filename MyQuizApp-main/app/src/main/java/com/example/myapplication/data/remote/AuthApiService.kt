package com.example.myapplication.data.remote

import com.example.myapplication.data.model.AuthSuccessResponse // 🔥 추가
import com.example.myapplication.data.model.IdCheckResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApiService {
    @GET("api/members/check-id")
    suspend fun checkId(@Query("loginId") loginId: String): Response<IdCheckResponse>

    @FormUrlEncoded
    @POST("api/email/send-code")
    suspend fun sendEmailCode(@Field("email") email: String): Response<ResponseBody>

    @FormUrlEncoded
    @POST("api/email/verify-code")
    suspend fun verifyEmailCode(
        @Field("email") email: String,
        @Field("verificationCode") verificationCode: String
    ): Response<ResponseBody>

    @FormUrlEncoded // 회원가입 (성공 시 JSON 응답을 기대)
    @POST("/register-process")
    suspend fun registerMember(
        @Field("userid") userid: String,
        @Field("pw") pw: String,
        @Field("name") name: String,
        @Field("email") email: String,
        @Field("phone") phone: String
        // 응답 타입은 유지 (성공 시)
    ): Response<AuthSuccessResponse>

    @FormUrlEncoded // 로그인 (성공 시 JSON 응답을 기대)
    @POST("/api/login")
    suspend fun login(
        @Field("userid") userid: String,
        @Field("pw") pw: String
        // 응답 타입은 유지 (성공 시)
    ): Response<AuthSuccessResponse>
}