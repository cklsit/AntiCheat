package com.anticheat.listeners;

import com.anticheat.AdvancedAntiCheat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class BountyListener implements Listener {
    private final AdvancedAntiCheat plugin;

    public BountyListener(AdvancedAntiCheat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getBountyManager().isInBounty(player)) {
            plugin.getBountyManager().leaveBounty(player);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getBountyManager().isInBounty(player)) {
            return;
        }

        String command = event.getMessage().toLowerCase();
        if (command.startsWith("/bounty ")) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage("§c在漏洞赏金沙箱中只能使用 /bounty 相关命令");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (plugin.getBountyManager().isInBounty(player)) {
            if (plugin.getBountyManager().getSession(player).isInTask()) {
                plugin.getBountyManager().getSession(player).log("[MOVE] Player moved: " + event.getTo().toString());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (plugin.getBountyManager().isInBounty(player)) {
            if (plugin.getBountyManager().getSession(player).isInTask()) {
                plugin.getBountyManager().getSession(player).log("[BLOCK] Block broken: " + event.getBlock().getType().name());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (plugin.getBountyManager().isInBounty(player)) {
            if (plugin.getBountyManager().getSession(player).isInTask()) {
                plugin.getBountyManager().getSession(player).log("[BLOCK] Block placed: " + event.getBlockPlaced().getType().name());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        if (plugin.getBountyManager().isInBounty(player)) {
            if (plugin.getBountyManager().getSession(player).isInTask()) {
                plugin.getBountyManager().getSession(player).log("[DAMAGE] Player took damage: " + event.getCause().name());
            }
        }
    }

    /**
     * 玩家在漏洞赏金沙箱中死亡 → 立即 leaveBounty（触发 session.end() 恢复床重生点 + 写入 pendingRespawnBackup）。
     * 死亡时 teleport 不生效，所以下一次复活事件中补传送。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeathInBounty(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!plugin.getBountyManager().isInBounty(player)) return;
        // 记录死因到 session 日志
        plugin.getBountyManager().getSession(player).log("[DEATH] Cause: " + (event.getDeathMessage() == null ? "unknown" : event.getDeathMessage()));
        // leaveBounty：内部 end() 会写 pendingRespawnBackup，因为此时玩家 isDead==true 不会立即 teleport
        try {
            plugin.getBountyManager().leaveBounty(player);
        } catch (Throwable t) {
            plugin.getLogger().warning("[BountyListener] 死亡时 leaveBounty 失败: " + t.getMessage());
        }
    }

    /**
     * 玩家从赏金死亡复活：
     * 1) 优先传送回进入赏金前的 originalLocation（pollPendingRespawnBackup 取出）
     * 2) 若没有 backup 但玩家仍然"在赏金世界但没 session"（理论不应存在，以防万一）→ 传送到主世界默认 spawn
     * 3) 1 tick 后再 teleport 一次，避免 1.8 Respawn 中 teleport 偶发不生效
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawnAfterBounty(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        Location backup = plugin.getBountyManager().pollPendingRespawnBackup(uuid);
        final World mainWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);

        if (backup != null) {
            // 让 respawn 点先正确（主世界），然后延迟 1 tick 再 teleport 回精确位置
            if (mainWorld != null) {
                event.setRespawnLocation(mainWorld.getSpawnLocation());
            }
            final Location dest = backup;
            new BukkitRunnable() {
                @Override public void run() {
                    if (player.isOnline()) {
                        try {
                            player.teleport(dest);
                            player.sendMessage("§a已将你送回进入赏金前的位置");
                        } catch (Throwable t) {
                            plugin.getLogger().warning("[BountyListener] Respawn 传送回原位置失败: " + t.getMessage());
                            if (mainWorld != null) player.teleport(mainWorld.getSpawnLocation());
                        }
                    }
                }
            }.runTask(plugin);
        } else {
            // 没有 backup：若玩家还在赏金世界但无 session（极端情况）→ 移回主世界
            boolean session = plugin.getBountyManager().isInBounty(player);
            if (!session && mainWorld != null && player.getWorld().equals(plugin.getBountyManager().getBountyWorld().getWorld())) {
                final Location dest = mainWorld.getSpawnLocation();
                new BukkitRunnable() {
                    @Override public void run() {
                        if (player.isOnline()) player.teleport(dest);
                    }
                }.runTask(plugin);
            }
        }
    }
}
