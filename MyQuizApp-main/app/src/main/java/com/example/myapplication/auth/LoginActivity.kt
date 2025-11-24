package com.example.myapplication.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
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

        // 1. 저장된 아이디가 있으면 채워넣기
        val savedId = AuthManager.getSavedIdForDisplay(this)
        if (!savedId.isNullOrEmpty()) {
            binding.etId.setText(savedId)
            binding.cbSaveId.isChecked = true
        }

        // 2. 로그인 유지 확인
        if (AuthManager.isLoggedIn(this)) {
            moveToMain()
        }

        // 3. 버튼 클릭 리스너
        binding.btnLogin.setOnClickListener { performLogin() }

        // 4. 회원가입 화면 이동
        binding.tvGoSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        // 5. 🔥 [추가] 비밀번호 입력 후 엔터(Done) 키 누르면 로그인 실행
        binding.etPassword.setOnEditorActionListener { _, actionId, event ->
            val isEnterKey = (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)

            if (actionId == EditorInfo.IME_ACTION_DONE || isEnterKey) {
                performLogin()
                true // 이벤트 소비 (키보드 내려감 등 후속 동작 제어)
            } else {
                false
            }
        }
    }

    private fun performLogin() {
        val id = binding.etId.text.toString().trim()
        val pw = binding.etPassword.text.toString().trim()

        val isKeepLogin = binding.cbKeepLogin.isChecked
        val isSaveId = binding.cbSaveId.isChecked

        if (id.isEmpty() || pw.isEmpty()) {
            Toast.makeText(this, "아이디와 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.authApiService.login(id, pw)

                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse?.userId != null) {
                        Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()

                        // 아이디 저장 처리
                        AuthManager.setSavedIdForDisplay(this@LoginActivity, id, isSaveId)

                        // 로그인 유지 처리
                        AuthManager.setLoggedIn(this@LoginActivity, authResponse.userId, isKeepLogin)

                        moveToMain()
                    } else {
                        Toast.makeText(this@LoginActivity, "유저 정보를 받지 못했습니다.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "로그인 실패"
                    Toast.makeText(this@LoginActivity, "아이디 또는 비밀번호를 확인하세요.", Toast.LENGTH_LONG).show()
                    Log.e("LoginError", errorMsg)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@LoginActivity, "통신 오류: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun moveToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}