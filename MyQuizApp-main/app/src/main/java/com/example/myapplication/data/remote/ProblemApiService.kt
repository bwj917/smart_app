package com.example.myapplication.data.remote

import com.example.myapplication.data.model.HintResponse
import com.example.myapplication.data.model.Problem
import com.example.myapplication.data.model.SubmissionRequest
import com.example.myapplication.data.model.SubmissionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ProblemApiService {

    // ... (기존 문제 관련 API들 유지) ...
    @GET("api/problems/tenProblem")
    suspend fun getTenProblems(
        @Query("userId") userId: Long,
        @Query("courseId") courseId: String
    ): Response<List<Problem>>

    @POST("api/problems/submit")
    suspend fun submitAnswer(@Body request: SubmissionRequest): Response<SubmissionResponse>

    @GET("api/problems/hint/{problemId}/{hintCount}")
    suspend fun getHint(
        @Path("problemId") problemId: Long,
        @Path("hintCount") hintCount: Int,
        @Query("userId") userId: Long
    ): Response<HintResponse>


    // ------------- 통계 API -------------

    @GET("api/stats/today")
    suspend fun getTodaySolvedCount(
        @Query("userId") userId: Long,
        @Query("courseId") courseId: String
    ): Response<Map<String, Int>>

    // 🔥 [수정] getAllStats, getWeeklyStats 등을 모두 Map<String, Any>로 통일하면 안전합니다.
    // (JvmSuppressWildcards는 Any 타입 사용 시 필수일 수 있음)

    @GET("api/stats/weekly")
    suspend fun getWeeklyStats(@Query("userId") userId: Long): Response<Map<String, Any>>

    @GET("api/stats/monthly")
    suspend fun getMonthlyStats(@Query("userId") userId: Long): Response<Map<String, Any>>

    @GET("api/stats/yearly")
    suspend fun getYearlyStats(@Query("userId") userId: Long): Response<Map<String, Any>>

    // 🔥 여기도 Any로 변경 (리스트와 Long이 섞여 있으므로)
    @GET("api/stats/all")
    suspend fun getAllStats(@Query("userId") userId: Long): Response<Map<String, Any>>
}