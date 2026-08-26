package com.anticheat.repositories;

import com.anticheat.managers.BanManager.BanRecord;
import com.anticheat.managers.audit.AuditQuery;
import com.anticheat.managers.audit.AuditRecord;

import java.util.List;
import java.util.UUID;

public interface DatabaseRepository {

    void banPlayer(UUID playerUUID, String playerName, String reason,
                   String bannedBy, long banTime, long expiryTime, String serverName);

    boolean isPlayerBanned(UUID playerUUID);

    BanRecord getBanRecord(UUID playerUUID);

    void unbanPlayer(UUID playerUUID);

    List<BanRecord> getAllBans();

    void savePlayerProfile(UUID playerUUID, String serializedData);

    String loadPlayerProfile(UUID playerUUID);

    // ===== 审计日志 =====

    /** 建审计表（SQL 场景）。Mongo/Redis 可空实现。 */
    void ensureAuditTable();

    /** 插入一条审计记录。 */
    void saveAudit(AuditRecord audit);

    /** 按条件查询审计记录，按时间倒序返回当前页数据。 */
    List<AuditRecord> queryAudits(AuditQuery query);

    /** 按条件返回审计记录总数（用于分页）。 */
    long countAudits(AuditQuery query);

    void close();
}