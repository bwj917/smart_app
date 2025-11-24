package com.example.demo.dto; // dto 패키지에 만드세요

import com.example.demo.domain.Problem;
import com.example.demo.domain.UserProblemStats;
import java.util.Date;

public class ProblemResponseDto {
    private Long problemId;
    private String question;
    private String answer;
    private Integer problemLevel;
    private Date nextReviewTime;

    // 생성자: 문제와 통계 정보를 받아서 하나로 합칩니다.
    public ProblemResponseDto(Problem problem, UserProblemStats stats) {
        this.problemId = problem.getProblemId();
        this.question = problem.getQuestion();
        this.answer = problem.getAnswer();

        if (stats != null) {
            this.problemLevel = stats.getProblemLevel();
            this.nextReviewTime = stats.getNextReviewTime();
        } else {
            // 통계가 없으면(처음 푸는 문제) 기본값 설정
            this.problemLevel = 0;
            this.nextReviewTime = null; // null이면 앱에서 "새 문제"로 인식
        }
    }

    // Getter들 (JSON 변환을 위해 필수)
    public Long getProblemId() { return problemId; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public Integer getProblemLevel() { return problemLevel; }
    public Date getNextReviewTime() { return nextReviewTime; }
}