package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "USER_GOALS",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_USER_GOAL",
                        columnNames = {"USER_ID", "COURSE_NAME"}
                )
        })
public class UserGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "COURSE_NAME", nullable = false)
    private String courseName;

    @Column(name = "GOAL_COUNT", nullable = false)
    private int goalCount;

    public UserGoal() {}

    public UserGoal(Long userId, String courseName, int goalCount) {
        this.userId = userId;
        this.courseName = courseName;
        this.goalCount = goalCount;
    }
}