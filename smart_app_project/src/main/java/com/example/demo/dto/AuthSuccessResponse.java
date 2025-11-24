// package com.example.demo.dto;
// fileName: AuthSuccessResponse.java (새 파일)

package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthSuccessResponse {
    private Long userId;
    private String message;
}