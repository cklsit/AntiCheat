package com.anticheat.detection.fusion;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DecisionActionCenter {

    // ==================== 玩家通知节流（防止聊天刷屏） ====================
    // notifyType -> (playerUUID -> lastSentEpochMs). 小容量 LRU + 线程安全。
    private static final Map<String, Map<UUID, Long>> NOTIFY_LAST_SENT = new ConcurrentHashMap<>();
    // 每种通知类型默认冷却（毫秒）。可后续接入 ConfigManager notify.throttleMs / detection.*.warningCooldownSecs。
    private static final long DEFAULT_COOLDOWN_NORMAL_MS = 5000L;
    private static final long DEFAULT_COOLDOWN_MONITOR_MS = 2500L;   // 增加监控：2.5s
    private static final long DEFAULT_COOLDOWN_CAPTCHA_MS = 5000L;   // 验证码提示：5s
    private static final long DEFAULT_COOLDOWN_TEMP_BAN_MS = 30000L; // 临时封禁：30s
    private static final long DEFAULT_COOLDOWN_PERM_BAN_MS = 60000L; // 永久封禁：60s
    private static final int NOTIFY_LRU_CAP = 5000;

    /**
     * 是否允许向玩家发送该类通知（节流判断）。若允许则自动更新最后发送时间。
     * @return true 表示应该发送，false 表示冷却中跳过。
     */
    private static synchronized boolean shouldSendNotify(String notifyType, UUID playerUuid, long cooldownMs) {
        Map<UUID, Long> bucket = NOTIFY_LAST_SENT.computeIfAbsent(notifyType,
                k -> Collections_Synchronized_LRU(NOTIFY_LRU_CAP));
        long now = System.currentTimeMillis();
        Long last = bucket.get(playerUuid);
        if (last != null && (now - last) < cooldownMs) {
            return false;
        }
        bucket.put(playerUuid, now);
        return true;
    }

    private static <K,V> Map<K,V> Collections_Synchronized_LRU(final int cap) {
        // 小工具：避免额外依赖 Guava Cache 时仍能简单节流（LinkedHashMap accessOrder + removeEldest）
        LinkedHashMap<K,V> lru = new LinkedHashMap<K,V>(16, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<K,V> eldest) { return size() > cap; }
            private static final long serialVersionUID = 1L;
        };
        return java.util.Collections.synchronizedMap(lru);
    }

    public enum ActionLevel {
        NORMAL(0.0, 0.5, "正常放行", 0),
        MONITOR(0.5, 0.75, "增加监控", 1),
        CAPTCHA(0.75, 0.95, "验证码审判", 2),
        TEMP_BAN(0.95, 0.995, "临时封禁", 3),
        PERM_BAN(0.995, 1.0, "永久封禁", 4);

        private final double minThreshold;
        private final double maxThreshold;
        private final String description;
        private final int severity;

        ActionLevel(double minThreshold, double maxThreshold, String description, int severity) {
            this.minThreshold = minThreshold;
            this.maxThreshold = maxThreshold;
            this.description = description;
            this.severity = severity;
        }

        public double getMinThreshold() {
            return minThreshold;
        }

        public double getMaxThreshold() {
            return maxThreshold;
        }

        public String getDescription() {
            return description;
        }

        public int getSeverity() {
            return severity;
        }

        public static ActionLevel fromRCP(double rcp) {
            for (ActionLevel level : values()) {
                if (rcp >= level.minThreshold && rcp < level.maxThreshold) {
                    return level;
                }
            }
            if (rcp >= 1.0) {
                return PERM_BAN;
            }
            return NORMAL;
        }
    }

    private final Map<UUID, ActionLevel> currentActions;
    private final Map<UUID, Long> actionTimestamps;
    private final Map<UUID, Integer> consecutiveActions;
    private final Map<UUID, Double> historicalRCP;

    private static final int MAX_CONSECUTIVE_CAPTCHA = 3;
    private static final long ACTION_COOLDOWN_MS = 60000;
    
    public DecisionActionCenter() {
        this.currentActions = new ConcurrentHashMap<>();
        this.actionTimestamps = new ConcurrentHashMap<>();
        this.consecutiveActions = new ConcurrentHashMap<>();
        this.historicalRCP = new ConcurrentHashMap<>();
    }
    
    public ActionLevel decide(UUID playerUUID, double rcp) {
        if (rcp < 0.0 || rcp > 1.0) {
            throw new IllegalArgumentException("RCP must be between 0.0 and 1.0");
        }
        
        ActionLevel previousAction = currentActions.get(playerUUID);
        ActionLevel newAction = ActionLevel.fromRCP(rcp);
        
        if (previousAction != null && previousAction.getSeverity() > newAction.getSeverity()) {
            Long lastTimestamp = actionTimestamps.get(playerUUID);
            if (lastTimestamp != null) {
                long timeSinceLastAction = System.currentTimeMillis() - lastTimestamp;
                if (timeSinceLastAction < ACTION_COOLDOWN_MS) {
                    return previousAction;
                }
            }
        }
        
        Integer consecutive = consecutiveActions.get(playerUUID);
        if (consecutive != null && consecutive >= MAX_CONSECUTIVE_CAPTCHA && newAction == ActionLevel.CAPTCHA) {
            return ActionLevel.TEMP_BAN;
        }
        
        currentActions.put(playerUUID, newAction);
        actionTimestamps.put(playerUUID, System.currentTimeMillis());
        
        updateHistoricalRCP(playerUUID, rcp);
        
        return newAction;
    }
    
    public void executeAction(UUID playerUUID, ActionLevel level) {
        Player player = Bukkit.getPlayer(playerUUID);
        
        if (player == null || !player.isOnline()) {
            logAction(playerUUID, level, "Player not online");
            return;
        }
        
        switch (level) {
            case NORMAL:
                handleNormalAction(player);
                break;
            case MONITOR:
                handleMonitorAction(player);
                break;
            case CAPTCHA:
                handleCaptchaAction(player);
                break;
            case TEMP_BAN:
                handleTempBanAction(player);
                break;
            case PERM_BAN:
                handlePermBanAction(player);
                break;
        }
        
        Integer consecutive = consecutiveActions.getOrDefault(playerUUID, 0);
        if (level == ActionLevel.CAPTCHA) {
            consecutiveActions.put(playerUUID, consecutive + 1);
        } else {
            consecutiveActions.put(playerUUID, 0);
        }
        
        logAction(playerUUID, level, "Action executed successfully");
    }
    
    private void handleNormalAction(Player player) {
        if (!shouldSendNotify("NORMAL", player.getUniqueId(), DEFAULT_COOLDOWN_NORMAL_MS)) return;
        player.sendMessage("§a[AntiCheat] §f您的行为正常，继续保持良好游戏体验！");
    }

    private void handleMonitorAction(Player player) {
        if (!shouldSendNotify("MONITOR", player.getUniqueId(), DEFAULT_COOLDOWN_MONITOR_MS)) {
            // 即使跳过消息，仍然需要开启监控（逻辑不丢）
            startEnhancedMonitoring(player);
            return;
        }
        player.sendMessage("§e[AntiCheat] §f我们注意到您的一些异常行为，将增加对您的监控。");
        startEnhancedMonitoring(player);
    }

    private void handleCaptchaAction(Player player) {
        boolean inCooldown = !shouldSendNotify("CAPTCHA", player.getUniqueId(), DEFAULT_COOLDOWN_CAPTCHA_MS);
        // 即使聊天提示跳过，仍然要启动验证码（否则可以通过快速违规来回避验证码）
        initiateCaptcha(player);
        if (inCooldown) return;
        player.sendMessage("§6[AntiCheat] §f为了确认您的身份，请完成验证码测试。");
    }

    private void handleTempBanAction(Player player) {
        if (!shouldSendNotify("TEMP_BAN", player.getUniqueId(), DEFAULT_COOLDOWN_TEMP_BAN_MS)) {
            // 封禁动作即便消息冷却也要执行（防止重复刷屏但不阻止封禁）
            applyTempBan(player);
            return;
        }
        player.sendMessage("§c[AntiCheat] §f检测到严重的作弊行为，您将被临时封禁。");
        applyTempBan(player);
    }

    private void handlePermBanAction(Player player) {
        if (!shouldSendNotify("PERM_BAN", player.getUniqueId(), DEFAULT_COOLDOWN_PERM_BAN_MS)) {
            applyPermBan(player);
            return;
        }
        player.sendMessage("§4[AntiCheat] §f检测到持续或严重的作弊行为，您将被永久封禁。");
        applyPermBan(player);
    }
    
    private void startEnhancedMonitoring(Player player) {
        // Integration point with monitoring system
    }
    
    private void initiateCaptcha(Player player) {
        // Integration point with CaptchaManager
    }
    
    private void applyTempBan(Player player) {
        // Integration point with BanManager
        // Default: 1 hour temp ban
    }
    
    private void applyPermBan(Player player) {
        // Integration point with BanManager
    }
    
    public ActionLevel getCurrentAction(UUID playerUUID) {
        return currentActions.get(playerUUID);
    }
    
    public boolean shouldTakeAction(UUID playerUUID, double rcp) {
        ActionLevel action = ActionLevel.fromRCP(rcp);
        return action != ActionLevel.NORMAL;
    }
    
    public long getTimeSinceLastAction(UUID playerUUID) {
        Long timestamp = actionTimestamps.get(playerUUID);
        if (timestamp == null) {
            return -1;
        }
        return System.currentTimeMillis() - timestamp;
    }
    
    public boolean isOnCooldown(UUID playerUUID) {
        Long timestamp = actionTimestamps.get(playerUUID);
        if (timestamp == null) {
            return false;
        }
        return System.currentTimeMillis() - timestamp < ACTION_COOLDOWN_MS;
    }
    
    private void updateHistoricalRCP(UUID playerUUID, double rcp) {
        historicalRCP.put(playerUUID, rcp);
    }
    
    public double getLatestRCP(UUID playerUUID) {
        return historicalRCP.getOrDefault(playerUUID, 0.0);
    }
    
    public void clearPlayerAction(UUID playerUUID) {
        currentActions.remove(playerUUID);
        consecutiveActions.remove(playerUUID);
        historicalRCP.remove(playerUUID);
    }
    
    private void logAction(UUID playerUUID, ActionLevel level, String message) {
        // Logging implementation
    }
    
    public int getConsecutiveActionCount(UUID playerUUID) {
        return consecutiveActions.getOrDefault(playerUUID, 0);
    }
    
    public void resetConsecutiveActions(UUID playerUUID) {
        consecutiveActions.put(playerUUID, 0);
    }
    
    public Map<String, Object> getActionStatistics(UUID playerUUID) {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("currentAction", currentActions.get(playerUUID));
        stats.put("consecutiveCount", getConsecutiveActionCount(playerUUID));
        stats.put("latestRCP", getLatestRCP(playerUUID));
        stats.put("lastActionTime", actionTimestamps.get(playerUUID));
        return stats;
    }
    
    public boolean shouldEscalate(UUID playerUUID, double rcp) {
        ActionLevel current = getCurrentAction(playerUUID);
        ActionLevel potential = ActionLevel.fromRCP(rcp);
        
        if (current == null) {
            return potential != ActionLevel.NORMAL;
        }
        
        return potential.getSeverity() > current.getSeverity();
    }
}
