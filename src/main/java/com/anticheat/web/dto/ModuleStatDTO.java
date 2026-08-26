package com.anticheat.web.dto;

/**
 * 案件各模块触发统计。与前端 ModuleStat 对齐。
 */
public class ModuleStatDTO {

    public String name;
    public int triggerCount;
    public double avgScore;
    public int peakScore;

    public ModuleStatDTO() {
    }

    public ModuleStatDTO(String name, int triggerCount, double avgScore, int peakScore) {
        this.name = name;
        this.triggerCount = triggerCount;
        this.avgScore = avgScore;
        this.peakScore = peakScore;
    }
}
