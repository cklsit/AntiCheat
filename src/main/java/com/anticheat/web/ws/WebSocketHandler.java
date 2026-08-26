package com.anticheat.web.ws;

import com.anticheat.web.auth.AuthManager;
import com.anticheat.web.dto.WSMessage;
import com.anticheat.web.util.JsonMapper;
import io.javalin.Javalin;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsConnectContext;

/**
 * WebSocket /ws 路由处理器。
 * <p>
 * - onConnect：校验 ?token=xxx，失败 close(4001)；成功注册到 AlertBroadcaster
 * - onMessage：处理 __PING__ 回 __PONG__
 * - onClose：注销
 */
public class WebSocketHandler {

    private final AuthManager authManager;
    private final AlertBroadcaster broadcaster;

    public WebSocketHandler(AuthManager authManager, AlertBroadcaster broadcaster) {
        this.authManager = authManager;
        this.broadcaster = broadcaster;
    }

    public void register(Javalin app) {
        app.ws("/ws", this::configure);
    }

    private void configure(WsConfig ws) {
        ws.onConnect(this::onConnect);
        ws.onMessage(this::onMessage);
        ws.onClose(this::onClose);
        ws.onError(this::onError);
    }

    private void onConnect(WsConnectContext ctx) {
        String token = ctx.queryParam("token");
        if (token == null || token.isEmpty() || authManager.verifyToken(token) == null) {
            ctx.closeSession(4001, "未授权");
            return;
        }
        ctx.attribute("web.token", token);
        broadcaster.register(ctx);
        try {
            ctx.send(JsonMapper.toJson(WSMessage.system("connected")));
        } catch (Exception ignored) {
        }
    }

    private void onMessage(io.javalin.websocket.WsMessageContext ctx) {
        String msg = ctx.message();
        if (msg == null) return;
        if ("__PING__".equalsIgnoreCase(msg.trim())) {
            ctx.send(JsonMapper.toJson(WSMessage.pong()));
        }
    }

    private void onClose(io.javalin.websocket.WsCloseContext ctx) {
        broadcaster.unregister(ctx);
    }

    private void onError(io.javalin.websocket.WsErrorContext ctx) {
        broadcaster.unregister(ctx);
    }
}
