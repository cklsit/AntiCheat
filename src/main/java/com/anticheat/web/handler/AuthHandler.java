package com.anticheat.web.handler;

import com.anticheat.AdvancedAntiCheat;
import com.anticheat.managers.AuditManager;
import com.anticheat.web.auth.AuthManager;
import com.anticheat.web.auth.Permission;
import com.anticheat.web.dto.LoginResultDTO;
import com.anticheat.web.dto.UserInfoDTO;
import com.anticheat.web.util.InetAddressUtil;
import com.anticheat.web.util.JsonMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

/**
 * 认证路由：POST /api/auth/login | POST /api/auth/logout | GET /api/auth/me。
 */
public class AuthHandler extends AbstractHandler {

    private final AuthManager authManager;

    public AuthHandler(AdvancedAntiCheat plugin, AuditManager auditManager, AuthManager authManager) {
        super(plugin, auditManager);
        this.authManager = authManager;
    }

    public void register(Javalin app) {
        app.post("/api/auth/login", this::login);
        app.post("/api/auth/logout", this::logout);
        app.get("/api/auth/me", this::me);
    }

    private void login(Context ctx) {
        LoginPayload payload = JsonMapper.fromJson(ctx.body(), LoginPayload.class);
        if (payload == null || payload.username == null || payload.password == null) {
            fail(ctx, 400, "请提供 username 和 password");
            return;
        }
        String ip = InetAddressUtil.getRemoteAddr(ctx);
        LoginResultDTO result = authManager.login(payload.username, payload.password, ip);
        if (result == null) {
            audit(ctx, "login", payload.username, "failed", "账号或密码错误");
            fail(ctx, 401, "账号或密码错误");
            return;
        }
        // 写审计
        auditManager.log(result.user.username, result.user.role, "login",
                payload.username, ip, "success", "Web 面板登录");
        ok(ctx, result);
    }

    private void logout(Context ctx) {
        String authHeader = ctx.header("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            authManager.logout(token);
        }
        audit(ctx, "logout", "-", "success", "Web 面板登出");
        ok(ctx, null);
    }

    private void me(Context ctx) {
        var account = account(ctx);
        if (account == null) {
            fail(ctx, 401, "未登录");
            return;
        }
        // 同步从 AuthManager 取最新信息（运行时附加字段如 lastLogin/lastIp）
        UserInfoDTO user = new UserInfoDTO(
                "u-" + account.username.toLowerCase(),
                account.username,
                account.nickname(),
                account.roleOrdinal(),
                account.avatarChar(),
                account.lastLogin == null ? "" : account.lastLogin,
                account.lastIp == null ? "" : account.lastIp,
                new java.util.ArrayList<>(account.permissions == null ? java.util.Collections.emptyList() : account.permissions)
        );
        ok(ctx, user);
    }

    /** 登录请求体。 */
    public static class LoginPayload {
        public String username;
        public String password;
        public String totp;
        public String captcha;
    }

    /** 静态方法：直接通过 Permission 校验当前 ctx 的账号。 */
    public static boolean require(Context ctx, String perm) {
        Object acc = ctx.attribute("web.account");
        if (!(acc instanceof com.anticheat.web.auth.Account account)) {
            ctx.status(401);
            ctx.contentType("application/json; charset=utf-8");
            ctx.result(com.anticheat.web.util.JsonMapper.toJson(
                    com.anticheat.web.dto.ApiResp.unauthorized("未登录或 token 失效")));
            return false;
        }
        if (!Permission.hasPermission(account, perm)) {
            ctx.status(403);
            ctx.contentType("application/json; charset=utf-8");
            ctx.result(com.anticheat.web.util.JsonMapper.toJson(
                    com.anticheat.web.dto.ApiResp.forbidden("权限不足: 需要 " + perm)));
            return false;
        }
        return true;
    }
}
