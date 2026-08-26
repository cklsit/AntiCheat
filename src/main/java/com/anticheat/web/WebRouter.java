package com.anticheat.web;

import com.anticheat.AdvancedAntiCheat;
import com.anticheat.managers.AuditManager;
import com.anticheat.web.handler.*;
import io.javalin.Javalin;

/**
 * REST 路由集中注册。
 * 所有 Handler 实例化在此完成，路由前缀与文档对齐。
 */
public final class WebRouter {

    private WebRouter() {
    }

    public static void register(Javalin app, AdvancedAntiCheat plugin, WebServer webServer) {
        AuditManager auditManager = plugin.getAuditManager();
        if (auditManager == null) {
            plugin.getLogger().warning("[Web] AuditManager 未初始化，REST 路由将不记录审计");
        }

        AuthHandler auth = new AuthHandler(plugin, auditManager, webServer.getAuthManager());
        PlayerHandler players = new PlayerHandler(plugin, auditManager);
        CaseHandler cases = new CaseHandler(plugin, auditManager);
        DashboardHandler dashboard = new DashboardHandler(plugin, auditManager, webServer.getBroadcaster());
        AuditHandler audit = new AuditHandler(plugin, auditManager);
        NotificationHandler notif = new NotificationHandler(plugin, auditManager);
        ConfigHandler config = new ConfigHandler(plugin, auditManager);

        auth.register(app);
        players.register(app);
        cases.register(app);
        dashboard.register(app);
        audit.register(app);
        notif.register(app);
        config.register(app);

        plugin.getLogger().info("[Web] REST 路由注册完成");
    }
}
