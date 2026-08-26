package com.anticheat.web.handler;

import com.anticheat.AdvancedAntiCheat;
import com.anticheat.managers.ViolationManager;
import com.anticheat.detection.ViolationRecord;
import com.anticheat.managers.AuditManager;
import com.anticheat.web.BukkitBridge;
import com.anticheat.web.auth.Permission;
import com.anticheat.web.dto.ApiResp;
import com.anticheat.web.dto.LinkedAccountDTO;
import com.anticheat.web.dto.PageDTO;
import com.anticheat.web.dto.PlayerDTO;
import com.anticheat.web.dto.ViolationRecordDTO;
import com.anticheat.web.util.JsonMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 玩家相关 REST 端点：
 * <ul>
 *   <li>GET /api/players - 在线玩家分页</li>
 *   <li>GET /api/players/{uuid} - 玩家详情</li>
 *   <li>POST /api/players/{uuid}/ban - 封禁玩家</li>
 * </ul>
 */
public class PlayerHandler extends AbstractHandler {

    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneId.systemDefault());

    public PlayerHandler(AdvancedAntiCheat plugin, AuditManager auditManager) {
        super(plugin, auditManager);
    }

    public void register(Javalin app) {
        app.get("/api/players", this::list);
        app.get("/api/players/{uuid}", this::detail);
        app.post("/api/players/{uuid}/ban", this::ban);
    }

    private void list(Context ctx) {
        if (!AuthHandler.require(ctx, Permission.PLAYERS_READ)) return;

        int page = parseInt(ctx.queryParam("page"), 1);
        int pageSize = parseInt(ctx.queryParam("pageSize"), 20);
        String keyword = ctx.queryParam("keyword");
        String status = ctx.queryParam("status");

        // 切到主线程拿玩家快照
        List<PlayerDTO> snapshot = BukkitBridge.syncSupply(plugin, () -> {
            List<PlayerDTO> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                list.add(toDTO(p));
            }
            return list;
        });

        // 过滤
        List<PlayerDTO> filtered = new ArrayList<>();
        for (PlayerDTO p : snapshot) {
            if (keyword != null && !keyword.isEmpty()) {
                String k = keyword.toLowerCase(Locale.ROOT);
                if (!p.name.toLowerCase(Locale.ROOT).contains(k) && !p.uuid.toLowerCase(Locale.ROOT).contains(k) && !p.ip.contains(k)) {
                    continue;
                }
            }
            if (status != null && !status.isEmpty() && !status.equals(p.status)) {
                continue;
            }
            filtered.add(p);
        }
        // 按风险分倒序
        filtered.sort((a, b) -> Integer.compare(b.riskScore, a.riskScore));

        int total = filtered.size();
        int from = (page - 1) * pageSize;
        int to = Math.min(filtered.size(), from + pageSize);
        List<PlayerDTO> pageList = (from >= filtered.size())
                ? Collections.emptyList()
                : filtered.subList(from, to);

        PageDTO<PlayerDTO> result = new PageDTO<>(pageList, total, page, pageSize);
        ok(ctx, result);
    }

    private void detail(Context ctx) {
        if (!AuthHandler.require(ctx, Permission.PLAYERS_READ)) return;
        String uuidStr = ctx.pathParam("uuid");
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            notFound(ctx, "UUID 非法: " + uuidStr);
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            notFound(ctx, "玩家不在线或不存在");
            return;
        }
        PlayerDTO dto = BukkitBridge.syncSupply(plugin, () -> toDTO(player));
        ok(ctx, dto);
    }

    private void ban(Context ctx) {
        if (!AuthHandler.require(ctx, Permission.PLAYERS_BAN)) return;
        String uuidStr = ctx.pathParam("uuid");
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            notFound(ctx, "UUID 非法: " + uuidStr);
            return;
        }
        BanPayload payload = JsonMapper.fromJson(ctx.body(), BanPayload.class);
        if (payload == null || payload.duration == null || payload.duration.isEmpty()) {
            fail(ctx, 400, "请提供 duration 与 reason");
            return;
        }

        // 切回主线程执行封禁
        BukkitBridge.syncRun(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            String name = p == null ? uuidStr : p.getName();
            plugin.getBanManager().banPlayer(uuid, name, payload.duration,
                    payload.reason == null ? "Web 面板封禁" : payload.reason);
        });

        audit(ctx, "ban", uuidStr, "success", "duration=" + payload.duration
                + (payload.reason != null ? " reason=" + payload.reason : ""));
        ok(ctx, null);
    }

    // ===== 内部映射 =====

    private PlayerDTO toDTO(Player p) {
        PlayerDTO dto = new PlayerDTO();
        dto.uuid = p.getUniqueId().toString();
        dto.name = p.getName();
        dto.avatar = p.getName() == null || p.getName().isEmpty() ? "?" : p.getName().substring(0, 1).toUpperCase();
        dto.ip = p.getAddress() == null || p.getAddress().getAddress() == null ? "" : p.getAddress().getAddress().getHostAddress();
        dto.ping = p.getPing();
        dto.gameMode = p.getGameMode() == null ? "" : p.getGameMode().name();
        dto.world = p.getWorld() == null ? "" : p.getWorld().getName();
        dto.firstJoin = ISO.format(Instant.ofEpochMilli(p.getFirstPlayed()));
        dto.lastJoin = ISO.format(Instant.ofEpochMilli(p.getLastPlayed()));
        dto.onlineDuration = (System.currentTimeMillis() - p.getLastPlayed()) / 1000L;
        dto.version = Bukkit.getVersion();
        dto.country = "";
        dto.hardwareId = "";

        boolean banned = plugin.getBanManager().isBanned(p.getUniqueId());
        dto.status = banned ? "banned" : "online";

        // 违规历史 + 风险评分
        ViolationManager vm = plugin.getDetectionManager().getViolationManager();
        List<ViolationRecord> history = vm.getViolationHistory(p.getUniqueId());
        List<ViolationRecordDTO> his = new ArrayList<>();
        int score = 0;
        long lastTs = 0;
        String serverName = plugin.getConfig().getString("database.server-name", "Server-1");
        for (ViolationRecord r : history) {
            int s = (int) Math.round(r.getViolationLevel() * 10);
            score += s;
            his.add(new ViolationRecordDTO(
                    r.getPlayerUUID() + "-" + r.getTimestamp(),
                    r.getType().name(),
                    r.getSeverity().name(),
                    s,
                    ISO.format(Instant.ofEpochMilli(r.getTimestamp())),
                    serverName,
                    r.getDetails()
            ));
            if (r.getTimestamp() > lastTs) lastTs = r.getTimestamp();
        }
        if (score > 100) score = 100;
        dto.riskScore = score;
        dto.riskLevel = scoreToLevel(score);
        dto.lastTrigger = lastTs == 0 ? "" : ISO.format(Instant.ofEpochMilli(lastTs));
        dto.violationsCount = history.size();
        dto.violationHistory = his;
        dto.linkedAccounts = Collections.emptyList();
        return dto;
    }

    private static int scoreToLevel(int score) {
        if (score >= 90) return 3;
        if (score >= 70) return 2;
        if (score >= 50) return 1;
        return 0;
    }

    private static int parseInt(String s, int def) {
        if (s == null || s.isEmpty()) return def;
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    public static class BanPayload {
        public String duration;
        public String reason;
    }
}
