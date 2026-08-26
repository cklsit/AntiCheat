package com.anticheat.web.dto;

/**
 * 登录响应。与前端 LoginResult 对齐。
 */
public class LoginResultDTO {

    public String token;
    public UserInfoDTO user;

    public LoginResultDTO() {
    }

    public LoginResultDTO(String token, UserInfoDTO user) {
        this.token = token;
        this.user = user;
    }
}
