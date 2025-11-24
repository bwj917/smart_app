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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class StatsFragment : Fragment() {

    private lateinit var chart: BarChart
    private lateinit var tvTitle: TextView

    private lateinit var tvTodaySolved: TextView
    private lateinit var tvTodayTime: TextView

    private lateinit var tvTotalSolved: TextView
    private lateinit var tvStudyTime: TextView
    private lateinit var tvBestDay: TextView

    private lateinit var tvHeaderTotalSolved: TextView
    private lateinit var tvHeaderTotalTime: TextView

    private var currentMode = "weekly"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chart = view.findViewById(R.id.mainChart)
        tvTitle = view.findViewById(R.id.tvTitle)

        tvTodaySolved = view.findViewById(R.id.tvTodaySolved)
        tvTodayTime = view.findViewById(R.id.tvTodayTime)

        tvTotalSolved = view.findViewById(R.id.tvTotalSolved)
        tvStudyTime = view.findViewById(R.id.tvStudyTime)
        tvBestDay = view.findViewById(R.id.tvBestDay)

        tvHeaderTotalSolved = view.findViewById(R.id.tvHeaderTotalSolved)
        tvHeaderTotalTime = view.findViewById(R.id.tvHeaderTotalTime)

        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)

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
                fetchStats(view, mode)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { v ->
            startRefresh(v)
        }
    }

    private fun startRefresh(view: View) {
        val userId = AuthManager.getUserId(requireContext()) ?: return

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
                delay(300)

                val service = RetrofitClient.problemApiService

                // 🔥 [핵심 수정] "all"을 보내서 전체 합계를 요청합니다.
                val todayResponse = service.getTodayStats(userId, "all")

                if (todayResponse.isSuccessful) {
                    val body = todayResponse.body()
                    val todayCount = (body?.get("solvedCount") as? Number)?.toInt() ?: 0
                    val todaySeconds = (body?.get("studyTime") as? Number)?.toLong() ?: 0L

                    tvTodaySolved.text = "${todayCount}문제"
                    tvTodayTime.text = formatSecondsToTime(todaySeconds)
                }

                // 전체 누적 기록
                val allStatsResponse = service.getAllStats(userId)
                if (allStatsResponse.isSuccessful) {
                    val body = allStatsResponse.body()
                    val rawCounts = body?.get("dailyCounts") as? List<*>
                    val counts = rawCounts?.map { (it as? Number)?.toInt() ?: 0 } ?: emptyList()

                    val totalAllSolved = counts.sum()
                    val totalAllSeconds = (body?.get("totalTimeSeconds") as? Number)?.toLong() ?: 0L

                    tvHeaderTotalSolved.text = "${totalAllSolved}문제"
                    tvHeaderTotalTime.text = formatSecondsToTime(totalAllSeconds)

                    if (mode == "all") {
                        updateUI(counts, mode, totalAllSeconds)
                    } else {
                        fetchStats(view, mode)
                    }
                }

            } catch (e: Exception) {
                Log.e("Stats", "Refresh Error: ${e.message}")
            }
        }
    }

    private fun fetchStats(view: View, mode: String) {
        val userId = AuthManager.getUserId(requireContext()) ?: return
        currentMode = mode

        lifecycleScope.launch {
            try {
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
                    val counts = rawCounts?.map { (it as? Number)?.toInt() ?: 0 } ?: emptyList()

                    val timeKey = if (mode == "all") "totalTimeSeconds" else "periodTimeSeconds"
                    val periodSeconds = (body?.get(timeKey) as? Number)?.toLong() ?: 0L

                    updateUI(counts, mode, periodSeconds)
                }
            } catch (e: Exception) {
                Log.e("Stats", "Fetch Error", e)
            }
        }
    }

    private fun updateUI(counts: List<Int>, mode: String, periodSeconds: Long) {
        val totalSolved = counts.sum()
        val best = counts.maxOrNull() ?: 0
        val timeString = formatSecondsToTime(periodSeconds)

        tvTotalSolved.text = "${totalSolved}문제"
        tvStudyTime.text = timeString

        val bestText = when (mode) {
            "yearly" -> "${best}"
            "all" -> "${best}"
            else -> "${best}"
        }
        tvBestDay.text = "${bestText}문제"

        setupChart(counts, mode)
    }

    private fun setupChart(counts: List<Int>, mode: String) {
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
        } else if (mode == "weekly") {
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
        } else if (mode == "monthly") {
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
            counts.forEachIndexed { i, v ->
                entries.add(BarEntry(i.toFloat(), v.toFloat()))
                labels.add("${i + 1}")
                tooltipLabels.add("${currentMonth}월 ${i + 1}일")
            }
        } else {
            counts.forEachIndexed { i, v ->
                entries.add(BarEntry(i.toFloat(), v.toFloat()))
                labels.add("${i + 1}월")
                tooltipLabels.add("${i + 1}월")
            }
        }

        val dataSet = BarDataSet(entries, "학습량")
        dataSet.color = Color.parseColor("#57419d")
        dataSet.highLightColor = Color.parseColor("#FFD700")
        dataSet.setDrawValues(false)
        val barData = BarData(dataSet)
        barData.barWidth = 0.5f
        chart.data = barData

        val marker = CustomMarkerView(requireContext(), R.layout.view_custom_marker, tooltipLabels)
        marker.chartView = chart
        chart.marker = marker

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
                axisMinimum = 0f
            }
            axisRight.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.DKGRAY
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
                if (mode == "monthly") setLabelCount(6, false)
                else setLabelCount(labels.size, false)
            }
        }
        chart.invalidate()
        chart.animateY(600)
    }

    private fun formatSecondsToTime(totalSeconds: Long): String {
        if (totalSeconds <= 0L) return "0분"
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return if (hours > 0) "${hours}시간 ${minutes}분" else "${minutes}분"
    }
}