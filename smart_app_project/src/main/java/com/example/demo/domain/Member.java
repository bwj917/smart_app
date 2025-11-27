package com.example.demo.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Member {
    private Long id;
    private String userid;
    private String pw;
    private String name;
    private String email;
    private String phone;
    private LocalDateTime joinDate;
    private int points;
    private String ownedCharacters;
    private int equippedCharacterIdx;

    public int getEquippedCharacterIdx() {
        return equippedCharacterIdx;
    }

    public void setEquippedCharacterIdx(int equippedCharacterIdx) {
        this.equippedCharacterIdx = equippedCharacterIdx;
    }

}

