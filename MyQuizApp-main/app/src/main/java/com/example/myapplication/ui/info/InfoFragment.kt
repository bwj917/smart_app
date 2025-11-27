package com.example.myapplication.ui.info

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.auth.AuthManager
import com.example.myapplication.data.remote.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class InfoFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSetGoal = view.findViewById<MaterialButton>(R.id.btnSetGoal)
        btnSetGoal.setOnClickListener {
            showGoalSettingDialog()
        }


        val btnFrequent = view.findViewById<MaterialButton>(R.id.btnFrequentWrong)
        btnFrequent.setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.example.myapplication.ui.wrongnote.FrequentWrongActivity::class.java)
            startActivity(intent)
        }

        val btnMyNote = view.findViewById<MaterialButton>(R.id.btnMyNote)
        btnMyNote.setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.example.myapplication.ui.wrongnote.MyNoteActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showGoalSettingDialog() {
        val context = requireContext()
        val prefs = context.getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)

        // --- 1. 전체 레이아웃 ---
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(32), dpToPx(24), dpToPx(24))
        }

        // --- 2. 과목 선택 (드롭다운) ---
        val textInputLayout = TextInputLayout(context).apply {
            hint = "과목 선택"
            // 🔥 [핵심 수정] 드롭다운 모드 활성화 (화살표 아이콘 표시 및 클릭 동작)
            endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU

            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(dpToPx(12).toFloat(), dpToPx(12).toFloat(), dpToPx(12).toFloat(), dpToPx(12).toFloat())
            boxStrokeColor = Color.parseColor("#57419D")

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val courses = arrayOf("정보처리기능사", "컴활 1급 필기", "파이썬")
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, courses)

        val autoCompleteTV = MaterialAutoCompleteTextView(context).apply {
            inputType = InputType.TYPE_NULL // 키보드 숨김
            setAdapter(adapter)
            setText(courses[0], false)
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            background = null

            // 혹시라도 클릭이 안 될 경우를 대비한 강제 트리거
            setOnClickListener { showDropDown() }
        }

        textInputLayout.addView(autoCompleteTV)
        layout.addView(textInputLayout)


        // --- 3. 라벨 ---
        val tvLabel = TextView(context).apply {
            text = "일일 목표 개수"
            textSize = 14f
            setTextColor(Color.parseColor("#888888"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(32)
                bottomMargin = dpToPx(12)
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        layout.addView(tvLabel)


        // --- 4. 카운터 영역 (버튼 + 숫자) ---
        val counterLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        var currentSelectedCourse = courses[0]
        var currentGoal = prefs.getInt("GOAL_$currentSelectedCourse", 60)

        val tvCount = TextView(context).apply {
            text = "$currentGoal"
            textSize = 28f
            setTextColor(Color.parseColor("#333333"))
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F5F5F5"))
                cornerRadius = dpToPx(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(dpToPx(100), dpToPx(60)).apply {
                marginStart = dpToPx(16)
                marginEnd = dpToPx(16)
            }
        }

        fun createCounterButton(text: String): MaterialButton {
            return MaterialButton(context).apply {
                this.text = text
                textSize = 18f
                setTextColor(Color.WHITE)
                backgroundTintList = ColorStateList.valueOf(Color.parseColor("#57419D"))
                cornerRadius = dpToPx(12)
                stateListAnimator = null
                layoutParams = LinearLayout.LayoutParams(dpToPx(64), dpToPx(64))
            }
        }

        val btnMinus = createCounterButton("-5").apply {
            setOnClickListener {
                if (currentGoal > 5) {
                    currentGoal -= 5
                    tvCount.text = "$currentGoal"
                } else {
                    Toast.makeText(context, "최소 5개 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btnPlus = createCounterButton("+5").apply {
            setOnClickListener {
                if (currentGoal < 200) {
                    currentGoal += 5
                    tvCount.text = "$currentGoal"
                }
            }
        }

        counterLayout.addView(btnMinus)
        counterLayout.addView(tvCount)
        counterLayout.addView(btnPlus)
        layout.addView(counterLayout)


        // --- 5. 로직 연결 ---
        autoCompleteTV.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            currentSelectedCourse = selected
            currentGoal = prefs.getInt("GOAL_$selected", 60)
            tvCount.text = "$currentGoal"
        }


        // --- 6. 다이얼로그 띄우기 ---
        MaterialAlertDialogBuilder(context)
            .setTitle("목표 설정")
            .setView(layout)
            .setNegativeButton("취소") { d, _ -> d.dismiss() }
            .setPositiveButton("저장") { d, _ ->
                saveGoal(currentSelectedCourse, currentGoal)
                d.dismiss()
            }
            .show()
    }

    private fun saveGoal(courseTitle: String, goal: Int) {
        // 1. 로컬 저장
        val prefs = requireContext().getSharedPreferences("GoalPrefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("GOAL_$courseTitle", goal).apply()

        val userId = AuthManager.getUserId(requireContext())
        if (userId == null) {
            Toast.makeText(requireContext(), "설정이 기기에만 저장되었습니다. (로그인 필요)", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. 서버 저장
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.problemApiService.updateGoal(userId, courseTitle, goal)
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "$courseTitle 목표: ${goal}개 저장 완료!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "서버 저장 실패 (로컬엔 저장됨)", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}