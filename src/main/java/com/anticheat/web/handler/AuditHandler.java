package com.anticheat.web.handler;

import com.anticheat.AdvancedAntiCheat;
import com.anticheat.managers.AuditManager;
import com.anticheat.managers.audit.AuditQuery;
import com.anticheat.web.auth.Permission;
import com.anticheat.web.dto.PageDTO;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;

/**
 * GET /api/audit - 审计日志分页查询。
 */
public class AuditHandler extends AbstractHandler {

    public AuditHandler(AdvancedAntiCheat plugin, AuditManager auditManager) {
        super(plugin, auditManager);
    }

    public void register(Javalin app) {
        app.get("/api/audit", this::list);
    }

    private void list(Context ctx) {
        if (!AuthHandler.require(ctx, Permission.AUDIT_READ)) return;

        AuditQuery q = new AuditQuery()
                .setType(ctx.queryParam("type"))
                .setResult(ctx.queryParam("result"))
                .setKeyword(ctx.queryParam("keyword"))
                .setPage(parseInt(ctx.queryParam("page"), 1))
                .setPageSize(parseInt(ctx.queryParam("pageSize"), 20));

        // 时间范围（epoch millis）
        String start = ctx.queryParam("startTime");
        String end = ctx.queryParam("endTime");
        if (start != null && !start.isEmpty()) {
            try { q.setStartTime(Long.parseLong(start)); } catch (Exception ignored) {}
        }
        if (end != null && !end.isEmpty()) {
            try { q.setEndTime(Long.parseLong(end)); } catch (Exception ignored) {}
        }

        long total = auditManager.count(q);
        List<com.anticheat.web.dto.AuditDTO> list = auditManager.query(q);
        int pageSize = q.getPageSize();
        int pages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
        PageDTO<com.anticheat.web.dto.AuditDTO> result = new PageDTO<>(list, total, q.getPage(), pageSize);
        result.pages = pages;
        ok(ctx, result);
    }

    private static int parseInt(String s, int def) {
        if (s == null || s.isEmpty()) return def;
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }
}
