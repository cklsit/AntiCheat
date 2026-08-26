package com.anticheat.web.dto;

/**
 * 检测模块配置视图。与前端 ConfigView 的 Threshold 行对齐。
 */
public class ConfigModuleDTO {

    public String id;
    /** 显示名（如 "战斗 / KillAura"） */
    public String name;
    /** 内部 key（如 "killaura"） */
    public String module;
    public boolean enabled;
    /** 自动封禁阈值（与 maxViolations 等价的可视化数值） */
    public int autoBan;
    /** 人工审理阈值（默认 70） */
    public int humanReview;

    public ConfigModuleDTO() {
    }

    public ConfigModuleDTO(String id, String name, String module, boolean enabled, int autoBan, int humanReview) {
        this.id = id;
        this.name = name;
        this.module = module;
        this.enabled = enabled;
        this.autoBan = autoBan;
        this.humanReview = humanReview;
    }
}
