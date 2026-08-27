package com.anticheat.bounty;

import com.anticheat.AdvancedAntiCheat;
import com.anticheat.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BountySession {
    private final AdvancedAntiCheat plugin;
    private final Player player;
    private final UUID uuid;
    private final Location originalLocation;
    private final Location originalBedSpawnLocation; // 进入赏金前的重生点（可能为 null = 默认世界）
    private final long startTime;
    private long timeLimitMinutes;
    private long timeSpentSeconds;
    private boolean inTask;
    private BountyTaskType currentTask;
    private long taskStartTime;
    private List<String> logs;
    private BukkitRunnable timerTask;
    private BukkitRunnable taskTimerTask;
    private int detectionCount;
    private int suspiciousCount;
    private Location pointALocation;
    private Location pointBLocation;

    public BountySession(AdvancedAntiCheat plugin, Player player, long timeLimitMinutes) {
        this.plugin = plugin;
        this.player = player;
        this.uuid = player.getUniqueId();
        this.originalLocation = player.getLocation().clone();
        // 保存原重生点（允许为 null 表示玩家没有设置床重生点，使用默认）
        Location bed;
        try {
            bed = player.getBedSpawnLocation();
        } catch (Throwable t) {
            bed = null;
        }
        this.originalBedSpawnLocation = bed;
        this.startTime = Instant.now().getEpochSecond();
        this.timeLimitMinutes = timeLimitMinutes;
        this.timeSpentSeconds = 0;
        this.inTask = false;
        this.currentTask = null;
        this.taskStartTime = 0;
        this.logs = new ArrayList<>();
        this.detectionCount = 0;
        this.suspiciousCount = 0;
        this.pointALocation = null;
        this.pointBLocation = null;
    }

    public void start() {
        plugin.getBountyManager().getBountyWorld().preparePlayer(player);
        // 先设置赏金世界为重生点（确保玩家在赏金世界死亡/断开后复活不会回到主世界床）
        Location bountySpawn = plugin.getBountyManager().getBountyWorld().getSpawnLocation();
        try {
            player.setBedSpawnLocation(bountySpawn, true);
        } catch (Throwable t) {
            plugin.getLogger().warning("[BountySession] 设置赏金重生点失败（1.8 兼容处理）: " + t.getMessage());
            try {
                // 1.8 单参数版本（无 force）
                player.setBedSpawnLocation(bountySpawn);
            } catch (Throwable ignored) {
            }
        }
        player.teleport(bountySpawn);

        player.sendMessage("§e================================================");
        player.sendMessage("§c§l欢迎来到漏洞赏金沙箱");
        player.sendMessage("§e================================================");
        player.sendMessage("§6你已被授权在此区域使用任何第三方工具");
        player.sendMessage("§6你的所有行为将被完整记录");
        player.sendMessage("§e================================================");
        player.sendMessage("§a使用 /bounty leave 退出沙箱");
        player.sendMessage("§a使用 /bounty report <描述> 主动报告发现");

        startTimer();
        log("[SESSION] Session started for player " + player.getName());
    }

    private void startTimer() {
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                timeSpentSeconds++;
                if (timeSpentSeconds >= timeLimitMinutes * 60) {
                    player.sendMessage("§c你的沙箱时长已用完，正在退出...");
                    plugin.getBountyManager().leaveBounty(player);
                    cancel();
                } else if (timeSpentSeconds % 300 == 0) {
                    long remaining = (timeLimitMinutes * 60 - timeSpentSeconds) / 60;
                    player.sendMessage("§e沙箱剩余时长：" + remaining + " 分钟");
                }
            }
        };
        timerTask.runTaskTimer(plugin, 0, 20);
    }

    public void startTask(BountyTaskType taskType) {
        this.currentTask = taskType;
        this.taskStartTime = Instant.now().getEpochSecond();
        this.inTask = true;
        this.detectionCount = 0;
        this.suspiciousCount = 0;

        player.sendMessage("§a开始任务：" + taskType.getDisplayName());
        player.sendMessage("§e" + taskType.getDescription());
        player.sendMessage("§6任务时间限制：" + taskType.getDurationMinutes() + " 分钟");
        player.sendMessage("§6系统将自动检测并评估你的表现");

        prepareTaskResources(taskType);

        log("[TASK] Started task: " + taskType.name());
        
        startTaskTimer(taskType.getDurationMinutes());
    }
    
    private void prepareTaskResources(BountyTaskType taskType) {
        World world = plugin.getBountyManager().getBountyWorld().getWorld();
        
        switch (taskType) {
            case MOVE_BASIC:
                setupMoveBasicTask(world);
                break;
            case MOVE_ADVANCED:
                setupMoveAdvancedTask(world);
                break;
            case COMBAT_BASIC:
                setupCombatBasicTask(world);
                break;
            case COMBAT_ADVANCED:
                setupCombatAdvancedTask(world);
                break;
            case INVENTORY_CHALLENGE:
                setupInventoryChallengeTask();
                break;
            case FREE_TEST:
                setupFreeTestTask();
                break;
        }
    }
    
    private void setupMoveBasicTask(World world) {
        pointALocation = new Location(world, -30, PLATFORM_HEIGHT, -30);
        pointBLocation = new Location(world, 30, PLATFORM_HEIGHT, 30);
        
        // 1.8 无彩色羊毛枚举：RED_WOOL/GREEN_WOOL → 使用 WOOL + DyeColor 数据值
        Material matA = VersionUtil.compatColoredWoolMaterial("RED_WOOL", DyeColor.RED);
        Material matB = VersionUtil.compatColoredWoolMaterial("GREEN_WOOL", DyeColor.GREEN);
        createPointMarker(pointALocation, matA, "A", DyeColor.RED);
        createPointMarker(pointBLocation, matB, "B", DyeColor.GREEN);
        
        player.sendMessage("§a起点 A (-30, " + PLATFORM_HEIGHT + ", -30) 已标记为红色羊毛");
        player.sendMessage("§a终点 B (30, " + PLATFORM_HEIGHT + ", 30) 已标记为绿色羊毛");
        player.sendMessage("§e尝试在不触发检测的情况下从A点到达B点");
    }
    
    private void setupMoveAdvancedTask(World world) {
        player.sendMessage("§e尝试完成空中直角变向");
        player.sendMessage("§6提示：在空中进行90度转向而不被检测");
    }
    
    private void setupCombatBasicTask(World world) {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 3));
        
        for (int i = 0; i < 5; i++) {
            Location spawnLoc = new Location(world, 
                (Math.random() - 0.5) * 20, 
                PLATFORM_HEIGHT + 1, 
                (Math.random() - 0.5) * 20
            );
            Zombie zombie = (Zombie) world.spawnEntity(spawnLoc, EntityType.ZOMBIE);
            zombie.setHealth(20);
            zombie.setCustomName("§c测试傀儡 " + (i + 1));
            zombie.setCustomNameVisible(true);
        }
        
        player.sendMessage("§a已给予钻石剑和金苹果");
        player.sendMessage("§a已生成5个测试傀儡");
        player.sendMessage("§e在10秒内击杀所有傀儡");
    }
    
    private void setupCombatAdvancedTask(World world) {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));
        
        player.sendMessage("§e尝试持续锁定目标而不被识别为杀戮光环");
        player.sendMessage("§6提示：使用准星持续对准一个点");
    }
    
    private void setupInventoryChallengeTask() {
        // 1.8 无不死图腾：回退为金苹果（仍能考验背包操作速度）
        Material totemCompat = VersionUtil.compatTotemOrFallback();
        player.getInventory().addItem(new ItemStack(totemCompat));
        if (totemCompat == Material.GOLDEN_APPLE) {
            player.sendMessage("§a已给予金苹果（1.8 替代图腾）");
        } else {
            player.sendMessage("§a已给予不死图腾");
        }
        player.sendMessage("§e快速将物品放到副手栏（1.8 无副手栏时直接放入快捷栏并选中）");
    }
    
    private void setupFreeTestTask() {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));
        player.getInventory().addItem(new ItemStack(Material.BOW));
        player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
        // 1.8 无不死图腾 → 回退为金苹果
        player.getInventory().addItem(new ItemStack(VersionUtil.compatTotemOrFallback()));
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 5));
        player.getInventory().addItem(new ItemStack(Material.POTION, 1, (short) 8226));
        
        World world = plugin.getBountyManager().getBountyWorld().getWorld();
        for (int i = 0; i < 3; i++) {
            Location spawnLoc = new Location(world, 
                (Math.random() - 0.5) * 15, 
                PLATFORM_HEIGHT + 1, 
                (Math.random() - 0.5) * 15
            );
            world.spawnEntity(spawnLoc, EntityType.ZOMBIE);
        }
        
        player.sendMessage("§a已给予测试道具：钻石剑、弓、箭矢、金苹果（或图腾）、金苹果、跳跃药水");
        player.sendMessage("§a已生成测试怪物");
        player.sendMessage("§e自由测试任何作弊功能");
    }
    
    private void createPointMarker(Location location, Material material, String label, DyeColor dye) {
        World world = location.getWorld();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block block = world.getBlockAt(location.getBlockX() + x, location.getBlockY() - 1, location.getBlockZ() + z);
                block.setType(material);
                // 1.8 WOOL 方块需 setData(DyeColor.getData()) 才有颜色；高版本 RED_WOOL 已是彩色，无需此行
                if (dye != null) {
                    VersionUtil.applyDyeColorIfLegacyWool(block, material, dye);
                }
            }
        }

        player.sendMessage("§6[" + label + "] 位置: (" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")");
    }
    
    private void startTaskTimer(long durationMinutes) {
        if (taskTimerTask != null) {
            taskTimerTask.cancel();
        }
        
        taskTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!inTask || currentTask == null) {
                    cancel();
                    return;
                }
                
                BountyResult result = evaluateResult();
                completeTask(result);
            }
        };
        
        taskTimerTask.runTaskLater(plugin, durationMinutes * 60 * 20);
    }
    
    private BountyResult evaluateResult() {
        if (detectionCount == 0 && suspiciousCount == 0) {
            return BountyResult.BYPASSED;
        } else if (detectionCount == 0 && suspiciousCount > 0) {
            return BountyResult.ZERO_DAY;
        } else {
            return BountyResult.DETECTED;
        }
    }
    
    public void recordDetection(String detectionType) {
        detectionCount++;
        log("[DETECTION] " + detectionType);
    }
    
    public void recordSuspicious(String suspiciousType) {
        suspiciousCount++;
        log("[SUSPICIOUS] " + suspiciousType);
    }

    public void completeTask(BountyResult result) {
        if (!inTask || currentTask == null) return;

        long taskDuration = Instant.now().getEpochSecond() - taskStartTime;
        log("[TASK] Task completed: " + currentTask.name() + " with result: " + result.name() + " in " + taskDuration + "s");

        switch (result) {
            case DETECTED:
                player.sendMessage("§e你的操作已被检测！感谢参与。");
                break;
            case BYPASSED:
                player.sendMessage("§a恭喜！你发现了一个潜在绕过！");
                BukkitRunnable broadcast = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (currentTask != null) {
                            Bukkit.broadcastMessage("§c§l[漏洞赏金] §a玩家 " + player.getName() + " 在 " + currentTask.getDisplayName() + " 中实现了潜在绕过！");
                        }
                    }
                };
                broadcast.runTask(plugin);
                break;
            case ZERO_DAY:
                player.sendMessage("§c§l高危发现！感谢你的贡献！");
                BukkitRunnable broadcastZero = new BukkitRunnable() {
                    @Override
                    public void run() {
                        Bukkit.broadcastMessage("§c§l[漏洞赏金] §e玩家 " + player.getName() + " 发现了一个高危绕过！该漏洞将被紧急修复。");
                    }
                };
                broadcastZero.runTask(plugin);
                break;
        }

        plugin.getBountyManager().saveEvidence(this, result);
        endTask();
    }

    public void endTask() {
        if (taskTimerTask != null) {
            taskTimerTask.cancel();
            taskTimerTask = null;
        }
        this.inTask = false;
        this.currentTask = null;
        this.taskStartTime = 0;
        this.detectionCount = 0;
        this.suspiciousCount = 0;
        this.pointALocation = null;
        this.pointBLocation = null;
    }

    public void end() {
        if (timerTask != null) {
            timerTask.cancel();
        }
        
        endTask();

        // 恢复玩家原重生点（先恢复，再 teleport/重置状态，避免服务器默认 spawn 逻辑覆盖）
        try {
            if (originalBedSpawnLocation != null) {
                try {
                    player.setBedSpawnLocation(originalBedSpawnLocation, true);
                } catch (Throwable t) {
                    try { player.setBedSpawnLocation(originalBedSpawnLocation); } catch (Throwable ignored) { }
                }
            } else {
                // 玩家原先生就是默认重生 → 清除 bed spawn：1.9+ setBedSpawnLocation(null)；1.8 需要反射或 NMS
                try {
                    player.setBedSpawnLocation(null, true);
                } catch (Throwable t) {
                    try { player.setBedSpawnLocation(null); } catch (Throwable ignored) {
                        // 1.8 无法直接清成 null，设置为原世界默认 spawn 作为兜底
                        Location defSpawn = Bukkit.getWorlds().get(0).getSpawnLocation();
                        try { player.setBedSpawnLocation(defSpawn); } catch (Throwable ignored2) { }
                    }
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[BountySession] 恢复玩家原重生点失败: " + t.getMessage());
        }

        // 传送回进入前的位置（resetPlayerState 内部会清理效果/装备等，但需要再 teleport）
        plugin.getBountyManager().getBountyWorld().resetPlayerState(player);

        // 玩家死亡时 teleport 会被服务器取消 → 暂存位置让 BountyListener.PlayerRespawnEvent 下一次复活时补传送
        boolean playerIsDead = false;
        try {
            playerIsDead = player.isDead() || player.getHealth() <= 0;
        } catch (Throwable ignored) {
        }
        if (playerIsDead) {
            plugin.getBountyManager().putPendingRespawnBackup(uuid, originalLocation);
            player.sendMessage("§c你在漏洞赏金沙箱中死亡，已自动退出沙箱。复活后将返回原位置。");
        } else {
            player.teleport(originalLocation);
        }

        log("[SESSION] Session ended for player " + player.getName() + " (dead=" + playerIsDead + ")");
    }

    public void log(String message) {
        String timestamp = Instant.now().toString();
        logs.add("[" + timestamp + "] " + message);
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Location getOriginalLocation() {
        return originalLocation;
    }

    public boolean isInTask() {
        return inTask;
    }

    public BountyTaskType getCurrentTask() {
        return currentTask;
    }

    public List<String> getLogs() {
        return new ArrayList<>(logs);
    }

    public long getTimeSpentSeconds() {
        return timeSpentSeconds;
    }
    
    private static final int PLATFORM_HEIGHT = 64;
}