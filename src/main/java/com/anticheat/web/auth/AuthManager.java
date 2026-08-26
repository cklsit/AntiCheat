package com.anticheat.web.auth;

import com.anticheat.AdvancedAntiCheat;
import com.anticheat.managers.ConfigManager;
import com.anticheat.web.dto.LoginResultDTO;
import com.anticheat.web.dto.UserInfoDTO;
import com.anticheat.web.util.PasswordHasher;
import com.anticheat.web.util.TokenGenerator;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Web 面板会话与认证管理。
 * <ul>
 *   <li>启动时从 config 加载账号列表（不可热更，配置变更需 /ac reload）</li>
 *   <li>login 成功签发 UUID token，TTL 由 config.web.session-timeout-minutes 决定</li>
 *   <li>verifyToken 命中后自动续期</li>
 *   <li>每分钟清理过期 session</li>
 * </ul>
 */
public class AuthManager {

    private static final DateTimeFormatter ISO = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final AdvancedAntiCheat plugin;
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private volatile long ttlMillis;
    private BukkitRunnable cleaner;

    public AuthManager(AdvancedAntiCheat plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * 从 config 重新加载账号列表。已签发 token 不受影响。
     */
    public void reload() {
        ConfigManager cm = plugin.getConfigManager();
        ttlMillis = Math.max(1, cm.getWebSessionTimeoutMinutes()) * 60_000L;

        accounts.clear();
        for (Map<String, Object> entry : cm.getWebAccounts()) {
            Account acc = new Account();
            acc.username = String.valueOf(entry.getOrDefault("username", ""));
            acc.passwordHash = String.valueOf(entry.getOrDefault("password-hash", ""));
            acc.role = String.valueOf(entry.getOrDefault("role", "observer"));
            Object perms = entry.get("permissions");
            if (perms instanceof List<?> list) {
                List<String> ps = new ArrayList<>();
                for (Object p : list) {
                    ps.add(p == null ? "" : p.toString());
                }
                acc.permissions = Collections.unmodifiableList(ps);
            } else {
                acc.permissions = Collections.emptyList();
            }
            if (!acc.username.isEmpty()) {
                accounts.put(acc.username.toLowerCase(), acc);
            }
        }
        plugin.getLogger().info("[Web] 认证模块已加载 " + accounts.size() + " 个账号，会话 TTL=" + (ttlMillis / 60_000) + " 分钟");
    }

    /**
     * 启动定时清理任务。
     */
    public void startCleaner() {
        if (cleaner != null) {
            return;
        }
        cleaner = new BukkitRunnable() {
            @Override
            public void run() {
                purgeExpired();
            }
        };
        // 1200 ticks = 60 秒
        cleaner.runTaskTimerAsynchronously(plugin, 1200L, 1200L);
    }

    /**
     * 登录：校验通过签发 token 并附加 TTL。
     *
     * @return LoginResultDTO 含 token 与 UserInfo；登录失败返回 null。
     */
    public LoginResultDTO login(String username, String plainPassword, String ip) {
        if (username == null || plainPassword == null) {
            return null;
        }
        Account acc = accounts.get(username.toLowerCase());
        if (acc == null) {
            return null;
        }
        if (!PasswordHasher.verify(plainPassword, acc.passwordHash)) {
            return null;
        }
        String token = TokenGenerator.token();
        long now = System.currentTimeMillis();
        Session session = new Session();
        session.account = acc;
        session.token = token;
        session.expiresAt = now + ttlMillis;
        session.ip = ip == null ? "unknown" : ip;
        sessions.put(token, session);

        acc.lastLogin = ISO.format(Instant.ofEpochMilli(now));
        acc.lastIp = session.ip;

        UserInfoDTO user = new UserInfoDTO(
                "u-" + acc.username.toLowerCase(),
                acc.username,
                acc.nickname(),
                acc.roleOrdinal(),
                acc.avatarChar(),
                acc.lastLogin,
                acc.lastIp,
                new ArrayList<>(acc.permissions)
        );
        return new LoginResultDTO(token, user);
    }

    /**
     * 校验 token，命中后自动续期；失败或过期返回 null。
     */
    public Account verifyToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (session.expiresAt <= now) {
            sessions.remove(token);
            return null;
        }
        // 续期
        session.expiresAt = now + ttlMillis;
        return session.account;
    }

    public String getIp(String token) {
        Session s = sessions.get(token);
        return s == null ? "unknown" : s.ip;
    }

    public boolean logout(String token) {
        return sessions.remove(token) != null;
    }

    public Collection<Account> accounts() {
        return Collections.unmodifiableCollection(accounts.values());
    }

    public void purgeExpired() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(e -> e.getValue().expiresAt <= now);
    }

    public void shutdown() {
        if (cleaner != null) {
            try {
                cleaner.cancel();
            } catch (IllegalStateException ignored) {
                // 插件关闭期间可能已停用
            }
            cleaner = null;
        }
        sessions.clear();
    }

    private static class Session {
        Account account;
        String token;
        long expiresAt;
        String ip;
    }
}
