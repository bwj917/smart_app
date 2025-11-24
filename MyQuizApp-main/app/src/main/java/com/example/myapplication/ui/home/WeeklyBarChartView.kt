package com.example.myapplication.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class WeeklyBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6A5AE0.toInt() // 보라 톤 (원하는 색으로)
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x22000000.toInt() // 아주 연한 검정(투명)
        strokeWidth = 2f
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66000000.toInt() // 반투명 라벨
        textSize = 28f
        color = 0xFF9E9E9E.toInt()
    }

    var values: List<Int> = emptyList()
        set(v) {
            field = v
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return

        val density = resources.displayMetrics.density
        val sideMarginPx = 16f * density // 좌우 마진 16dp
        val padL = paddingLeft + sideMarginPx.toInt()
        val padR = paddingRight + sideMarginPx.toInt()
        val padT = paddingTop
        val labelExtra = (labelPaint.textSize + 12f).toInt()
        val padB = paddingBottom + labelExtra

        val w = width - padL - padR
        val h = height - padT - padB
        val baseY = (height - padB).toFloat()

        // 바닥선
        canvas.drawLine(
            padL.toFloat(), baseY - 1f,
            (width - padR).toFloat(), baseY - 1f,
            gridPaint
        )

        val count = values.size
        val spacingFactor = 2.0f // 1.8f → 2.0f로 간격 넓힘
        val barSpace = w / (count * spacingFactor)
        val barWidth = barSpace // 필요하면 barWidth를 0.9f*barSpace 등으로 더 줄여도 됨
        var x = padL + barSpace / 2f

        val days = arrayOf("일","월","화","수","목","금","토")
        val maxV = (values.maxOrNull() ?: 1).coerceAtLeast(1)

        values.forEachIndexed { idx, v ->
            val ratio = v.toFloat() / maxV
            val barH = ratio * (h * 0.9f)
            val left = x
            val top = baseY - barH
            val right = x + barWidth
            val bottom = baseY

            canvas.drawRoundRect(left, top, right, bottom, barWidth/3f, barWidth/3f, barPaint)

            val day = days[idx % 7]
            val tw = labelPaint.measureText(day)
            canvas.drawText(
                day,
                left + (barWidth - tw) / 2f,
                baseY + labelPaint.textSize + 6f,
                labelPaint
            )

            x += (barWidth + barSpace)
        }
    }
}