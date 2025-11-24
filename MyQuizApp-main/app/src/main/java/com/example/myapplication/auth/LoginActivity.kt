package com.example.myapplication.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.MainActivity
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { performLogin() }



        binding.tvGoSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun performLogin() {
        val id = binding.etId.text.toString().trim()
        val pw = binding.etPassword.text.toString().trim()

        // 1. 입력값 검사
        if (id.isEmpty() || pw.isEmpty()) {
            Toast.makeText(this, "아이디와 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. 서버로 로그인 요청 (비동기 실행)
        lifecycleScope.launch {
            try {
                // 🔥 AuthSuccessResponse DTO를 기대
                val response = RetrofitClient.authApiService.login(id, pw)

                // 3. 응답 처리
                if (response.isSuccessful) {
                    // 성공 (200 OK)
                    val authResponse = response.body()
                    if (authResponse?.userId != null) {
                        Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()
                        // 내부 저장소에 유저 ID와 로그인 상태 저장
                        AuthManager.setLoggedIn(this@LoginActivity, true, authResponse.userId)

                        // 메인 화면으로 이동
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // 서버가 200 OK를 보냈지만 body에 userId가 없을 경우 (서버 오류 가능성)
                        Toast.makeText(this@LoginActivity, "로그인 성공했으나 유저 정보를 받지 못했습니다.", Toast.LENGTH_LONG).show()
                    }

                } else {
                    // 실패 (4xx, 5xx) -> 아이디/비번 틀림 또는 서버 비즈니스 예외
                    val errorMsg = response.errorBody()?.string() ?: "로그인 실패"
                    Toast.makeText(this@LoginActivity, "실패: 아이디 또는 비밀번호를 확인하세요. (오류: $errorMsg)", Toast.LENGTH_LONG).show()
                    Log.e("LoginError", errorMsg)
                }

            } catch (e: Exception) {
                // 네트워크 오류, Gson 파싱 오류 등 (주로 MalformedJsonException)
                e.printStackTrace()
                Toast.makeText(this@LoginActivity, "통신 오류 또는 응답 처리 실패: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
