package com.anticheat.managers;

import com.anticheat.AdvancedAntiCheat;
import com.anticheat.detection.ViolationRecord;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ViolationManager {
    private final AdvancedAntiCheat plugin;
    private final List<ViolationRecord> violationRecords;

    public ViolationManager(AdvancedAntiCheat plugin) {
        this.plugin = plugin;
        this.violationRecords = new ArrayList<>();
    }

    public void recordViolation(Player player, ViolationRecord.ViolationType type, String details, double level) {
        violationRecords.add(new ViolationRecord(
                player.getUniqueId(),
                player.getName(),
                type,
                details,
                level
        ));

        plugin.getDetectionManager().addViolation(player, type.name().toLowerCase());
    }

    public List<ViolationRecord> getViolationRecords() {
        return new ArrayList<>(violationRecords);
    }

    /**
     * 返回指定玩家的违规历史（按 UUID 过滤）。
     */
    public List<ViolationRecord> getViolationHistory(UUID uuid) {
        List<ViolationRecord> result = new ArrayList<>();
        for (ViolationRecord r : violationRecords) {
            if (uuid.equals(r.getPlayerUUID())) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * 返回所有有违规记录的玩家 UUID → 记录列表快照，供案件聚合 / 仪表盘统计使用。
     */
    public Map<UUID, List<ViolationRecord>> getViolationHistoryByPlayer() {
        Map<UUID, List<ViolationRecord>> result = new HashMap<>();
        for (ViolationRecord r : violationRecords) {
            result.computeIfAbsent(r.getPlayerUUID(), k -> new ArrayList<>()).add(r);
        }
        return result;
    }

    public void clearRecords() {
        violationRecords.clear();
    }
}
