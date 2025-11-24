package com.example.demo.dto;

import com.example.demo.domain.Problem;
import com.example.demo.domain.UserProblemStats;
import java.util.Date;

public class ProblemResponseDto {

    private Long problemId;
    private String question; // title -> question 변경 (Entity, Android와 통일)
    private String answer;   // answer 추가

    // 안드로이드 Problem.kt에 맞춰 통계 정보를 펼쳐서 보냄
    private Integer problemLevel;
    private Date nextReviewTime;

    public ProblemResponseDto(Problem problem, UserProblemStats stats) {
        this.problemId = problem.getProblemId();
        // 🔥 [수정] 없는 getTitle() 대신 getQuestion() 사용
        this.question = problem.getQuestion();
        this.answer = problem.getAnswer();

        if (stats != null) {
            this.problemLevel = stats.getProblemLevel();
            this.nextReviewTime = stats.getNextReviewTime();
        } else {
            // 통계가 없으면(처음 푸는 문제) 기본값
            this.problemLevel = 0;
            this.nextReviewTime = null;
        }
    }

    // Getters
    public Long getProblemId() { return problemId; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public Integer getProblemLevel() { return problemLevel; }
    public Date getNextReviewTime() { return nextReviewTime; }
}