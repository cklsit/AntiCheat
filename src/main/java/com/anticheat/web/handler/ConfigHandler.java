package com.anticheat.web.handler;

import com.anticheat.AdvancedAntiCheat;
import com.anticheat.managers.AuditManager;
import com.anticheat.web.BukkitBridge;
import com.anticheat.web.auth.Permission;
import com.anticheat.web.dto.ConfigModuleDTO;
import com.anticheat.web.util.JsonMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置中心 REST 端点：
 * <ul>
 *   <li>GET /api/config/modules - 模块配置列表</li>
 *   <li>PUT /api/config/modules/{id} - 修改模块配置</li>
 * </ul>
 * 模块 id 即 config.yml 中 detection.{module}.enabled 等键。
 */
public class ConfigHandler extends AbstractHandler {

    public ConfigHandler(AdvancedAntiCheat plugin, AuditManager auditManager) {
        super(plugin, auditManager);
    }

    public void register(Javalin app) {
        app.get("/api/config/modules", this::list);
        app.put("/api/config/modules/{id}", this::update);
    }

    private static final String[][] MODULES = {
            {"fly",       "飞行检测"},
            {"speed",     "速度作弊"},
            {"esp",       "透视 ESP"},
            {"killaura",  "KillAura"},
            {"reach",     "攻击距离"},
            {"scaffold",  "脚手架"},
            {"fastbreak", "快速破坏"},
            {"noslow",    "无减速挖掘"}
    };

    private void list(Context ctx) {
        if (!AuthHandler.require(ctx, Permission.CONFIG_READ)) return;

        List<ConfigModuleDTO> list = new ArrayList<>();
        for (String[] m : MODULES) {
            String key = m[0];
            String name = m[1];
            boolean enabled = plugin.getConfigManager().isDetectionEnabled(key);
            int max = plugin.getConfigManager().getMaxViolations(key);
            int autoBan = 50 + max * 10;  // 简单派生：1→60, 5→100
            if (autoBan > 100) autoBan = 100;
            int humanReview = Math.max(40, autoBan - 20);
            list.add(new ConfigModuleDTO(key, name, key, enabled, autoBan, humanReview));
        }
        ok(ctx, list);
    }

    private void update(Context ctx) {
        if (!AuthHandler.require(ctx, Permission.CONFIG_UPDATE)) return;
        String id = ctx.pathParam("id");
        ConfigModuleDTO payload = JsonMapper.fromJson(ctx.body(), ConfigModuleDTO.class);
        if (payload == null) {
            fail(ctx, 400, "请求体非法");
            return;
        }
        // 切回主线程保存 config（避免 Bukkit API 警告）
        BukkitBridge.syncRun(plugin, () -> {
            plugin.getConfig().set("detection." + id + ".enabled", payload.enabled);
            if (payload.autoBan > 0) {
                int maxViolations = Math.max(1, (payload.autoBan - 50) / 10);
                plugin.getConfig().set("detection." + id + ".maxViolations", maxViolations);
            }
            plugin.saveConfig();
        });
        audit(ctx, "config_change", id, "success",
                "enabled=" + payload.enabled + " autoBan=" + payload.autoBan + " humanReview=" + payload.humanReview);
        ok(ctx, payload);
    }
}
