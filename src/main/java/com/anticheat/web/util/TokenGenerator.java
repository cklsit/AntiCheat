package com.anticheat.web.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Token 生成工具：UUID + Base64 简短 token，用于会话令牌。
 */
public final class TokenGenerator {

    private TokenGenerator() {
    }

    /**
     * 生成 32 字符的标准 UUID（无连字符）。
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成基于 UUID 的 Base64URL 编码 token，去掉等号填充。
     */
    public static String token() {
        String raw = UUID.randomUUID() + UUID.randomUUID().toString();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
