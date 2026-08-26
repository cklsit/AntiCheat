package com.anticheat.web.dto;

/**
 * 统一响应包装。
 * <pre>
 * code: 0 = 成功；非 0 = 业务错误；HTTP 状态码与之一致（401/403/404/500 等）
 * message: 人类可读的描述
 * data: 实际数据负载
 * </pre>
 */
public class ApiResp<T> {

    public int code;
    public String message;
    public T data;

    public ApiResp() {
    }

    public ApiResp(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResp<T> ok(T data) {
        return new ApiResp<>(0, "ok", data);
    }

    public static <T> ApiResp<T> ok() {
        return new ApiResp<>(0, "ok", null);
    }

    public static <T> ApiResp<T> fail(int code, String message) {
        return new ApiResp<>(code, message, null);
    }

    public static <T> ApiResp<T> notFound(String message) {
        return new ApiResp<>(404, message, null);
    }

    public static <T> ApiResp<T> unauthorized(String message) {
        return new ApiResp<>(401, message, null);
    }

    public static <T> ApiResp<T> forbidden(String message) {
        return new ApiResp<>(403, message, null);
    }

    public static <T> ApiResp<T> error(String message) {
        return new ApiResp<>(500, message, null);
    }
}
