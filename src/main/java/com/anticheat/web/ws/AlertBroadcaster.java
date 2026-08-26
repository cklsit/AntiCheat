package com.anticheat.web.ws;

import com.anticheat.web.dto.AlertDTO;
import com.anticheat.web.dto.WSMessage;
import com.anticheat.web.util.JsonMapper;
import io.javalin.websocket.WsContext;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 告警广播器（单例）。
 * <p>
 * - 维护活跃 WebSocket 连接集合
 * - broadcast(AlertDTO) 遍历推送 WSMessage{type:"alert"}，同时入内存环形缓冲
 * - 内存缓冲最近 100 条供 DashboardHandler.alerts 拉取
 */
public class AlertBroadcaster {

    private static final int BUFFER_SIZE = 100;

    private final Set<WsContext> clients = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedDeque<AlertDTO> ringBuffer = new ConcurrentLinkedDeque<>();

    /** 注册新连接 */
    public void register(WsContext ctx) {
        clients.add(ctx);
    }

    /** 注销连接 */
    public void unregister(WsContext ctx) {
        clients.remove(ctx);
    }

    /** 推送给所有在线客户端，并缓存最近 100 条。 */
    public void broadcast(AlertDTO alert) {
        if (alert == null) {
            return;
        }
        // 入环形缓冲（容量超出则从尾部丢弃）
        ringBuffer.addFirst(alert);
        while (ringBuffer.size() > BUFFER_SIZE) {
            ringBuffer.removeLast();
        }
        // 推送
        String json = JsonMapper.toJson(WSMessage.alert(alert));
        for (Iterator<WsContext> it = clients.iterator(); it.hasNext(); ) {
            WsContext ctx = it.next();
            try {
                if (ctx.session.isOpen()) {
                    ctx.send(json);
                } else {
                    it.remove();
                }
            } catch (Exception e) {
                it.remove();
            }
        }
    }

    /** 取最近 100 条告警（最新在前）。 */
    public AlertDTO[] recentAlerts() {
        return ringBuffer.toArray(new AlertDTO[0]);
    }

    /** 当前在线 WS 客户端数量。 */
    public int clientCount() {
        return clients.size();
    }
}
