package com.example.myapplication.ui.quiz

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.google.android.material.card.MaterialCardView
class QuizFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_quiz, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val choice1 = view.findViewById<MaterialCardView>(R.id.choice1)
        val tvChoice1 = view.findViewById<TextView>(R.id.tvChoice1)

        choice1.setOnClickListener {
            setChoiceCorrect(choice1, tvChoice1)
        }
    }

    private fun setChoiceCorrect(card: MaterialCardView, text: TextView) {
        card.setCardBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.brand_light_bg
            )
        )
        card.strokeWidth = 0
        text.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_primary))
        val check = ContextCompat.getDrawable(requireContext(), R.drawable.ic_check_bold_purple)
        text.setCompoundDrawablesWithIntrinsicBounds(check, null, null, null)
        text.compoundDrawablePadding = (8 * resources.displayMetrics.density).toInt()
    }

    private fun setChoiceDefault(card: MaterialCardView, text: TextView) {
        card.setCardBackgroundColor(Color.WHITE)
        card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
        card.strokeColor = Color.parseColor("#E5E5E5")
        text.setTextColor(Color.parseColor("#222222"))
        text.setCompoundDrawables(null, null, null, null)
    }
}