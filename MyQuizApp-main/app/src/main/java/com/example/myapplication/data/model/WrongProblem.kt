package com.example.myapplication.data.model
import java.io.Serializable

data class WrongProblem(
    val question: String,
    val yourAnswer: String,
    val correctAnswer: String
) : Serializable