package com.anticheat.web.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * BCrypt 密码哈希工具包装。
 * 默认 cost = 10（与配置示例 hash 一致）。
 */
public final class PasswordHasher {

    private static final int COST = 10;

    private PasswordHasher() {
    }

    /**
     * 对明文密码生成 bcrypt 哈希字符串。
     */
    public static String hash(String plain) {
        if (plain == null) {
            return null;
        }
        return BCrypt.withDefaults().hashToString(COST, plain.toCharArray());
    }

    /**
     * 校验明文密码是否与给定哈希匹配。
     * 哈希格式非法或为 null 时返回 false。
     */
    public static boolean verify(String plain, String hash) {
        if (plain == null || hash == null || hash.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.verifyer().verify(plain.toCharArray(), hash).verified;
        } catch (Exception e) {
            // 非法哈希格式：视为不匹配
            return false;
        }
    }
}
