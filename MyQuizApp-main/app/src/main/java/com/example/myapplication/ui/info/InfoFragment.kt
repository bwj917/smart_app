package com.example.myapplication.ui.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class InfoFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = TextView(context)
        view.text = "학습 정보 화면입니다"
        view.gravity = android.view.Gravity.CENTER
        view.textSize = 24f
        return view
    }
}