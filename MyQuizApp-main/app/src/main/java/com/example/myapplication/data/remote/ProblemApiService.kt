package com.example.myapplication.data.remote

import com.example.myapplication.data.model.HintResponse
import com.example.myapplication.data.model.Problem
import com.example.myapplication.data.model.SubmissionRequest
import com.example.myapplication.data.model.SubmissionResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ProblemApiService {

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

    // 🔥 [수정] 이름 변경: getTodayStats / 반환 타입: Map<String, Any>
    // Any를 쓰기 위해선 JvmSuppressWildcards가 필요할 수도 있지만, 보통 Map<String, Any>는 Retrofit에서 잘 처리됩니다.
    @GET("api/stats/today")
    suspend fun getTodayStats(
        @Query("userId") userId: Long,
        @Query("courseId") courseId: String
    ): Response<Map<String, Any>>

    @GET("api/stats/weekly")
    suspend fun getWeeklyStats(@Query("userId") userId: Long): Response<Map<String, Any>>

    @GET("api/stats/monthly")
    suspend fun getMonthlyStats(@Query("userId") userId: Long): Response<Map<String, Any>>

    @GET("api/stats/yearly")
    suspend fun getYearlyStats(@Query("userId") userId: Long): Response<Map<String, Any>>

    @GET("api/stats/all")
    suspend fun getAllStats(@Query("userId") userId: Long): Response<Map<String, Any>>


    @POST("api/stats/reward")
    suspend fun rewardPoints(
        @Query("userId") userId: Long,
        @Query("amount") amount: Int
    ): Response<Map<String, Any>>

    @POST("api/stats/buy-character")
    suspend fun buyCharacter(
        @Query("userId") userId: Long,
        @Query("characterIdx") characterIdx: Int,
        @Query("price") price: Int
    ): Response<Map<String, Any>>

    @POST("api/stats/equip-character")
    suspend fun equipCharacter(
        @Query("userId") userId: Long,
        @Query("characterIdx") characterIdx: Int
    ): Response<ResponseBody>

    @POST("api/stats/goal")
    suspend fun updateGoal(
        @Query("userId") userId: Long,
        @Query("courseName") courseName: String,
        @Query("goal") goal: Int
    ): Response<String>

    // 🔥 [신규] 내 목표 가져오기 API
    @GET("api/stats/goals")
    suspend fun getUserGoals(
        @Query("userId") userId: Long
    ): Response<Map<String, Int>>

    @GET("api/problems/frequent-wrong")
    suspend fun getFrequentWrongProblems(
        @Query("userId") userId: Long,
        @Query("courseId") courseId: String // 🔥 파라미터 추가
    ): Response<List<Problem>>

    @POST("api/problems/scrap")
    suspend fun scrapProblem(
        @Query("userId") userId: Long,
        @Query("problemId") problemId: Long
    ): Response<Boolean>

    @GET("api/problems/scrapped")
    suspend fun getScrappedProblems(
        @Query("userId") userId: Long,
        @Query("courseId") courseId: String
    ): Response<List<Problem>>
}
