package com.anticheat.web.dto;

import java.util.List;

/**
 * 登录用户信息。与前端 UserInfo 对齐。
 */
public class UserInfoDTO {

    public String id;
    public String username;
    public String nickname;
    /** 数字角色枚举：0=SUPER_ADMIN 1=ADMIN 2=REVIEWER 3=OBSERVER */
    public int role;
    public String avatar;
    public String lastLogin;
    public String lastIp;
    public List<String> permissions;

    public UserInfoDTO() {
    }

    public UserInfoDTO(String id, String username, String nickname, int role, String avatar,
                       String lastLogin, String lastIp, List<String> permissions) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.role = role;
        this.avatar = avatar;
        this.lastLogin = lastLogin;
        this.lastIp = lastIp;
        this.permissions = permissions;
    }
}
