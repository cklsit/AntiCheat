package com.anticheat.captcha;

import com.anticheat.AdvancedAntiCheat;
import com.anticheat.captcha.tasks.CaptchaTask;
import com.anticheat.captcha.tasks.TypeA_DirectInteraction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CaptchaManager {

    public enum Initiator {
        ADMIN,
        NEW_PLAYER,
        AUTO_DETECTION
    }

    private final AdvancedAntiCheat plugin;
    private final Map<UUID, CaptchaSession> activeSessions;
    private final CaptchaWorld captchaWorld;
    private final Random random;

    private boolean newPlayerCaptchaEnabled;
    private int timeLimit;

    public CaptchaManager(AdvancedAntiCheat plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
        this.captchaWorld = new CaptchaWorld(plugin);
        this.random = new Random();
        loadConfig();
    }

    private void loadConfig() {
        newPlayerCaptchaEnabled = plugin.getConfig().getBoolean("captcha.new-player-enabled", false);
        timeLimit = plugin.getConfig().getInt("captcha.time-limit", 45);
    }

    public void startCaptcha(Player player) {
        startCaptcha(player, Initiator.AUTO_DETECTION);
    }

    /**
     * 启动验证码。具备前置互斥与启动失败兜底：
     * <ul>
     *   <li>已有 captcha session 不重复启动</li>
     *   <li>玩家不在线不启动</li>
     *   <li>玩家正在被 CheckClientManager 查端（人工）时不启动，避免两个"传送+限制+计时"状态机冲突（互相 cancel teleport / 互相 cancel 事件）</li>
     * </ul>
     */
    public void startCaptcha(Player player, Initiator initiator) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        if (activeSessions.containsKey(uuid)) {
            return;
        }
        if (!player.isOnline()) {
            return;
        }
        // 互斥：查端系统 (startCheck) 会 cancel teleport、冻结玩家。两个并行会导致"验证码 teleport 被取消→session遗留→超时踢人"
        if (plugin.getCheckClientManager() != null && plugin.getCheckClientManager().isBeingChecked(uuid)) {
            player.sendMessage("§c你正在进行人工查端，无法同时进行验证码验证。");
            return;
        }
        // 若玩家已被封禁：由 PlayerJoinListener 踢人，不启动 captcha
        if (plugin.getBanManager() != null && plugin.getBanManager().isBanned(uuid)) {
            return;
        }

        Location originalLocation = player.getLocation().clone();
        Location captchaLocation = captchaWorld.getNextLocation();
        List<CaptchaTask> tasks = generateTasks();

        CaptchaSession session = new CaptchaSession(
                plugin,
                player,
                originalLocation,
                captchaLocation,
                tasks,
                timeLimit,
                initiator,
                activeSessions
        );

        activeSessions.put(uuid, session);

        try {
            session.start();
        } catch (Throwable t) {
            // 极端保护：start 抛异常时确保 activeSessions 不残留，避免下次无法启动 / 超时踢人
            plugin.getLogger().warning("[CaptchaManager] 启动验证码失败，清理 session: " + t.getMessage());
            activeSessions.remove(uuid);
        }
    }

    private List<CaptchaTask> generateTasks() {
        List<CaptchaTask> tasks = new ArrayList<>();
        int taskCount = random.nextInt(2) + 1;

        List<Class<? extends CaptchaTask>> taskTypes = new ArrayList<>(Arrays.asList(
                TypeA_DirectInteraction.class
        ));

        for (int i = 0; i < taskCount && i < taskTypes.size(); i++) {
            try {
                CaptchaTask task = taskTypes.get(i).getConstructor(AdvancedAntiCheat.class).newInstance(plugin);
                tasks.add(task);
            } catch (Exception e) {
                plugin.getLogger().severe("创建验证码任务失败: " + e.getMessage());
            }
        }

        return tasks;
    }

    public void completeTask(Player player) {
        CaptchaSession session = activeSessions.get(player.getUniqueId());
        if (session != null) {
            session.completeCurrentTask();
        }
    }

    public void failCaptcha(Player player) {
        CaptchaSession session = activeSessions.get(player.getUniqueId());
        if (session != null) {
            session.fail();
        }
    }

    public boolean isInCaptcha(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    public void removeSession(UUID uuid) {
        activeSessions.remove(uuid);
        
        if (activeSessions.isEmpty()) {
            captchaWorld.resetWorld();
        }
    }

    public void onPlayerQuit(Player player) {
        CaptchaSession session = activeSessions.get(player.getUniqueId());
        if (session != null) {
            session.fail();
        }
    }

    public boolean isNewPlayerCaptchaEnabled() {
        return newPlayerCaptchaEnabled;
    }

    public void setNewPlayerCaptchaEnabled(boolean enabled) {
        newPlayerCaptchaEnabled = enabled;
        plugin.getConfig().set("captcha.new-player-enabled", enabled);
        plugin.saveConfig();
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int seconds) {
        timeLimit = seconds;
        plugin.getConfig().set("captcha.time-limit", seconds);
        plugin.saveConfig();
    }

    public CaptchaWorld getCaptchaWorld() {
        return captchaWorld;
    }

    public CaptchaSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public static class CaptchaSession {

        private final AdvancedAntiCheat plugin;
        private final Player player;
        private final UUID playerUuid;
        private final Location originalLocation;
        private final Location captchaLocation;
        private final List<CaptchaTask> tasks;
        private final int timeLimit;
        private final Initiator initiator;
        private final Map<UUID, CaptchaSession> activeSessions;

        private int currentTaskIndex;
        private long startTime;
        private boolean completed;
        private boolean failed;
        /** 被守卫机制/外部打断：不惩罚也不传送，只是安静清理。 */
        private boolean cancelled;
        private BukkitRunnable timerTask;
        private BukkitRunnable warningTask;
        private CaptchaTask currentTask;

        public CaptchaSession(AdvancedAntiCheat plugin, Player player, Location originalLocation,
                             Location captchaLocation, List<CaptchaTask> tasks, int timeLimit,
                             Initiator initiator, Map<UUID, CaptchaSession> activeSessions) {
            this.plugin = plugin;
            this.player = player;
            this.playerUuid = player.getUniqueId();
            this.originalLocation = originalLocation;
            this.captchaLocation = captchaLocation;
            this.tasks = tasks;
            this.timeLimit = timeLimit;
            this.initiator = initiator;
            this.activeSessions = activeSessions;
            this.currentTaskIndex = 0;
            this.completed = false;
            this.failed = false;
            this.cancelled = false;
            this.currentTask = null;
        }

        public void start() {
            plugin.getCaptchaManager().getCaptchaWorld().preparePlayer(player);

            player.teleport(captchaLocation);

            // 启动消息按 initiator 区分：NEW_PLAYER 不提示"作弊行为"
            if (initiator == Initiator.NEW_PLAYER) {
                player.sendMessage("§e[!] §f新玩家验证：请在 " + timeLimit + " 秒内完成下方任务");
            } else {
                player.sendMessage("§c§l[!] §f由于你的行为触犯了反作弊系统，正在进行验证");
            }

            startTimer();
            startCurrentTask();

            // ============================================================
            // Teleport 成功守卫（核心修复：修复"瞬间传回来 session 残留→超时踢人"）
            //
            // 背景：其它插件（Essentials、AuthMe、多世界默认 spawn 传送、查端 cancel teleport）
            //       可能在 Join 阶段 / start() 同步调用后，把玩家从验证码世界抢回主世界。
            //       session 的 timerTask 不知道这件事，会正常计时然后 fail() 踢人。
            // 机制：2 tick 后检查玩家实际位置是否仍在验证码区域（同世界且距离 < 16 格）。
            //       如果不在，安静 cancel（不 ban / 不 kick / 不再 teleport，只清状态）。
            // ============================================================
            final UUID capturedUuid = playerUuid;
            final Location capturedCaptchaLoc = captchaLocation;
            new BukkitRunnable() {
                @Override
                public void run() {
                    // 已经自然结束的（complete/fail）不再介入
                    if (completed || failed || cancelled) return;
                    Player current = Bukkit.getPlayer(capturedUuid);
                    if (current == null || !current.isOnline()) {
                        // 玩家已下线：onPlayerQuit 会处理 fail，这里不重复
                        return;
                    }
                    Location now = current.getLocation();
                    boolean sameWorld = now.getWorld() != null
                            && capturedCaptchaLoc.getWorld() != null
                            && now.getWorld().getName().equals(capturedCaptchaLoc.getWorld().getName());
                    boolean nearCaptchaLocation = sameWorld && now.distance(capturedCaptchaLoc) < 16.0;
                    if (!nearCaptchaLocation) {
                        plugin.getLogger().info("[Captcha] 玩家 " + current.getName()
                                + " 被外部插件从验证码区域传送回主世界，安静取消验证码 (initiator=" + initiator + ")");
                        abortSilent();
                    }
                }
            }.runTaskLater(plugin, 2L);
        }

        private void startTimer() {
            startTime = System.currentTimeMillis();

            final long startTimeFinal = startTime;
            final int timeLimitFinal = timeLimit;

            timerTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (completed || failed || cancelled) {
                        this.cancel();
                        return;
                    }

                    long elapsed = (System.currentTimeMillis() - startTimeFinal) / 1000;
                    long remaining = timeLimitFinal - elapsed;

                    if (remaining <= 0) {
                        fail();
                        return;
                    }

                    // 玩家如果此刻不在线，保留任务等 onPlayerQuit fail，这里不额外处理以免干扰
                    try {
                        float progress = (float) remaining / timeLimitFinal;
                        player.setExp(progress);
                        player.setLevel((int) remaining);
                    } catch (Throwable ignored) {
                        // 玩家可能瞬间下线，ignore
                    }

                    if (remaining == 10) {
                        startWarning();
                    }
                }
            };

            timerTask.runTaskTimer(plugin, 0, 20);
        }

        private void startWarning() {
            warningTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (completed || failed || cancelled) {
                        this.cancel();
                        return;
                    }
                    try {
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    } catch (Throwable ignored) {
                    }
                }
            };

            warningTask.runTaskTimer(plugin, 0, 40);
        }

        private void startCurrentTask() {
            if (currentTaskIndex >= tasks.size()) {
                complete();
                return;
            }

            currentTask = tasks.get(currentTaskIndex);
            currentTask.start(player, captchaLocation);
            player.sendMessage("§e[任务 " + (currentTaskIndex + 1) + "/" + tasks.size() + "] " + currentTask.getTaskDescription());
        }

        public void completeCurrentTask() {
            if (completed || failed || cancelled) return;

            if (currentTask != null) {
                try {
                    currentTask.cleanup(player);
                } catch (Throwable ignored) {
                }
            }

            currentTaskIndex++;

            if (currentTaskIndex >= tasks.size()) {
                complete();
            } else {
                startCurrentTask();
            }
        }

        /**
         * 清理 Session 资源（timer / warning / tasks / world / exp / activeSessions）。
         * 由 complete() / fail() / abortSilent() 共用，避免三处重复代码导致 "恢复不完整，状态残留"。
         *
         * @param teleportBack 是否把玩家传 originalLocation（complete 成功时 true；abort 被外部传回主世界 false；fail 惩罚分支 false）
         */
        private void cleanup(boolean teleportBack) {
            if (timerTask != null) try { timerTask.cancel(); } catch (Throwable ignored) { timerTask = null; }
            if (warningTask != null) try { warningTask.cancel(); } catch (Throwable ignored) { warningTask = null; }

            cleanupTasks();
            try {
                plugin.getCaptchaManager().getCaptchaWorld().cleanup(captchaLocation);
            } catch (Throwable ignored) {
            }

            // 恢复玩家状态（EXP / Level / WalkSpeed），只针对在线玩家，避免 NPE
            Player current = Bukkit.getPlayer(playerUuid);
            if (current != null && current.isOnline()) {
                try {
                    current.setExp(0);
                    current.setLevel(0);
                    current.setWalkSpeed(0.2f);
                    current.setFlySpeed(0.2f);
                } catch (Throwable ignored) {
                }
                if (teleportBack) {
                    try {
                        if (!current.isDead()) {
                            current.teleport(originalLocation);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }

            activeSessions.remove(playerUuid);

            // 全部 session 结束重置世界（保持与 CaptchaManager.removeSession 一致的世界回收）
            if (activeSessions.isEmpty()) {
                try {
                    plugin.getCaptchaManager().getCaptchaWorld().resetWorld();
                } catch (Throwable ignored) {
                }
            }
        }

        private void complete() {
            if (completed || failed || cancelled) return;
            completed = true;
            cleanup(true);
            Player current = Bukkit.getPlayer(playerUuid);
            if (current != null && current.isOnline()) {
                current.sendMessage("§a§l[!] §f验证完毕");
            }
        }

        public void fail() {
            if (completed || failed || cancelled) return;
            failed = true;
            // fail 分支惩罚在 cleanup 之后执行：确保状态清完再 ban/kick，避免被 teleport 到验证码残留世界
            cleanup(false);
            Player current = Bukkit.getPlayer(playerUuid);
            if (current == null || !current.isOnline()) return;

            if (initiator != Initiator.NEW_PLAYER) {
                plugin.getBanManager().banPlayer(
                        current.getUniqueId(),
                        current.getName(),
                        "1d",
                        "验证码验证失败"
                );
            } else {
                current.kickPlayer("§c验证码验证失败，请重新加入服务器");
            }
        }

        /**
         * 被外部打断（其它插件 teleport、玩家在 Join 阶段被 spawn 传送抢回主世界、守卫检测到不在验证码区）。
         * 不惩罚、不 kick，仅安静清理，避免误踢。
         */
        private void abortSilent() {
            if (completed || failed || cancelled) return;
            cancelled = true;
            // 玩家已经被传出去了，不要再 teleportBack
            cleanup(false);
        }

        private void cleanupTasks() {
            Player current = Bukkit.getPlayer(playerUuid);
            boolean online = current != null && current.isOnline();
            for (CaptchaTask task : tasks) {
                try {
                    if (online) {
                        task.cleanup(current);
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        public CaptchaTask getCurrentTask() {
            return currentTask;
        }

        public int getCurrentTaskIndex() {
            return currentTaskIndex;
        }

        public int getTotalTasks() {
            return tasks.size();
        }

        public long getRemainingTime() {
            if (completed || failed) return 0;
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            return Math.max(0, timeLimit - elapsed);
        }

        public Initiator getInitiator() {
            return initiator;
        }

        public Player getPlayer() {
            return player;
        }
    }
}
