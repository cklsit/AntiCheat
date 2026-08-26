package com.anticheat.web.auth;

import java.util.List;

/**
 * Web 面板登录账号 POJO，对应 config.yml 中 web.auth.accounts 列表项。
 */
public class Account {

    public String username;
    public String passwordHash;
    /** 配置文件中的 role 字符串：admin / mod / reviewer / observer */
    public String role;
    public List<String> permissions;

    // ===== 运行时附加字段，不参与序列化 =====
    public String lastLogin;
    public String lastIp;

    public Account() {
    }

    public Account(String username, String passwordHash, String role, List<String> permissions) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.permissions = permissions;
    }

    /**
     * 将字符串角色映射为前端 Role 数字枚举：
     * 0=SUPER_ADMIN 1=ADMIN 2=REVIEWER 3=OBSERVER。
     * 未知角色默认 3（观察员）。
     */
    public int roleOrdinal() {
        if (role == null) {
            return 3;
        }
        switch (role.toLowerCase()) {
            case "admin":
            case "super_admin":
                return 0;
            case "mod":
                return 1;
            case "reviewer":
                return 2;
            default:
                return 3;
        }
    }

    public String nickname() {
        switch (roleOrdinal()) {
            case 0: return username + " (超级管理员)";
            case 1: return username + " (管理员)";
            case 2: return username + " (审理员)";
            default: return username + " (观察员)";
        }
    }

    public String avatarChar() {
        if (username == null || username.isEmpty()) {
            return "?";
        }
        return username.substring(0, 1).toUpperCase();
    }
}
