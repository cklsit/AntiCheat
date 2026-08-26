package com.anticheat.repositories.impl;

import com.anticheat.managers.BanManager.BanRecord;
import com.anticheat.managers.audit.AuditQuery;
import com.anticheat.managers.audit.AuditRecord;
import com.anticheat.repositories.DatabaseRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class RedisRepository implements DatabaseRepository {
    
    private final JedisPool jedisPool;
    
    public RedisRepository(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }
    
    @Override
    public void banPlayer(UUID playerUUID, String playerName, String reason, 
                         String bannedBy, long banTime, long expiryTime, String serverName) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "anticheat:ban:" + playerUUID.toString();
            String data = playerUUID.toString() + "|" + playerName + "|" + reason + "|" + 
                         bannedBy + "|" + banTime + "|" + expiryTime + "|" + serverName;
            if (expiryTime > 0) {
                long ttl = (expiryTime - System.currentTimeMillis()) / 1000;
                if (ttl > 0) {
                    jedis.setex(key, ttl, data);
                }
            } else {
                jedis.set(key, data);
            }
            String playerListKey = "anticheat:banned_players";
            jedis.sadd(playerListKey, playerUUID.toString());
        } catch (Exception e) {
            throw new RuntimeException("封禁玩家Redis失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isPlayerBanned(UUID playerUUID) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "anticheat:ban:" + playerUUID.toString();
            String data = jedis.get(key);
            if (data != null) {
                String[] parts = data.split("\\|");
                if (parts.length >= 6) {
                    long expiryTime = Long.parseLong(parts[5]);
                    if (expiryTime > 0 && expiryTime < System.currentTimeMillis()) {
                        unbanPlayer(playerUUID);
                        return false;
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("检查封禁Redis失败: " + e.getMessage(), e);
        }
        return false;
    }
    
    @Override
    public BanRecord getBanRecord(UUID playerUUID) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "anticheat:ban:" + playerUUID.toString();
            String data = jedis.get(key);
            if (data != null) {
                String[] parts = data.split("\\|");
                if (parts.length >= 7) {
                    return new BanRecord(
                            UUID.fromString(parts[0]),
                            parts[1],
                            parts[2],
                            parts[3],
                            Long.parseLong(parts[4]),
                            Long.parseLong(parts[5]),
                            parts[6]
                    );
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("获取封禁记录Redis失败: " + e.getMessage(), e);
        }
        return null;
    }
    
    @Override
    public void unbanPlayer(UUID playerUUID) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "anticheat:ban:" + playerUUID.toString();
            jedis.del(key);
            String playerListKey = "anticheat:banned_players";
            jedis.srem(playerListKey, playerUUID.toString());
        } catch (Exception e) {
            throw new RuntimeException("解封玩家Redis失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<BanRecord> getAllBans() {
        List<BanRecord> bans = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> bannedPlayers = jedis.smembers("anticheat:banned_players");
            for (String playerUUID : bannedPlayers) {
                BanRecord record = getBanRecord(UUID.fromString(playerUUID));
                if (record != null) {
                    bans.add(record);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("获取所有封禁Redis失败: " + e.getMessage(), e);
        }
        return bans;
    }
    
    @Override
    public void savePlayerProfile(UUID playerUUID, String serializedData) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "anticheat:profile:" + playerUUID.toString();
            jedis.set(key, serializedData);
        } catch (Exception e) {
            throw new RuntimeException("保存玩家档案Redis失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String loadPlayerProfile(UUID playerUUID) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "anticheat:profile:" + playerUUID.toString();
            return jedis.get(key);
        } catch (Exception e) {
            throw new RuntimeException("加载玩家档案Redis失败: " + e.getMessage(), e);
        }
    }

    // ===== 审计日志（Redis List 存储） =====

    private static final String AUDIT_KEY = "anticheat:audit_logs";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    @Override
    public void ensureAuditTable() {
        // Redis 无需建表
    }

    @Override
    public void saveAudit(AuditRecord audit) {
        try (Jedis jedis = jedisPool.getResource()) {
            // lpush 保证最近记录在前；查询时直接 lrange 即可保持倒序
            jedis.lpush(AUDIT_KEY, GSON.toJson(audit));
            // 限制最多保留 10 万条，防止无界增长
            jedis.ltrim(AUDIT_KEY, 0, 99_999);
        } catch (Exception e) {
            throw new RuntimeException("保存审计记录Redis失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AuditRecord> queryAudits(AuditQuery query) {
        List<AuditRecord> all = loadAllAudits();
        List<AuditRecord> filtered = new ArrayList<>();
        for (AuditRecord r : all) {
            if (matches(r, query)) {
                filtered.add(r);
            }
        }
        int from = (query.getPage() - 1) * query.getPageSize();
        int to = Math.min(filtered.size(), from + query.getPageSize());
        if (from >= filtered.size()) {
            return new ArrayList<>();
        }
        return filtered.subList(from, to);
    }

    @Override
    public long countAudits(AuditQuery query) {
        return loadAllAudits().stream()
                .filter(r -> matches(r, query))
                .count();
    }

    private List<AuditRecord> loadAllAudits() {
        List<AuditRecord> list = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> raw = jedis.lrange(AUDIT_KEY, 0, -1);
            for (String s : raw) {
                AuditRecord r = GSON.fromJson(s, AuditRecord.class);
                if (r != null) {
                    list.add(r);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("加载审计记录Redis失败: " + e.getMessage(), e);
        }
        return list;
    }

    private boolean matches(AuditRecord r, AuditQuery q) {
        if (r == null) return false;
        if (q == null) return true;
        if (q.getType() != null && !q.getType().isEmpty()
                && !q.getType().equals(r.getType())) {
            return false;
        }
        if (q.getResult() != null && !q.getResult().isEmpty()
                && !q.getResult().equals(r.getResult())) {
            return false;
        }
        if (q.getStartTime() != null && r.getTimestamp() < q.getStartTime()) {
            return false;
        }
        if (q.getEndTime() != null && r.getTimestamp() > q.getEndTime()) {
            return false;
        }
        if (q.getKeyword() != null && !q.getKeyword().isEmpty()) {
            String kw = q.getKeyword().toLowerCase(Locale.ROOT);
            boolean hit = (r.getOperator() != null && r.getOperator().toLowerCase(Locale.ROOT).contains(kw))
                    || (r.getTarget() != null && r.getTarget().toLowerCase(Locale.ROOT).contains(kw))
                    || (r.getDetail() != null && r.getDetail().toLowerCase(Locale.ROOT).contains(kw));
            if (!hit) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}