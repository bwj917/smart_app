package com.example.myapplication.ui.stats

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.auth.AuthManager
import com.example.myapplication.data.remote.RetrofitClient
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class StatsFragment : Fragment() {

    private lateinit var chart: BarChart
    private lateinit var tvTitle: TextView
    private var currentMode = "weekly"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvTitle = view.findViewById(R.id.tvTitle)
        chart = view.findViewById(R.id.mainChart)
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)

        // 1. 버튼 클릭 리스너 설정
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val mode = when (checkedId) {
                    R.id.btnWeekly -> "weekly"
                    R.id.btnMonthly -> "monthly"
                    R.id.btnYearly -> "yearly"
                    R.id.btnAll -> "all"
                    else -> "weekly"
                }
                currentMode = mode
                // 탭 변경 시에는 즉시 로드 시작
                fetchStats(view, mode)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { v ->
            // 🔥 [핵심] 뷰가 포그라운드에 올 때마다 단일 진입점에서 로드 시작
            startRefresh(v)
        }
    }

    // 🔥 [통합 함수] DB 커밋 대기 및 상단/차트 데이터 갱신을 순차적으로 처리
    private fun startRefresh(view: View) {
        val userId = AuthManager.getUserId(requireContext()) ?: return

        // 현재 선택된 모드 파악 (버튼 상태)
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)
        val mode = when (toggleGroup.checkedButtonId) {
            R.id.btnWeekly -> "weekly"
            R.id.btnMonthly -> "monthly"
            R.id.btnYearly -> "yearly"
            R.id.btnAll -> "all"
            else -> "weekly"
        }
        currentMode = mode

        lifecycleScope.launch {
            try {
                // 1. 🔥 [COMMIT 대기] 딜레이를 1.5초로 늘려 서버 커밋 완료를 확실히 보장합니다.
                delay(1500)

                // 2. 상단 누적 통계 API 호출 (/all)
                val allStatsResponse = RetrofitClient.problemApiService.getAllStats(userId)

                if (allStatsResponse.isSuccessful) {
                    val body = allStatsResponse.body()

                    // Header 데이터 추출 및 계산
                    val rawCounts = body?.get("dailyCounts") as? List<*>
                    val counts = rawCounts?.filterIsInstance<Number>()?.map { it.toInt() } ?: emptyList()
                    val totalSolved = counts.sum() // 누적 총 문제 수

                    val totalSeconds = (body?.get("totalTimeSeconds") as? Number)?.toLong() ?: 0L
                    val timeString = formatSecondsToTime(totalSeconds)

                    // Header UI 업데이트
                    view.findViewById<TextView>(R.id.tvHeaderTotalSolved).text = "${totalSolved}문제"
                    view.findViewById<TextView>(R.id.tvHeaderTotalTime).text = timeString

                    // 3. 차트 데이터 갱신 (선택된 모드가 '전체'일 경우, 이미 받은 데이터를 사용)
                    if (mode == "all") {
                        updateUI(view, counts, mode, totalSeconds)
                    } else {
                        // 4. 다른 모드인 경우, 해당 기간의 API를 호출하여 최신 데이터로 갱신
                        fetchStats(view, mode)
                    }
                }

            } catch (e: Exception) {
                Log.e("Stats", "Load/Sync Error: ${e.message}", e)
            }
        }
    }


    // 🔥 [재활용 함수] 하단 상세 요약 + 차트 (버튼 클릭 및 startRefresh에서 호출)
    private fun fetchStats(view: View, mode: String) {
        val userId = AuthManager.getUserId(requireContext()) ?: return
        currentMode = mode

        lifecycleScope.launch {
            try {
                // 🔥 [딜레이 제거] startRefresh에서 이미 딜레이를 충분히 했으므로 여기서는 제거합니다.
                val service = RetrofitClient.problemApiService
                val response = when(mode) {
                    "weekly" -> service.getWeeklyStats(userId)
                    "monthly" -> service.getMonthlyStats(userId)
                    "yearly" -> service.getYearlyStats(userId)
                    "all" -> service.getAllStats(userId)
                    else -> service.getAllStats(userId)
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    val rawCounts = body?.get("dailyCounts") as? List<*>
                    val counts = rawCounts?.filterIsInstance<Number>()?.map { it.toInt() } ?: emptyList()

                    val timeKey = if (mode == "all") "totalTimeSeconds" else "periodTimeSeconds"
                    val periodSeconds = (body?.get(timeKey) as? Number)?.toLong() ?: 0L

                    updateUI(view, counts, mode, periodSeconds)
                }
            } catch (e: Exception) {
                Log.e("Stats", "Fetch Error", e)
            }
        }
    }

    private fun updateUI(view: View, counts: List<Int>, mode: String, periodSeconds: Long) {
        val totalSolved = counts.sum()
        val best = counts.maxOrNull() ?: 0

        val timeString = formatSecondsToTime(periodSeconds)

        // 하단 요약 업데이트
        view.findViewById<TextView>(R.id.tvTotalSolved).text = "${totalSolved}문제"
        view.findViewById<TextView>(R.id.tvStudyTime).text = timeString // 기간별 학습시간

        // 최고 기록 표시 (모드별)
        if (mode == "yearly") {
            view.findViewById<TextView>(R.id.tvBestDay).text = "최고의 달: ${best}문제"
        } else if (mode == "all") {
            view.findViewById<TextView>(R.id.tvBestDay).text = "최고의 해: ${best}문제"
        } else {
            view.findViewById<TextView>(R.id.tvBestDay).text = "최고 기록: ${best}문제"
        }

        // 제목
        tvTitle.text = when(mode) {
            "weekly" -> "주간 상세 요약"
            "monthly" -> "월간 상세 요약"
            "yearly" -> "연간 상세 요약"
            else -> "전체 상세 요약"
        }

        // 3. 차트 그리기 준비 (entries, labels, tooltipLabels 로직 유지)
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val tooltipLabels = ArrayList<String>()

        if (mode == "all") {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val startYear = currentYear - 4
            counts.forEachIndexed { i, v ->
                entries.add(BarEntry(i.toFloat(), v.toFloat()))
                val year = startYear + i
                labels.add("$year")
                tooltipLabels.add("${year}년")
            }
        }
        else if (mode == "weekly") {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -6)
            val xFmt = SimpleDateFormat("MM/dd", Locale.getDefault())
            val toolFmt = SimpleDateFormat("M월 d일", Locale.getDefault())
            for (i in counts.indices) {
                entries.add(BarEntry(i.toFloat(), counts[i].toFloat()))
                labels.add(xFmt.format(calendar.time))
                tooltipLabels.add(toolFmt.format(calendar.time))
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        else if (mode == "monthly") {
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
            counts.forEachIndexed { i, v ->
                entries.add(BarEntry(i.toFloat(), v.toFloat()))
                labels.add("${i + 1}")
                tooltipLabels.add("${currentMonth}월 ${i + 1}일")
            }
        }
        else {
            counts.forEachIndexed { i, v ->
                entries.add(BarEntry(i.toFloat(), v.toFloat()))
                labels.add("${i + 1}월")
                tooltipLabels.add("${i + 1}월")
            }
        }

        // 4. 차트 적용 (스타일링 로직 유지)
        val dataSet = BarDataSet(entries, "학습량")
        dataSet.color = Color.parseColor("#57419d")
        dataSet.highLightColor = Color.parseColor("#FFD700")
        dataSet.setDrawValues(false)
        val barData = BarData(dataSet)
        barData.barWidth = 0.5f
        chart.data = barData
        // CustomMarkerView 연결 로직 (CustomMarkerView 파일이 별도로 필요함)

        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            axisLeft.apply {
                isEnabled = true
                setDrawLabels(false)
                setDrawAxisLine(false)
                setDrawGridLines(true)
                gridColor = Color.parseColor("#F0F0F0")
            }
            axisRight.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.DKGRAY
                setDrawGridLines(true)
                gridColor = Color.parseColor("#F0F0F0")
                granularity = 1f
                valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
                if (mode == "monthly") setLabelCount(6, false)
                else setLabelCount(labels.size, false)
            }
        }
        chart.invalidate()
        chart.animateY(600)
    }

    // 시간 변환 헬퍼 함수
    private fun formatSecondsToTime(totalSeconds: Long): String {
        if (totalSeconds == 0L) return "0분"
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return if (hours > 0) "${hours}시간 ${minutes}분" else "${minutes}분"
    }
}