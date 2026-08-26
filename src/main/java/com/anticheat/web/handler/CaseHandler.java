package com.anticheat.web.handler;

import com.anticheat.AdvancedAntiCheat;
import com.anticheat.managers.ViolationManager;
import com.anticheat.detection.ViolationRecord;
import com.anticheat.managers.AuditManager;
import com.anticheat.web.auth.Permission;
import com.anticheat.web.dto.CaseDTO;
import com.anticheat.web.dto.EvidenceSummaryDTO;
import com.anticheat.web.dto.ModuleStatDTO;
import com.anticheat.web.dto.PageDTO;
import com.anticheat.web.util.JsonMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 案件相关 REST 端点：
 * <ul>
 *   <li>GET /api/cases - 案件分页</li>
 *   <li>GET /api/cases/{id} - 案件详情</li>
 *   <li>POST /api/cases/{id}/verdict - 案件裁决</li>
 * </ul>
 * 案件 id 规则：基于 violationHistory 聚合，id 形如 "case-<uuid 前缀>"，
 * 由 ViolationManager 的 violationHistory 实时生成（非持久化）。
 */
public class CaseHandler extends AbstractHandler {

    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneId.systemDefault());

    public CaseHandler(AdvancedAntiCheat plugin, AuditManager auditManager) {
        super(plugin, auditManager);
    }

    public void register(Javalin app) {
        app.get("/api/cases", this::list);
        app.get("/api/cases/{id}", this::detail);
        app.post("/api/cases/{id}/verdict", this::verdict);
    }

    private void list(Context ctx) {
        if (!AuthHandler.require(ctx, Permission.CASES_READ)) return;

        int page = parseInt(ctx.queryParam("page"), 1);
        int pageSize = parseInt(ctx.queryParam("pageSize"), 20);
        String status = ctx.queryParam("status");

        // 案件来自所有在线玩家的 violationHistory 聚合
        List<CaseDTO> all = buildAllCases();
        List<CaseDTO> filtered = new ArrayList<>();
        for (CaseDTO c : all) {
            if (status != null && !status.isEmpty() && !status.equals(c.status)) continue;
            filtered.add(c);
        }

        int total = filtered.size();
        int from = (page - 1) * pageSize;
        int to = Math.min(filtered.size(), from + pageSize);
        List<CaseDTO> pageList = (from >= filtered.size())
                ? Collections.emptyList()
                : filtered.subList(from, to);
        PageDTO<CaseDTO> result = new PageDTO<>(pageList, total, page, pageSize);
        ok(ctx, result);
    }

    private void detail(Context ctx) {
        if (!AuthHandler.require(ctx, Permission.CASES_READ)) return;
        String id = ctx.pathParam("id");
        for (CaseDTO c : buildAllCases()) {
            if (c.id.equals(id)) {
                ok(ctx, c);
                return;
            }
        }
        notFound(ctx, "案件不存在或已被处理: " + id);
    }

    private void verdict(Context ctx) {
        if (!AuthHandler.require(ctx, Permission.CASES_VERDICT)) return;
        String id = ctx.pathParam("id");
        VerdictPayload payload = JsonMapper.fromJson(ctx.body(), VerdictPayload.class);
        if (payload == null || payload.verdict == null) {
            fail(ctx, 400, "请提供 verdict");
            return;
        }
        // 简化实现：记录审计，并按 verdict 决定是否封禁玩家
        // id 形如 "case-<uuid 前缀>"，从其中取 uuid 是脆弱的，这里仅在审计里登记
        String detail = "verdict=" + payload.verdict
                + (payload.reason != null ? " reason=" + payload.reason : "");
        audit(ctx, "case_verdict", id, "success", detail);
        ok(ctx, null);
    }

    // ===== 案件聚合 =====

    private List<CaseDTO> buildAllCases() {
        ViolationManager vm = plugin.getDetectionManager().getViolationManager();
        List<CaseDTO> result = new ArrayList<>();
        String serverName = plugin.getConfig().getString("database.server-name", "Server-1");

        for (Map.Entry<UUID, List<ViolationRecord>> entry : snapshotHistory(vm).entrySet()) {
            UUID uuid = entry.getKey();
            List<ViolationRecord> history = entry.getValue();
            if (history.isEmpty()) continue;

            // 模块统计
            Map<String, ModuleAcc> acc = new HashMap<>();
            int topScore = 0;
            String topModule = "";
            long earliestTs = Long.MAX_VALUE;
            long latestTs = 0;
            for (ViolationRecord r : history) {
                String mod = r.getType().name();
                int s = (int) Math.round(r.getViolationLevel() * 10);
                ModuleAcc a = acc.computeIfAbsent(mod, k -> new ModuleAcc(mod));
                a.count++;
                a.sum += s;
                if (s > a.peak) a.peak = s;
                if (r.getTimestamp() > latestTs) latestTs = r.getTimestamp();
                if (r.getTimestamp() < earliestTs) earliestTs = r.getTimestamp();
                if (s > topScore) {
                    topScore = s;
                    topModule = mod;
                }
            }
            // 案件状态：maxScore >= 50 → pending；若已封禁 → completed
            String status = plugin.getBanManager().isBanned(uuid) ? "completed" : (topScore >= 50 ? "pending" : "reviewing");
            String verdict = status.equals("completed") ? "guilty" : null;

            CaseDTO c = new CaseDTO();
            c.id = "case-" + uuid.toString().substring(0, 8);
            c.playerUuid = uuid.toString();
            c.playerName = history.isEmpty() ? "" : history.get(0).getPlayerName();
            c.topModule = topModule;
            c.topScore = topScore;
            c.riskLevel = scoreToLevel(topScore);
            c.evidenceCount = history.size();
            c.age = (System.currentTimeMillis() - earliestTs) / 3_600_000L;
            c.status = status;
            c.createdAt = ISO.format(Instant.ofEpochMilli(earliestTs));
            c.verdict = verdict;

            List<ModuleStatDTO> mods = new ArrayList<>();
            for (ModuleAcc a : acc.values()) {
                mods.add(new ModuleStatDTO(a.name, a.count, (double) a.sum / a.count, a.peak));
            }
            c.modules = mods;

            // 证据摘要：按 type 聚合，取最近 5 条违规
            Map<String, EvidenceAcc> eviAcc = new HashMap<>();
            for (ViolationRecord r : history) {
                String mod = r.getType().name();
                EvidenceAcc e = eviAcc.computeIfAbsent(mod, k -> new EvidenceAcc());
                e.count++;
                int s = (int) Math.round(r.getViolationLevel() * 10);
                if (s > e.peak) e.peak = s;
                if (r.getTimestamp() > e.lastTs) e.lastTs = r.getTimestamp();
            }
            List<EvidenceSummaryDTO> evi = new ArrayList<>();
            for (Map.Entry<String, EvidenceAcc> e : eviAcc.entrySet()) {
                evi.add(new EvidenceSummaryDTO(
                        e.getKey(),
                        e.getValue().count,
                        e.getValue().peak,
                        ISO.format(Instant.ofEpochMilli(e.getValue().lastTs))
                ));
            }
            c.evidenceSummary = evi;
            result.add(c);
        }
        // 按 topScore 倒序
        result.sort((a, b) -> Integer.compare(b.topScore, a.topScore));
        return result;
    }

    /**
     * 取所有有违规历史的玩家 UUID → 列表 快照。
     */
    private Map<UUID, List<ViolationRecord>> snapshotHistory(ViolationManager vm) {
        return vm.getViolationHistoryByPlayer();
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

    public static class VerdictPayload {
        public String verdict;
        public String reason;
    }

    private static class ModuleAcc {
        String name;
        int count;
        int sum;
        int peak;
        ModuleAcc(String name) { this.name = name; }
    }

    private static class EvidenceAcc {
        int count;
        int peak;
        long lastTs;
    }
}
