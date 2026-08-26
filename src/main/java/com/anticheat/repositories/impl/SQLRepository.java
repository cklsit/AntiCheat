package com.anticheat.repositories.impl;

import com.anticheat.managers.BanManager.BanRecord;
import com.anticheat.managers.audit.AuditQuery;
import com.anticheat.managers.audit.AuditRecord;
import com.anticheat.repositories.DatabaseRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SQLRepository implements DatabaseRepository {
    
    private final Connection connection;
    
    public SQLRepository(Connection connection) {
        this.connection = connection;
    }
    
    public Connection getConnection() {
        return connection;
    }
    
    @Override
    public void banPlayer(UUID playerUUID, String playerName, String reason, 
                         String bannedBy, long banTime, long expiryTime, String serverName) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO bans (player_uuid, player_name, reason, banned_by, ban_time, expiry_time, server_name, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, 1)")) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, playerName);
            pstmt.setString(3, reason);
            pstmt.setString(4, bannedBy);
            pstmt.setLong(5, banTime);
            pstmt.setLong(6, expiryTime);
            pstmt.setString(7, serverName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("封禁玩家SQL失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isPlayerBanned(UUID playerUUID) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT expiry_time FROM bans WHERE player_uuid = ? AND is_active = 1 ORDER BY ban_time DESC LIMIT 1")) {
            pstmt.setString(1, playerUUID.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long expiryTime = rs.getLong("expiry_time");
                    if (expiryTime > 0 && expiryTime < System.currentTimeMillis()) {
                        unbanPlayer(playerUUID);
                        return false;
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("检查封禁SQL失败: " + e.getMessage(), e);
        }
        return false;
    }
    
    @Override
    public BanRecord getBanRecord(UUID playerUUID) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT * FROM bans WHERE player_uuid = ? AND is_active = 1 ORDER BY ban_time DESC LIMIT 1")) {
            pstmt.setString(1, playerUUID.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new BanRecord(
                            UUID.fromString(rs.getString("player_uuid")),
                            rs.getString("player_name"),
                            rs.getString("reason"),
                            rs.getString("banned_by"),
                            rs.getLong("ban_time"),
                            rs.getLong("expiry_time"),
                            rs.getString("server_name")
                    );
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("获取封禁记录SQL失败: " + e.getMessage(), e);
        }
        return null;
    }
    
    @Override
    public void unbanPlayer(UUID playerUUID) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "UPDATE bans SET is_active = 0 WHERE player_uuid = ?")) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("解封玩家SQL失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<BanRecord> getAllBans() {
        List<BanRecord> bans = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM bans WHERE is_active = 1 ORDER BY ban_time DESC")) {
            while (rs.next()) {
                bans.add(new BanRecord(
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("player_name"),
                        rs.getString("reason"),
                        rs.getString("banned_by"),
                        rs.getLong("ban_time"),
                        rs.getLong("expiry_time"),
                        rs.getString("server_name")
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("获取所有封禁SQL失败: " + e.getMessage(), e);
        }
        return bans;
    }
    
    @Override
    public void savePlayerProfile(UUID playerUUID, String serializedData) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO player_profiles (player_uuid, profile_data, last_updated) VALUES (?, ?, ?)")) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, serializedData);
            pstmt.setLong(3, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("保存玩家档案SQL失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String loadPlayerProfile(UUID playerUUID) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT profile_data FROM player_profiles WHERE player_uuid = ?")) {
            pstmt.setString(1, playerUUID.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("profile_data");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("加载玩家档案SQL失败: " + e.getMessage(), e);
        }
        return null;
    }

    // ===== 审计日志 =====

    @Override
    public void ensureAuditTable() {
        try (Statement stmt = connection.createStatement()) {
            // SQLite/H2 兼容写法：INTEGER PRIMARY KEY AUTOINCREMENT；
            // MySQL 环境下也兼容（其接受 INTEGER 与 AUTOINCREMENT 列别名）
            stmt.execute("CREATE TABLE IF NOT EXISTS audit_logs ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "timestamp BIGINT NOT NULL, "
                    + "operator VARCHAR(64), "
                    + "operator_role INTEGER, "
                    + "type VARCHAR(64), "
                    + "target VARCHAR(128), "
                    + "ip VARCHAR(64), "
                    + "result VARCHAR(16), "
                    + "detail TEXT"
                    + ")");
            try {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs(timestamp)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_audit_operator ON audit_logs(operator)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_audit_type ON audit_logs(type)");
            } catch (SQLException ignored) {
                // 部分方言对 IF NOT EXISTS 不支持，忽略
            }
        } catch (SQLException e) {
            throw new RuntimeException("创建审计表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveAudit(AuditRecord audit) {
        String sql = "INSERT INTO audit_logs "
                + "(timestamp, operator, operator_role, type, target, ip, result, detail) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, audit.getTimestamp());
            pstmt.setString(2, audit.getOperator());
            pstmt.setInt(3, audit.getOperatorRole());
            pstmt.setString(4, audit.getType());
            pstmt.setString(5, audit.getTarget());
            pstmt.setString(6, audit.getIp());
            pstmt.setString(7, audit.getResult());
            pstmt.setString(8, audit.getDetail());
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    audit.setId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("保存审计记录失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AuditRecord> queryAudits(AuditQuery query) {
        StringBuilder sql = new StringBuilder("SELECT * FROM audit_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendWhere(sql, params, query);
        sql.append(" ORDER BY timestamp DESC");
        int offset = (query.getPage() - 1) * query.getPageSize();
        sql.append(" LIMIT ? OFFSET ?");
        params.add(query.getPageSize());
        params.add(offset);

        List<AuditRecord> list = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询审计记录失败: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public long countAudits(AuditQuery query) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM audit_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendWhere(sql, params, query);
        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("统计审计记录失败: " + e.getMessage(), e);
        }
        return 0;
    }

    private void appendWhere(StringBuilder sql, List<Object> params, AuditQuery q) {
        if (q == null) return;
        if (q.getType() != null && !q.getType().isEmpty()) {
            sql.append(" AND type = ?");
            params.add(q.getType());
        }
        if (q.getResult() != null && !q.getResult().isEmpty()) {
            sql.append(" AND result = ?");
            params.add(q.getResult());
        }
        if (q.getStartTime() != null) {
            sql.append(" AND timestamp >= ?");
            params.add(q.getStartTime());
        }
        if (q.getEndTime() != null) {
            sql.append(" AND timestamp <= ?");
            params.add(q.getEndTime());
        }
        if (q.getKeyword() != null && !q.getKeyword().isEmpty()) {
            sql.append(" AND (operator LIKE ? OR target LIKE ? OR detail LIKE ?)");
            String kw = "%" + q.getKeyword() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
    }

    private AuditRecord mapRow(ResultSet rs) throws SQLException {
        AuditRecord r = new AuditRecord();
        r.setId(rs.getLong("id"));
        r.setTimestamp(rs.getLong("timestamp"));
        r.setOperator(rs.getString("operator"));
        r.setOperatorRole(rs.getInt("operator_role"));
        r.setType(rs.getString("type"));
        r.setTarget(rs.getString("target"));
        r.setIp(rs.getString("ip"));
        r.setResult(rs.getString("result"));
        r.setDetail(rs.getString("detail"));
        return r;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException("关闭SQL数据库连接失败: " + e.getMessage(), e);
        }
    }
}