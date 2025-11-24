package com.example.myapplication.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class CircleProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE6E6E6.toInt()   // 바탕 링
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 12f
    }
    private val progPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6A5AE0.toInt()   // 진행 링 색
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 12f
    }

    private val oval = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val pad = progPaint.strokeWidth / 2f + 2f
        oval.set(
            (width - size) / 2f + pad,
            (height - size) / 2f + pad,
            (width + size) / 2f - pad,
            (height + size) / 2f - pad
        )
        // 바탕 원
        canvas.drawArc(oval, 0f, 360f, false, basePaint)
        // 진행 원(위쪽부터 시계방향)
        val sweep = 360f * (progress / 100f)
        canvas.drawArc(oval, -90f, sweep, false, progPaint)
    }
}