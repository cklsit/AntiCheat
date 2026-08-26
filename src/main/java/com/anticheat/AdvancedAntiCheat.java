package com.anticheat;

import com.anticheat.commands.*;
import com.anticheat.compat.CompatManager;
import com.anticheat.captcha.CaptchaManager;
import com.anticheat.bounty.BountyManager;
import com.anticheat.listeners.*;
import com.anticheat.managers.*;
import com.anticheat.profiles.BehaviorTracker;
import com.anticheat.profiles.PlayerProfile;
import com.anticheat.utils.VersionUtil;
import com.anticheat.web.WebServer;
import com.anticheat.web.auth.AuthManager;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class AdvancedAntiCheat extends JavaPlugin {

    private BanManager banManager;
    private ReportManager reportManager;
    private DetectionManager detectionManager;
    private ConfigManager configManager;
    private CheckClientManager checkClientManager;
    private CheckClientConfigManager checkClientConfigManager;
    private BehaviorTracker behaviorTracker;
    private CaptchaManager captchaManager;
    private BountyManager bountyManager;
    private ProfileManager profileManager;
    private com.anticheat.listeners.ProfileGUIListener profileGUIListener;
    private AdvancedDetectionManager advancedDetectionManager;

    // Web 面板相关
    private AuditManager auditManager;
    private AuthManager authManager;
    private WebServer webServer;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String version = VersionUtil.getVersion();
        boolean isHighVersion = VersionUtil.isHighVersion();

        getLogger().info("§6[AdvancedAntiCheat] 检测到服务器版本: " + version);
        getLogger().info("§6[AdvancedAntiCheat] 使用" + (isHighVersion ? "高版本" : "低版本") + "兼容模式");

        initializeManagers();
        registerListeners();
        registerCommands();

        startRiskDecayTask();

        // 启动 Web 面板（依赖 AuditManager、BanManager 已就绪）
        startWebPanel();

        getLogger().info("§2[AdvancedAntiCheat] 插件已成功启用！");
        getLogger().info("§6[AdvancedAntiCheat] 保护您的服务器免受作弊侵害！");
    }

    @Override
    public void onDisable() {
        // 先停 Web 面板，避免后续保存过程中触发脏推送
        if (webServer != null) {
            webServer.stop();
        }
        if (auditManager != null) {
            // AuditManager 当前无 close 钩子，预留扩展位
        }
        banManager.saveBans();
        reportManager.saveReports();
        checkClientManager.saveCheckData();
        if (behaviorTracker != null) {
            behaviorTracker.saveAllProfiles();
        }
        if (bountyManager != null) {
            bountyManager.onDisable();
        }
        if (profileManager != null) {
            profileManager.shutdown();
        }
        if (advancedDetectionManager != null) {
            advancedDetectionManager.shutdown();
        }
        getLogger().info("§4[AdvancedAntiCheat] 插件已禁用！");
    }

    private void initializeManagers() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        configManager = new ConfigManager(this);
        checkClientConfigManager = new CheckClientConfigManager(this);
        banManager = new BanManager(this);
        reportManager = new ReportManager(this);
        detectionManager = new DetectionManager(this);
        checkClientManager = new CheckClientManager(this);
        behaviorTracker = new BehaviorTracker(this);
        captchaManager = new CaptchaManager(this);
        bountyManager = new BountyManager(this);
        profileManager = new ProfileManager(this);

        advancedDetectionManager = new AdvancedDetectionManager(this);
        advancedDetectionManager.initialize(this);
    }

    /**
     * 启动内嵌 Web 面板：先初始化 AuditManager → AuthManager → WebServer。
     * WebServer.start() 内部在独立 daemon 线程启动 Javalin，不阻塞主线程。
     */
    private void startWebPanel() {
        try {
            auditManager = new AuditManager(this);
            authManager = new AuthManager(this);
            authManager.startCleaner();
            webServer = new WebServer(this, authManager);
            webServer.start();
        } catch (Throwable t) {
            getLogger().severe("[Web] Web 面板启动异常: " + t.getMessage());
            t.printStackTrace();
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        // AuthManager 配置热更
        if (authManager != null) {
            try {
                authManager.reload();
            } catch (Throwable t) {
                getLogger().warning("[Web] 重新加载 AuthManager 配置失败: " + t.getMessage());
            }
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerCommandListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerLoginListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerCheckListener(this), this);
        getServer().getPluginManager().registerEvents(new BehaviorListener(this), this);
        getServer().getPluginManager().registerEvents(new CaptchaListener(this), this);
        getServer().getPluginManager().registerEvents(new BountyListener(this), this);
        profileGUIListener = new com.anticheat.listeners.ProfileGUIListener(this);
        getServer().getPluginManager().registerEvents(profileGUIListener, this);

        if (VersionUtil.isHighVersion()) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new BungeeCordMessageListener(this));
        }
    }

    private void registerCommands() {
        getCommand("report").setExecutor(new ReportCommand(this));
        getCommand("goto").setExecutor(new GotoCommand(this));
        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("unban").setExecutor(new UnbanCommand(this));
        getCommand("anticheat").setExecutor(new AntiCheatCommand(this));
        getCommand("ac").setExecutor(new AntiCheatCommand(this));
        getCommand("checkclient").setExecutor(new CheckClientCommand(this));
        getCommand("checkdone").setExecutor(new CheckDoneCommand(this));
        getCommand("captcha").setExecutor(new CaptchaCommand(this));
        getCommand("bounty").setExecutor(new BountyCommand(this));
    }

    public BanManager getBanManager() {
        return banManager;
    }

    public DatabaseManager getDatabaseManager() {
        return banManager.getDatabaseManager();
    }

    public ReportManager getReportManager() {
        return reportManager;
    }

    public DetectionManager getDetectionManager() {
        return detectionManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CheckClientManager getCheckClientManager() {
        return checkClientManager;
    }

    public CheckClientConfigManager getCheckClientConfigManager() {
        return checkClientConfigManager;
    }

    public BehaviorTracker getBehaviorTracker() {
        return behaviorTracker;
    }

    public CaptchaManager getCaptchaManager() {
        return captchaManager;
    }

    public BountyManager getBountyManager() {
        return bountyManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public com.anticheat.listeners.ProfileGUIListener getProfileGUIListener() {
        return profileGUIListener;
    }

    public AdvancedDetectionManager getAdvancedDetectionManager() {
        return advancedDetectionManager;
    }

    public AuditManager getAuditManager() {
        return auditManager;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public WebServer getWebServer() {
        return webServer;
    }

    private void startRiskDecayTask() {
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (PlayerProfile profile : profileManager.getCachedProfiles().values()) {
                profile.decayRiskScore();
            }
        }, 20L * 60 * 60, 20L * 60 * 60);
    }
}
