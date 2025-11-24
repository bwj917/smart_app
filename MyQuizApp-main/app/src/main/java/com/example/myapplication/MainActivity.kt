package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.auth.AuthManager
import com.example.myapplication.auth.LoginActivity
import com.example.myapplication.auth.SignUpActivity
import com.example.myapplication.ui.home.HomeFragment
import com.example.myapplication.ui.stats.StatsFragment
import com.example.myapplication.ui.info.InfoFragment
import com.example.myapplication.ui.wrongnote.WrongNoteActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navView: NavigationView
    private lateinit var bottomNav: BottomNavigationView

    // 🔥 [수정] 프래그먼트 인스턴스를 변수에 저장해두고 재사용합니다.
    private val homeFragment by lazy { HomeFragment() }
    private val statsFragment by lazy { StatsFragment() }
    private val infoFragment by lazy { InfoFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val drawer = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawerLayout)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        navView = findViewById(R.id.navigationView)
        bottomNav = findViewById(R.id.bottomNav)

        toolbar.setNavigationIcon(android.R.drawable.ic_menu_sort_by_size)
        toolbar.setNavigationOnClickListener { drawer.openDrawer(GravityCompat.START) }

        // 초기 화면 설정
        if (savedInstanceState == null) {
            showFragment(homeFragment) // changeFragment -> showFragment 변경
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showFragment(homeFragment)
                    toolbar.title = "코딩 퀴즈"
                    true
                }
                R.id.nav_study -> {
                    showFragment(statsFragment)
                    toolbar.title = "학습 통계"
                    true
                }
                R.id.nav_quiz -> {
                    showFragment(infoFragment)
                    toolbar.title = "학습 정보"
                    true
                }
                else -> false
            }
        }

        updateSideMenu()
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_login -> startActivity(Intent(this, LoginActivity::class.java))
                R.id.action_logout -> logoutUser()
                R.id.action_monthly_study -> {
                    showFragment(statsFragment)
                    bottomNav.selectedItemId = R.id.nav_study
                }
                R.id.action_wrong_notes -> startActivity(Intent(this, WrongNoteActivity::class.java))
                R.id.action_signup -> startActivity(Intent(this, SignUpActivity::class.java))
            }
            drawer.closeDrawer(GravityCompat.START)
            true
        }
    }

    // 🔥 [핵심 수정] 화면을 파괴하지 않고 숨겼다 보여주는 함수
    private fun showFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()

        // 1. 기존에 추가된 모든 프래그먼트를 숨김
        if (homeFragment.isAdded) transaction.hide(homeFragment)
        if (statsFragment.isAdded) transaction.hide(statsFragment)
        if (infoFragment.isAdded) transaction.hide(infoFragment)

        // 2. 선택한 프래그먼트가 아직 추가 안 됐으면 추가(Add), 이미 있으면 보여주기(Show)
        if (!fragment.isAdded) {
            transaction.add(R.id.fragment_container, fragment)
        } else {
            transaction.show(fragment)
        }
        transaction.commit()
    }

    override fun onResume() {
        super.onResume()
        updateSideMenu()
    }

    private fun updateSideMenu() {
        val isLoggedIn = AuthManager.isLoggedIn(this)
        val menu = navView.menu
        menu.findItem(R.id.action_login)?.isVisible = !isLoggedIn
        menu.findItem(R.id.action_signup)?.isVisible = !isLoggedIn
        menu.findItem(R.id.action_logout)?.isVisible = isLoggedIn
    }

    private fun logoutUser() {
        AuthManager.logout(this)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}