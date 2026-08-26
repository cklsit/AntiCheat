package com.anticheat.web.dto;

/**
 * 通知条目。与前端 NotificationItem 对齐。
 */
public class NotificationItemDTO {

    public String id;
    /** info / warning / danger / success */
    public String level;
    public String title;
    public String content;
    public String playerName;
    public String playerUuid;
    public String time;
    public boolean read;
    public String category;

    public NotificationItemDTO() {
    }

    public NotificationItemDTO(String id, String level, String title, String content, String playerName,
                               String playerUuid, String time, boolean read, String category) {
        this.id = id;
        this.level = level;
        this.title = title;
        this.content = content;
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.time = time;
        this.read = read;
        this.category = category;
    }
}
