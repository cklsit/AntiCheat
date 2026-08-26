package com.anticheat.web.dto;

/**
 * WebSocket 消息统一格式。
 * <pre>
 * { "type": "alert" | "pong" | "notification" | "system",
 *   "payload": {...},
 *   "timestamp": 1234567890 }
 * </pre>
 */
public class WSMessage<T> {

    public String type;
    public T payload;
    public long timestamp;

    public WSMessage() {
    }

    public WSMessage(String type, T payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    public static WSMessage<AlertDTO> alert(AlertDTO alert) {
        return new WSMessage<>("alert", alert);
    }

    public static WSMessage<String> pong() {
        return new WSMessage<>("pong", "__PONG__");
    }

    public static WSMessage<String> system(String text) {
        return new WSMessage<>("system", text);
    }
}
