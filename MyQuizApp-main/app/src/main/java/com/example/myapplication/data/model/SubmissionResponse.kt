package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

data class SubmissionResponse(
    @SerializedName("correct")
    val isCorrect: Boolean,
     val updatedProblem: Problem?
)