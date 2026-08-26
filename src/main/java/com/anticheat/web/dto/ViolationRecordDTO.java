package com.anticheat.web.dto;

/**
 * 玩家违规历史记录。与前端 ViolationRecord 对齐。
 */
public class ViolationRecordDTO {

    public String id;
    public String module;
    public String type;
    public int score;
    public String time;
    public String server;
    public String details;

    public ViolationRecordDTO() {
    }

    public ViolationRecordDTO(String id, String module, String type, int score, String time, String server, String details) {
        this.id = id;
        this.module = module;
        this.type = type;
        this.score = score;
        this.time = time;
        this.server = server;
        this.details = details;
    }
}
