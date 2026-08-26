package com.anticheat.web.util;

import io.javalin.http.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 客户端 IP 解析工具。
 * <p>
 * 反代场景下优先 X-Forwarded-For 第一项；缺失时回退 Context.ip()。
 */
public final class InetAddressUtil {

    private InetAddressUtil() {
    }

    /**
     * 获取客户端真实 IP。
     */
    public static String getRemoteAddr(Context ctx) {
        if (ctx == null) {
            return "unknown";
        }
        String xff = ctx.header("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            String first = xff.split(",")[0].trim();
            if (!first.isEmpty() && !"unknown".equalsIgnoreCase(first)) {
                return first;
            }
        }
        // 反代单跳场景
        String real = ctx.header("X-Real-IP");
        if (real != null && !real.isEmpty() && !"unknown".equalsIgnoreCase(real)) {
            return real;
        }
        return ctx.ip() == null ? "unknown" : ctx.ip();
    }

    /**
     * 解析 X-Forwarded-For 链，返回 IP 列表（去空格、去 unknown）。
     */
    public static List<String> parseForwardedChain(String xff) {
        if (xff == null || xff.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> list = new java.util.ArrayList<>();
        for (String s : Arrays.asList(xff.split(","))) {
            String t = s.trim();
            if (!t.isEmpty() && !"unknown".equalsIgnoreCase(t)) {
                list.add(t);
            }
        }
        return list;
    }
}
