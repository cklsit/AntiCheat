package com.anticheat.web.dto;

import java.util.List;

/**
 * Dashboard 统计聚合数据。与前端 DashboardStats 对齐。
 */
public class StatsDTO {

    public int onlinePlayers;
    public long totalPlayers;
    public int activeCases;
    public int todayViolations;
    public int todayBans;
    public List<RiskBucket> riskDistribution;
    public List<ModuleTrigger> moduleTriggers;
    public List<HourlyBucket> hourlyTrend;
    public List<ServerStatusDTO> serverStatus;

    public StatsDTO() {
    }

    /** 风险等级分布桶 */
    public static class RiskBucket {
        /** 0=LOW 1=MEDIUM 2=HIGH 3=EXTREME */
        public int level;
        public long count;
        public double percent;

        public RiskBucket() {
        }

        public RiskBucket(int level, long count, double percent) {
            this.level = level;
            this.count = count;
            this.percent = percent;
        }
    }

    /** 模块触发统计 */
    public static class ModuleTrigger {
        public String module;
        public int count;
        public double trend;

        public ModuleTrigger() {
        }

        public ModuleTrigger(String module, int count, double trend) {
            this.module = module;
            this.count = count;
            this.trend = trend;
        }
    }

    /** 每小时桶 */
    public static class HourlyBucket {
        public String hour;
        public int violations;
        public int bans;

        public HourlyBucket() {
        }

        public HourlyBucket(String hour, int violations, int bans) {
            this.hour = hour;
            this.violations = violations;
            this.bans = bans;
        }
    }
}
