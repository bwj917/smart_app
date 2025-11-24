package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.auth.LoginActivity
import com.example.myapplication.auth.SignUpActivity
import com.example.myapplication.ui.home.HomeFragment
import com.example.myapplication.ui.stats.StatsFragment // 🔥 import 확인
import com.example.myapplication.ui.info.InfoFragment   // 🔥 import 확인
import com.example.myapplication.ui.wrongnote.WrongNoteActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navView: NavigationView
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. 뷰 찾기
        val drawer = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawerLayout)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        navView = findViewById(R.id.navigationView)
        bottomNav = findViewById(R.id.bottomNav) // 🔥 하단바 연결

        // 2. 툴바 & 드로어 설정
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_sort_by_size)
        toolbar.setNavigationOnClickListener { drawer.openDrawer(GravityCompat.START) }

        // 3. 초기 화면 설정 (앱 켜면 홈 화면이 보이게)
        if (savedInstanceState == null) {
            changeFragment(HomeFragment())
        }

        // 4. 🔥 [핵심] 하단 탭 클릭 리스너
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    changeFragment(HomeFragment())
                    toolbar.title = "코딩 퀴즈" // 툴바 제목도 바꿔주면 좋아요
                    true
                }
                R.id.nav_study -> {
                    changeFragment(StatsFragment())
                    toolbar.title = "학습 통계"
                    true
                }
                R.id.nav_quiz -> { // 메뉴 XML에 있는 ID가 nav_quiz라고 가정
                    changeFragment(InfoFragment())
                    toolbar.title = "학습 정보"
                    true
                }
                else -> false
            }
        }

        // 5. 사이드 메뉴 설정 (기존 유지)
        updateSideMenu()
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_login -> startActivity(Intent(this, LoginActivity::class.java))
                R.id.action_logout -> logoutUser()
                R.id.action_monthly_study -> {
                    // 사이드 메뉴에서도 통계 누르면 이동하게 연결
                    changeFragment(StatsFragment())
                    bottomNav.selectedItemId = R.id.nav_study // 하단바도 같이 선택됨
                }
                R.id.action_wrong_notes -> startActivity(Intent(this, WrongNoteActivity::class.java))
                R.id.action_signup -> startActivity(Intent(this, SignUpActivity::class.java))
            }
            drawer.closeDrawer(GravityCompat.START)
            true
        }
    }

    // 🔥 프래그먼트를 교체하는 함수 (편리함)
    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment) // XML에 있는 컨테이너 ID
            .commit()
    }

    override fun onResume() {
        super.onResume()
        updateSideMenu()
    }

    private fun updateSideMenu() {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        // 로그인 여부에 따라 메뉴 갱신 로직 등...
    }

    private fun logoutUser() {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("isLogged_in", false).apply()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}