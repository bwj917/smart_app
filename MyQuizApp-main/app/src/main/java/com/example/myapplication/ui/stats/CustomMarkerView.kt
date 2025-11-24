package com.example.myapplication.ui.stats

import android.content.Context
import android.widget.TextView
import com.example.myapplication.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

// 🔥 [수정] 생성자에서 받는 변수명을 labels -> tooltipLabels로 변경 (의미 명확화)
class CustomMarkerView(context: Context, layoutResource: Int, private val tooltipLabels: List<String>) : MarkerView(context, layoutResource) {

    private val tvContent: TextView = findViewById(R.id.tvContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) return

        val index = e.x.toInt()
        // 🔥 [핵심] 전달받은 긴 날짜 문자열("11월 23일")을 가져옴
        val dateText = if (index >= 0 && index < tooltipLabels.size) tooltipLabels[index] else ""
        val value = e.y.toInt()

        // 🔥 [형식 적용] "11월 23일 : 5문제"
        tvContent.text = "$dateText : ${value}문제"

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // 말풍선 중심을 터치 지점 바로 위로 맞춤
        return MPPointF(-(width / 2).toFloat(), -height.toFloat() - 20)
    }
}