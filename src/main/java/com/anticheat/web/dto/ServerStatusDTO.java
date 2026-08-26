package com.anticheat.web.dto;

/**
 * 服务器节点状态。与前端 ServerStatus 对齐。
 */
public class ServerStatusDTO {

    public String id;
    public String name;
    public int online;
    public double tps;
    /** 内存占用百分比 0-100 */
    public int memory;
    public String region;
    /** healthy / warning / critical */
    public String status;

    public ServerStatusDTO() {
    }

    public ServerStatusDTO(String id, String name, int online, double tps, int memory, String region, String status) {
        this.id = id;
        this.name = name;
        this.online = online;
        this.tps = tps;
        this.memory = memory;
        this.region = region;
        this.status = status;
    }
}
