package com.example.demo.dto;

import com.example.demo.domain.Problem;
import com.example.demo.domain.UserProblemStats;
import java.util.Date;

public class ProblemResponseDto {
    private Long problemId;
    private String question;
    private String answer;
    private Integer problemLevel;
    private Date nextReviewTime;
    private Integer totalAttempts;
    private boolean isScrapped;

    public ProblemResponseDto(Problem problem, UserProblemStats stats) {
        this.problemId = problem.getProblemId();
        this.question = problem.getQuestion();
        this.answer = problem.getAnswer();

        if (stats != null) {
            this.problemLevel = stats.getProblemLevel();
            this.nextReviewTime = stats.getNextReviewTime();
            this.totalAttempts = stats.getTotalAttempts();
            this.isScrapped = stats.isScrapped();
        } else {
            this.problemLevel = 0;
            this.nextReviewTime = null;
            this.totalAttempts = 0;
            this.isScrapped = false;
        }
    }

    public Long getProblemId() { return problemId; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public Integer getProblemLevel() { return problemLevel; }
    public Date getNextReviewTime() { return nextReviewTime; }
    public Integer getTotalAttempts() { return totalAttempts; }

    // 🔥 [핵심 수정] Getter 이름을 getIsScrapped()로 변경
    // (Jackson 라이브러리가 JSON 키를 "isScrapped"로 만들어줍니다.)
    public boolean getIsScrapped() {
        return isScrapped;
    }
}