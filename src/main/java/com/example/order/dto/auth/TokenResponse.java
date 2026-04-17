package com.example.order.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long memberId;
    private String role;

    public static TokenResponse of(String accessToken, String refreshToken, Long memberId, String role) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", memberId, role);
    }
}
