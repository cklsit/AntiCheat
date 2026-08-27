package com.anticheat.web.dto;

/**
 * 检测模块配置视图。与前端 ConfigView 的 Threshold 行对齐。
 * <p>
 * 字段分两组：
 * <ul>
 *   <li>真实配置字段（与 config.yml 中 detection.&lt;id&gt;.* 一一对应）：enabled / maxViolations /
 *       banTime / kickThreshold / humanReviewThreshold / warningCooldownSecs / notifyCooldownMs</li>
 *   <li>前端可视化派生字段（向后兼容旧 UI）：autoBan / humanReview</li>
 * </ul>
 */
public class ConfigModuleDTO {

    public String id;
    /** 显示名（如 "战斗 / KillAura"） */
    public String name;
    /** 内部 key（如 "killaura"） */
    public String module;
    public boolean enabled;
    /** 触发封禁前允许的最大违规数 */
    public int maxViolations;
    /** 封禁时长字符串（"30m"/"1h"/"1d"/"permanent"） */
    public String banTime;
    /** 达到该违规数时踢出玩家 */
    public int kickThreshold;
    /** 达到该违规数时升级人工审核 */
    public int humanReviewThreshold;
    /** 警告消息冷却（秒），避免同类刷屏 */
    public int warningCooldownSecs;
    /** 检测通知冷却（毫秒） */
    public long notifyCooldownMs;

    // ===== 向后兼容的派生字段（前端旧 UI 使用） =====
    /** 自动封禁阈值（与 maxViolations 等价的可视化数值，1-100） */
    public int autoBan;
    /** 人工审理阈值（默认 = humanReviewThreshold，旧 UI 字段） */
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
