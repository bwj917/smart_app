package com.example.demo.domain;

import com.example.demo.dto.ProblemResponseDto; // DTO 임포트 확인

public class SubmissionResponse {
    private boolean isCorrect;
    private ProblemResponseDto updatedProblem; // Problem -> ProblemResponseDto로 변경됨

    // 기본 생성자 (JSON 라이브러리용)
    public SubmissionResponse() {}

    // 생성자
    public SubmissionResponse(boolean isCorrect, ProblemResponseDto updatedProblem) {
        this.isCorrect = isCorrect;
        this.updatedProblem = updatedProblem;
    }

    // Getter
    public boolean isCorrect() {
        return isCorrect;
    }

    public ProblemResponseDto getUpdatedProblem() {
        return updatedProblem;
    }

    // Setter (필요할 경우)
    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }

    public void setUpdatedProblem(ProblemResponseDto updatedProblem) {
        this.updatedProblem = updatedProblem;
    }
}