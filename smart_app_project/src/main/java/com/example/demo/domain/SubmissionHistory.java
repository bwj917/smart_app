package com.example.demo.domain;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "SUBMISSION_HISTORY",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_USER_PROBLEM",
                        columnNames = {"USER_ID", "PROBLEM_ID"}
                )
        })
public class SubmissionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long problemId;

    @Column(nullable = false)
    private boolean isCorrect;

    @Temporal(TemporalType.TIMESTAMP)
    private Date submittedAt;

    @Column(nullable = false)
    private int studyTime;

    public SubmissionHistory() {}

    public SubmissionHistory(Long userId, Long problemId, boolean isCorrect, Date submittedAt, int studyTime) {
        this.userId = userId;
        this.problemId = problemId;
        this.isCorrect = isCorrect;
        this.submittedAt = submittedAt;
        this.studyTime = studyTime;
    }

    // --- Getter ---
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getProblemId() { return problemId; }
    public boolean isCorrect() { return isCorrect; }
    public Date getSubmittedAt() { return submittedAt; }
    public int getStudyTime() { return studyTime; }

    // --- 🔥 [추가] Setter (이게 없어서 에러가 났습니다) ---
    public void setUserId(Long userId) { this.userId = userId; }
    public void setProblemId(Long problemId) { this.problemId = problemId; }
    public void setCorrect(boolean correct) { isCorrect = correct; }
    public void setSubmittedAt(Date submittedAt) { this.submittedAt = submittedAt; }
    public void setStudyTime(int studyTime) { this.studyTime = studyTime; }
}