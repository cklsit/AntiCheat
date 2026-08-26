package com.anticheat.web.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Gson 单例。所有 Handler 共享同一个 Gson 实例。
 * <p>
 * 使用宽松策略：不转义 HTML、不序列化 null 字段、不格式化（紧凑）。
 */
public final class JsonMapper {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    private JsonMapper() {
    }

    public static Gson get() {
        return GSON;
    }

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return GSON.fromJson(json, clazz);
    }
}
