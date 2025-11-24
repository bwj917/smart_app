package com.example.demo.domain; // 또는 com.example.demo.dto

public class HintResponse {
    private String hintText;

    public HintResponse(String hintText) {
        this.hintText = hintText;
    }

    // Jackson이 JSON으로 직렬화할 때 사용합니다.
    public String getHintText() {
        return hintText;
    }
}