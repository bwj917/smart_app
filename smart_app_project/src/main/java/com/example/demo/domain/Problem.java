package com.example.demo.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "PROBLEM")
public class Problem {

    @Id
    @Column(name = "PROBLEMID")
    private Long problemId;

    private String question;

    private String answer;

    @Column(name = "COURSE_ID")
    private String courseId;

    public Problem() {}

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    // 🔥 [추가] 누락되었던 Getter/Setter 추가
    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    @Override
    public String toString() {
        return "Problem [id=" + problemId + ", question=" + question + ", courseId=" + courseId + "]";
    }
}