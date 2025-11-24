package com.example.demo.domain;

public class SubmissionRequest {

    private Long problemId;
    private Long userId;
    private String userAnswer;
    private int checkCount;

    // 🔥 [신규 추가] 공부 시간 (초 단위)
    private int studyTime;

    // --- Getter & Setter ---
    public Long getProblemId() { return problemId; }
    public void setProblemId(Long problemId) { this.problemId = problemId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    public int getCheckCount() { return checkCount; }
    public void setCheckCount(int checkCount) { this.checkCount = checkCount; }

    // 🔥 추가된 필드의 Getter/Setter
    public int getStudyTime() { return studyTime; }
    public void setStudyTime(int studyTime) { this.studyTime = studyTime; }
}