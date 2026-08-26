package com.anticheat.web.handler;

import com.anticheat.AdvancedAntiCheat;
import com.anticheat.managers.AuditManager;
import com.anticheat.web.auth.Permission;
import com.anticheat.web.dto.NotificationItemDTO;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 通知 REST 端点：
 * <ul>
 *   <li>GET /api/notifications - 通知列表（内存环形缓冲，最近 50 条）</li>
 *   <li>POST /api/notifications/markAllRead - 标记全部已读（清空）</li>
 * </ul>
 * 通知由告警事件、审计事件追加而来。
 */
public class NotificationHandler extends AbstractHandler {

    private static final int CAPACITY = 50;
    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final AtomicLong ID_SEQ = new AtomicLong(0);

    private final ConcurrentLinkedDeque<NotificationItemDTO> buffer = new ConcurrentLinkedDeque<>();

    public NotificationHandler(AdvancedAntiCheat plugin, AuditManager auditManager) {
        super(plugin, auditManager);
    }

    public void register(Javalin app) {
        app.get("/api/notifications", this::list);
        app.post("/api/notifications/markAllRead", this::markAllRead);
    }

    /** 暴露给 AlertBroadcaster 等外部模块推送通知。 */
    public void push(NotificationItemDTO item) {
        if (item == null) return;
        if (item.id == null || item.id.isEmpty()) {
            item.id = "n-" + ID_SEQ.incrementAndGet();
        }
        if (item.time == null || item.time.isEmpty()) {
            item.time = ISO.format(Instant.now());
        }
        buffer.addFirst(item);
        while (buffer.size() > CAPACITY) {
            buffer.removeLast();
        }
    }

    private void list(Context ctx) {
        if (!AuthHandler.require(ctx, Permission.NOTIFICATIONS_READ)) return;
        ok(ctx, new ArrayList<>(buffer));
    }

    private void markAllRead(Context ctx) {
        if (!AuthHandler.require(ctx, Permission.NOTIFICATIONS_READ)) return;
        buffer.clear();
        ok(ctx, null);
    }
}
