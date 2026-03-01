package com.example.homework.domain.vo;

import lombok.Data;

@Data
public class AuthLoginResponse {

    private String token;
    private String tokenType = "Bearer";

    public AuthLoginResponse(String token) {
        this.token = token;
    }
}
