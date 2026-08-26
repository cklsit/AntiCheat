package com.anticheat.web.handler;

import com.anticheat.AdvancedAntiCheat;
import com.anticheat.managers.AuditManager;
import com.anticheat.web.auth.Account;
import com.anticheat.web.auth.AuthFilter;
import com.anticheat.web.dto.ApiResp;
import com.anticheat.web.util.JsonMapper;
import io.javalin.http.Context;

/**
 * Handler 通用工具基类，提供：账号获取、IP 获取、JSON 响应、审计写入。
 */
public abstract class AbstractHandler {

    protected final AdvancedAntiCheat plugin;
    protected final AuditManager auditManager;

    protected AbstractHandler(AdvancedAntiCheat plugin, AuditManager auditManager) {
        this.plugin = plugin;
        this.auditManager = auditManager;
    }

    /** 从 ctx attribute 取已鉴权的账号；无则返回 null。 */
    protected Account account(Context ctx) {
        return ctx.attribute(AuthFilter.ATTR_ACCOUNT);
    }

    /** 从 ctx attribute 取客户端 IP；无则 "unknown"。 */
    protected String ip(Context ctx) {
        Object v = ctx.attribute(AuthFilter.ATTR_IP);
        return v == null ? "unknown" : String.valueOf(v);
    }

    /** 成功响应：200 + {code:0,message:"ok",data} */
    protected void ok(Context ctx, Object data) {
        ctx.status(200);
        ctx.contentType("application/json; charset=utf-8");
        ctx.result(JsonMapper.toJson(ApiResp.ok(data)));
    }

    /** 失败响应：自定义状态码 + {code, message, data:null} */
    protected void fail(Context ctx, int status, String message) {
        ctx.status(status);
        ctx.contentType("application/json; charset=utf-8");
        ctx.result(JsonMapper.toJson(ApiResp.fail(status, message)));
    }

    /** 404 响应 */
    protected void notFound(Context ctx, String message) {
        ctx.status(404);
        ctx.contentType("application/json; charset=utf-8");
        ctx.result(JsonMapper.toJson(ApiResp.notFound(message)));
    }

    /** 403 响应 */
    protected void forbidden(Context ctx, String message) {
        ctx.status(403);
        ctx.contentType("application/json; charset=utf-8");
        ctx.result(JsonMapper.toJson(ApiResp.forbidden(message)));
    }

    /**
     * 写审计日志。operator/operatorRole/ip 来自 ctx，type/target/result/detail 由调用者提供。
     */
    protected void audit(Context ctx, String type, String target, String result, String detail) {
        Account acc = account(ctx);
        String operator = acc == null ? "unknown" : acc.username;
        int role = acc == null ? 3 : acc.roleOrdinal();
        auditManager.log(operator, role, type, target, ip(ctx), result, detail);
    }
}
