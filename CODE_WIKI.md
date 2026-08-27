# AdvancedAntiCheat · Code Wiki

> 版本：v2.1.0
> 仓库：[cklsit/AntiCheat](https://github.com/cklsit/AntiCheat)
> 许可证：MIT
> 适用服务端：Minecraft 1.8.x — 1.21.x（Paper / Purpur / Spigot / FlamePaper）

本文档系统性地说明 AdvancedAntiCheat（以下简称 AAC）的整体架构、模块职责、关键类与函数、依赖关系以及运行方式，便于二次开发、运维与代码审阅。

---

## 目录

1. [项目概述](#1-项目概述)
2. [整体架构](#2-整体架构)
3. [主要模块职责](#3-主要模块职责)
4. [关键类与函数说明](#4-关键类与函数说明)
5. [依赖关系](#5-依赖关系)
6. [项目运行方式](#6-项目运行方式)

---

## 1. 项目概述

AdvancedAntiCheat 是一个面向 Minecraft 1.8.x – 1.21.x 全版本跨度的高级反作弊插件，核心能力包括：

- **多层反作弊检测**：飞行、速度、KillAura、Reach、ESP、FastBreak、Scaffold、NoSlow 等基础检测，叠加 **运动 / 战斗 / 物理引擎 / 关联分析 / 概率融合** 高级检测体系。
- **概率融合与决策中心**：基于贝叶斯网络与自适应学习的 RCP（Real-time Cheating Probability）实时评分，输出 NORMAL / MONITOR / CAPTCHA / TEMP_BAN / PERM_BAN 五级处置。
- **玩家画像系统**：行为追踪、瞄准分析、挖矿模式、背包状态机、身份指纹、风险历史、关联图，为决策提供长程上下文。
- **客户端检查 / 验证码 / 漏洞赏金**：三套人工介入与白盒测试机制，含专用世界与会话管理。
- **智能封禁系统**：临时 / 永久封禁、跨服务器数据库同步（BungeeCord / Velocity）。
- **内嵌 Web 管理面板**：Javalin HTTP + WebSocket，Vue 3 SPA 前端（总览、玩家、案件、配置、审计、AI 实验室、联盟图谱、实时地图、回放），支持 RBAC 与审计日志。
- **版本兼容层**：通过 `VersionUtil` + `compat` 子包反射处理 1.8 与 1.19+ API 差异（如 `Material.COBWEB`、`Sheep.setInvulnerable` 等）。

### 仓库组成

| 顶级目录 | 说明 |
|----------|------|
| `src/main/java/com/anticheat/` | Java 插件源码 |
| `src/main/resources/` | 配置文件（config.yml / checkclient.yml / messages.yml） |
| `web-panel/` | Vue 3 + Vite 前端工程 |
| `plugin.yml` | 插件元数据、命令、权限 |
| `pom.xml` | Maven 构建配置（含前端构建集成） |
| `.github/workflows/` | CI/CD 工作流（build / nightly-release / test-dispatch） |
| `spec.md` / `tasks.md` / `check_list.md` | 项目规格、任务清单与检查清单 |

---

## 2. 整体架构

### 2.1 分层架构

AAC 采用 **事件驱动 + 分层处理** 架构，自下而上分为五层：

```
┌─────────────────────────────────────────────────────────────┐
│                    Web 管理面板（Vue 3 SPA）                │
│        总览 / 玩家 / 案件 / 配置 / 审计 / AI实验室 / 联盟      │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP REST + WebSocket (Javalin)
┌─────────────────────────┴───────────────────────────────────┐
│ L5 表现层：WebServer / WebRouter / Handlers / AuthFilter     │
│   BukkitBridge（主线程桥接） / WebSocketHandler / Audit       │
└─────────────────────────┬───────────────────────────────────┘
                          │ Bukkit API（主线程同步）
┌─────────────────────────┴───────────────────────────────────┐
│ L4 管理层：BanManager / ReportManager / CheckClientManager   │
│   CaptchaManager / BountyManager / ProfileManager            │
│   AuditManager / DatabaseManager / ConfigManager              │
│   AdvancedDetectionManager / DetectionCoordinator            │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────┴───────────────────────────────────┐
│ L3 决策融合层：DecisionActionCenter（五级处置）              │
│   ProbabilityFusionEngine / BayesianNetwork / RCPComputer     │
│   AdaptiveLearningSystem（自适应权重学习）                   │
└─────────────────────────┬───────────────────────────────────┘
                          │ DetectionResult + Evidence
┌─────────────────────────┴───────────────────────────────────┐
│ L2 检测执行层：movement / combat / association / network    │
│   physics（物理模拟）/ inventory / 蜜罐（Honeypot）          │
│   BaseDetectionModule + DetectionModule 接口                │
└─────────────────────────┬───────────────────────────────────┘
                          │ Bukkit Events
┌─────────────────────────┴───────────────────────────────────┐
│ L1 数据采集层：listeners（11 个监听器） + BehaviorTracker    │
│   PlayerProfile / BehaviorFeatures / HoneypotListener         │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 数据流

1. **采集**：Bukkit 事件触发 `listeners`，原始事件转发至 `BehaviorTracker` / `DetectionManager` / `AdvancedDetectionManager`。
2. **画像**：`BehaviorTracker` 将事件累计到 `PlayerProfile`（含 `BehaviorFeatures`、`AimAnalysis`、`MiningPatternAnalyzer`、`InventoryStateMachine` 等子结构）。
3. **检测**：L2 各检测模块继承 `BaseDetectionModule`，实现 `analyze()` / `calculateProbability()`，产出 `DetectionResult` + `Evidence`。
4. **融合**：`ProbabilityFusionEngine` 收集多模块概率，经 `BayesianNetwork` 推断，由 `RCPComputer` 结合先验、网络延迟、趋势得出 RCP。
5. **决策**：`DecisionActionCenter` 按 RCP 阈值分级处置：通知 → 监控 → 验证码 → 临时封禁 → 永久封禁。
6. **执行**：`BanManager` / `CaptchaManager` / `BountyManager` 等落地处置，并通过 `DatabaseManager` 持久化。
7. **可视化**：Web 层通过 `BukkitBridge.syncSupply/syncRun` 回主线程取数据，`WebSocketHandler` / `AlertBroadcaster` 实时推送给前端。

### 2.3 关键设计原则

- **跨版本兼容**：所有版本相关 API 调用收敛至 `utils/VersionUtil` 与 `compat` 包，使用反射规避低版本缺失字段/方法。
- **主线程安全**：Web 线程严禁直接调用 Bukkit API，必须经 `BukkitBridge` 同步或异步派发。
- **解耦**：检测模块通过 `DetectionModule` 接口标准化，新增检测只需继承 `BaseDetectionModule`。
- **降级机制**：`PerformanceMonitor` 监控 TPS/CPU/内存，触发阈值时进入降级模式，减少检测频次。
- **依赖隔离**：Maven Shade 重定位所有第三方包至 `com.anticheat.libs.*`，避免与宿主插件冲突。

---

## 3. 主要模块职责

### 3.1 顶层入口

| 文件 | 职责 |
|------|------|
| [AdvancedAntiCheat.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/AdvancedAntiCheat.java) | 插件主类，`onEnable` 初始化全部 Manager、注册监听器与命令、启动 Web 面板与风险衰减任务；`onDisable` 按序关闭 Web、保存数据、shutdown Profile/AdvancedDetection。 |
| [plugin.yml](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/plugin.yml) | 声明命令（report/goto/ban/unban/anticheat/ac/checkclient/checkdone/captcha/bounty）与权限节点。 |

### 3.2 模块速览表

| 包 | 模块定位 | 关键类 |
|----|----------|--------|
| `detection.core` | 检测模块抽象基座 | `DetectionModule`、`BaseDetectionModule`、`DetectionResult`、`Evidence` |
| `detection` (顶层) | 单项违规检测 | `FlyDetection`、`SpeedDetection`、`KillAuraDetection`、`ReachDetection`、`EspDetection`、`FastBreakDetection`、`ScaffoldDetection`、`NoSlowDetection`、`Detection`、`ViolationManager`、`ViolationRecord` |
| `detection.fusion` | 概率融合与决策 | `ProbabilityFusionEngine`、`BayesianNetwork`、`RCPComputer`、`AdaptiveLearningSystem`、`DecisionActionCenter` |
| `detection.movement` | 运动检测 | `MovementDetectionModule`、`ImpossibleActionDetector`、`MovementViolation`、`MovementViolationType` |
| `detection.combat` | 战斗检测 | `CombatDetectionModule`、`AimbotHardLockDetector`、`ReachValidator`、`CPSLimiter` |
| `detection.association` | 关联与团队作弊 | `AssociationDetector`、`AltAccountDetector`、`TeamCheatingDetector`、`BehaviorSimilarityCalculator`、`SocialGraph`、`DeviceFingerprint`、`HistoricalBaselineComparator`、`OfflineRuleScanner`、`DetectionRule`、`AltAccount`、`CheatingTeam`、`InteractionType` |
| `detection.physics` | 物理模拟 | `PhysicsSimulator`、`PhysicsConstants`、`MovementInput`、`EntitySnapshot`、`Vector3D` |
| `detection.network` | 协议校验 | `ProtocolValidator` |
| `detection.inventory` | 物品栏检测 | (README only) |
| `managers` | 业务管理器 | `ConfigManager`、`BanManager`、`ReportManager`、`DetectionManager`、`AdvancedDetectionManager`、`DetectionCoordinator`、`CheckClientManager`、`CheckClientConfigManager`、`CaptchaManager`(在 captcha 包)、`BountyManager`(在 bounty 包)、`ProfileManager`、`AuditManager`、`DatabaseManager`、`PerformanceMonitor`、`ViolationManager` |
| `managers.audit` | 审计数据结构 | `AuditRecord`、`AuditQuery` |
| `listeners` | Bukkit 事件监听 | 11 个监听器（见 3.4） |
| `commands` | 命令执行器 | 10 个命令（见 3.5） |
| `profiles` | 玩家画像与行为分析 | `PlayerProfile`、`BehaviorTracker`、`BehaviorAnalysisEngine`、`BehaviorFeatures`、`AimAnalysis`、`MiningPatternAnalyzer`、`InventoryStateMachine`、`IdentityFingerprint`、`AssociationGraph`、`RiskHistory`、`NormalDistribution`、`TimerDetection` |
| `captcha` | 验证码系统 | `CaptchaManager`、`CaptchaWorld`、`tasks/CaptchaTask`、`tasks/TypeA_DirectInteraction` |
| `bounty` | 漏洞赏金系统 | `BountyManager`、`BountySession`、`BountyResult`、`BountyTaskType`、`BountyWorld` |
| `compat` | 版本兼容层 | `CompatManager`、`ChatCompat`、`ChatCompat1_8`、`ChatCompat1_19` |
| `repositories` | 数据访问层 | `DatabaseRepository`、`impl/SQLRepository`、`impl/MongoRepository`、`impl/RedisRepository` |
| `utils` | 工具类 | `VersionUtil`、`ProfileSerializer` |
| `gui` | 游戏内 GUI | `ProfileGUI` |
| `web` | Web 后端 | `WebServer`、`WebRouter`、`BukkitBridge`、`ws/WebSocketHandler`、`ws/AlertBroadcaster` |
| `web.auth` | Web 认证 | `AuthManager`、`AuthFilter`、`Account`、`Permission` |
| `web.handler` | Web 路由处理器 | `AbstractHandler`、`AuthHandler`、`PlayerHandler`、`CaseHandler`、`DashboardHandler`、`ConfigHandler`、`AuditHandler`、`NotificationHandler` |
| `web.dto` | 数据传输对象 | 17 个 DTO（AlertDTO、ApiResp、AuditDTO、CaseDTO、ConfigModuleDTO、EvidenceSummaryDTO、LinkedAccountDTO、LoginResultDTO、ModuleStatDTO、NotificationItemDTO、PageDTO、PlayerDTO、ServerStatusDTO、StatsDTO、UserInfoDTO、ViolationRecordDTO、WSMessage） |
| `web.util` | Web 工具 | `JsonMapper`、`PasswordHasher`、`TokenGenerator`、`InetAddressUtil` |
| `web-panel/` | 前端 SPA | Vue 3 + Vite + Pinia + Vue Router + ECharts + TailwindCSS |

### 3.3 检测模块详解

#### 3.3.1 基础检测（`detection` 顶层）

继承自 `Detection` 抽象层，按违规计数（ViolationManager）达到 `maxViolations` 触发封禁：

- **FlyDetection**：飞行检测，支持创造模式排除。
- **SpeedDetection**：移动速度异常检测。
- **KillAuraDetection**：杀戮光环检测。
- **ReachDetection**：攻击距离异常检测。
- **EspDetection**：透视 / 实体追踪检测。
- **FastBreakDetection**：破坏方块速度异常。
- **ScaffoldDetection**：自动搭桥检测。
- **NoSlowDetection**：使用物品时未减速检测。

#### 3.3.2 高级检测（L2）

通过 `AdvancedDetectionManager` 统一编排，分为三层：

- **L1 事件检测**：`MovementDetectionModule`、`CombatDetectionModule` 监听原始事件。
- **L2 行为分析**：`BehaviorAnalysisEngine` 基于画像判断异常分数。
- **L3 关联检测**：`AssociationDetector` + `TeamCheatingDetector` + `AltAccountDetector` 通过社交图与设备指纹识别小号/团队作弊。

每个检测子包提供独立检测器：

| 子包 | 检测器 | 关注点 |
|------|--------|--------|
| movement | `ImpossibleActionDetector` | 空中二段跳、不可能角度变向 |
| combat | `AimbotHardLockDetector` | 视角锁定式 Aimbot |
| combat | `ReachValidator` | 攻击距离 NORMAL/MAX/ABSOLUTE 三级 |
| combat | `CPSLimiter` | 点击频率限制 |
| association | `HistoricalBaselineComparator` | 与历史基线对比 |
| association | `OfflineRuleScanner` | 离线规则扫描 |
| physics | `PhysicsSimulator` | 客户端物理模拟复算 |

### 3.4 监听器（listeners）

| 监听器 | 职责 |
|--------|------|
| `PlayerMoveListener` | 移动事件采样，触发 fly / speed 检测 |
| `PlayerJoinListener` | 清除查端残留效果、读取封禁、踢出被封玩家 |
| `PlayerLoginListener` | 登录阶段拦截封禁玩家 |
| `PlayerCommandListener` | 玩家造成伤害事件 → killaura / reach 检测 |
| `PlayerCheckListener` | 客户端检查期间严格限制玩家行为，退出即封 |
| `BehaviorListener` | 行为数据采集，转发至 `BehaviorTracker` |
| `CaptchaListener` | 新玩家自动验证码、验证码状态下的移动/交互处理 |
| `BountyListener` | 赏金会话期间的命令限制、行为日志、死亡复活处理 |
| `HoneypotListener` | 周期性生成诱饵矿石与幽灵实体，捕获异常行为 |
| `ProfileGUIListener` | 玩家档案 GUI 交互处理 |
| `BungeeCordMessageListener` | 跨服插件消息接收 |

### 3.5 命令（commands）

| 命令类 | 对应指令 | 权限 |
|--------|----------|------|
| `AntiCheatCommand` | `/ac reload|stats|reports|help|profile|genpwd` | `anticheat.admin` |
| `BanCommand` | `/ban <玩家> [时间] [原因]` | `anticheat.ban` |
| `UnbanCommand` | `/unban <玩家>` | `anticheat.unban` |
| `ReportCommand` | `/report <玩家> <原因>` | `anticheat.report` |
| `GotoCommand` | `/goto <玩家>`（支持跨服） | `anticheat.goto` |
| `CheckClientCommand` | `/checkclient <玩家> <QQ号>` | `anticheat.checkclient` |
| `CheckDoneCommand` | `/checkdone <玩家>` | `anticheat.checkclient` |
| `CaptchaCommand` | `/captcha <玩家|toggle|timelimit>` | `anticheat.captcha` |
| `BountyCommand` | `/bounty enter|leave|invite|report|lb|start|complete` | `anticheat.bounty` |

### 3.6 数据访问层（repositories）

`DatabaseRepository` 接口定义统一的数据操作契约，三个实现对应不同后端：

- **SQLRepository**：支持 SQLite / H2 / MySQL，分页查询、玩家档案与审计日志持久化。
- **MongoRepository**：MongoDB 文档型存储。
- **RedisRepository**：Redis 缓存型存储。

`DatabaseManager` 根据 `config.yml` 的 `database.type` 工厂式创建对应实现。

### 3.7 Web 后端（web）

详见 [4.6 Web 后端](#46-web-后端)。

### 3.8 前端（web-panel）

详见 [4.7 前端工程](#47-前端工程)。

---

## 4. 关键类与函数说明

### 4.1 主类与生命周期

#### `AdvancedAntiCheat`（主插件类）

[AdvancedAntiCheat.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/AdvancedAntiCheat.java)

| 方法 | 说明 |
|------|------|
| `onEnable()` | 加载默认配置 → 检测版本 → `initializeManagers()` → `registerListeners()` → `registerCommands()` → `startRiskDecayTask()` → `startWebPanel()` |
| `onDisable()` | 关闭 WebServer → 保存封禁/举报/查端数据 → 保存画像 → 关闭 BountyManager/ProfileManager/AdvancedDetectionManager |
| `initializeManagers()` | 创建 ConfigManager、BanManager、ReportManager、DetectionManager、CheckClientManager、BehaviorTracker、CaptchaManager、BountyManager、ProfileManager、AdvancedDetectionManager |
| `startWebPanel()` | 初始化 AuditManager → AuthManager（启动清理器）→ WebServer.start()（独立 daemon 线程） |
| `startRiskDecayTask()` | 每小时异步遍历缓存画像，调用 `PlayerProfile.decayRiskScore()` |
| `getXXManager()` | 一系列 getter 暴露各 Manager 单例 |

### 4.2 检测核心（detection.core）

#### `DetectionModule`（接口）

[detection/core/DetectionModule.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/detection/core/DetectionModule.java)

定义所有检测模块统一行为：

| 方法 | 说明 |
|------|------|
| `check()` | 执行检测入口 |
| `isEnabled()` | 模块是否启用 |
| `getName()` | 模块名称 |
| `getProbability()` | 当前作弊概率 [0,1] |

#### `BaseDetectionModule`（抽象基类）

[detection/core/BaseDetectionModule.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/detection/core/BaseDetectionModule.java)

提供通用违规计数、冷却、启用状态管理；子类必须实现：

| 抽象方法 | 说明 |
|----------|------|
| `analyze()` | 执行具体分析逻辑 |
| `getAnalysisData()` | 返回当前分析数据 |
| `calculateProbability()` | 基于分析数据计算概率 |

#### `DetectionResult` / `Evidence`

| 类 | 字段/方法 | 说明 |
|----|-----------|------|
| `DetectionResult` | `playerUUID` / `moduleName` / `probability` / `evidence` / `timestamp` | 单次检测结果封装 |
| `Evidence` | `Map<String, Object> data` | 键值型证据容器 |

### 4.3 概率融合与决策（detection.fusion）

#### `ProbabilityFusionEngine`

[detection/fusion/ProbabilityFusionEngine.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/detection/fusion/ProbabilityFusionEngine.java)

负责融合各检测模块的概率输出，维护模块权重与贝叶斯网络，接收模块概率证据。

#### `BayesianNetwork`

维护先验概率、条件概率与证据集合，是概率融合与贝叶斯推断的核心数据结构。

#### `RCPComputer`

调用 `ProbabilityFusionEngine` + `AdaptiveLearningSystem`，计算玩家实时作弊概率（RCP），应用先验、网络延迟、趋势分析。

#### `AdaptiveLearningSystem`

基于历史数据自适应调整各检测模块权重。

#### `DecisionActionCenter`

[detection/fusion/DecisionActionCenter.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/detection/fusion/DecisionActionCenter.java)

定义五级处置策略：

| 等级 | 含义 |
|------|------|
| `NORMAL` | 正常 |
| `MONITOR` | 加强监控 |
| `CAPTCHA` | 触发验证码 |
| `TEMP_BAN` | 临时封禁 |
| `PERM_BAN` | 永久封禁 |

包含同类提示冷却（防止聊天刷屏）。

### 4.4 玩家画像（profiles）

#### `PlayerProfile`

[profiles/PlayerProfile.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/profiles/PlayerProfile.java)

玩家档案核心结构，组合以下子模块：

| 子结构 | 职责 |
|--------|------|
| `BehaviorFeatures` | 移动/战斗/挖矿/背包/社交五大特征 |
| `AimAnalysis` | 视角变化、瞄准平滑度、方差 |
| `MiningPatternAnalyzer` | 挖矿时间规律，识别自动化 |
| `InventoryStateMachine` | 背包状态转移，识别自动换装备 |
| `IdentityFingerprint` | 历史名称/IP/客户端版本/语言/硬件 |
| `AssociationGraph` | 账号关联与高频交互 |
| `RiskHistory` | 违规与验证码历史、风险分数衰减 |
| `NormalDistribution` | 在线均值/方差/Z-score/异常判断 |
| `TimerDetection` | 操作时间间隔标准差，识别固定节奏自动化 |

关键方法：`decayRiskScore()`（每小时衰减风险分）。

#### `BehaviorTracker`

[profiles/BehaviorTracker.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/profiles/BehaviorTracker.java)

行为分析数据入口：

| 方法 | 说明 |
|------|------|
| `onJoin/onQuit` | 创建或保存 PlayerProfile |
| `recordClick/Animation/Move/Interact` | 采集各类事件，更新画像 |
| `saveAllProfiles()` | 退出时全量保存 |

#### `BehaviorAnalysisEngine`

[profiles/BehaviorAnalysisEngine.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/profiles/BehaviorAnalysisEngine.java)

| 方法 | 说明 |
|------|------|
| `initGlobalBaseline()` | 初始化全局行为基线 |
| `periodicAnalyze()` | 周期性分析任务 |
| `updatePlayerMetrics()` | 更新玩家行为指标 |
| `calculateAnomalyScore()` | 计算异常分数，超阈值触发处理 |

### 4.5 管理器（managers）

#### `ConfigManager`

[managers/ConfigManager.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/managers/ConfigManager.java)

集中读写 `config.yml`，覆盖：

| 区域 | 配置项 |
|------|--------|
| 通知节流 | `notify.throttleMs` |
| 检测项 | `detection.{fly|speed|esp|killaura|reach}` 的 `enabled/maxViolations/banTime/kickThreshold/humanReviewThreshold/warningCooldownSecs/notifyCooldownMs` |
| 封禁 | `ban.minTime/maxTime` |
| 数据库 | `database.type/server-name/sqlite/h2/mysql/redis/mongodb` |
| 查端 | `check-client.timeout-minutes` |
| 赏金 | `bounty.enabled/default-time-limit-minutes` |
| Web | `web.enabled/host/port/session-timeout-minutes/cors/auth.accounts` |

关键方法：`isWebEnabled()` / `getWebHost()` / `getWebPort()` / `getWebSessionTimeoutMinutes()` / `isWebCorsEnabled()` / `getWebAccounts()` / 检测项 `getXxxEnabled/setXxxEnabled` 系列供 Web 配置中心读写。

#### `BanManager`

[managers/BanManager.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/managers/BanManager.java)

封禁管理核心，组合 `DatabaseManager` 实现持久化：

| 方法 | 说明 |
|------|------|
| `isBanned(player)` | 查询封禁状态 |
| `banPlayer(player, duration, reason)` | 封禁并踢人、保存、广播 |
| `unbanPlayer(player)` | 解除封禁 |
| `saveBans()` | 持久化内存封禁表 |

#### `DetectionManager`（基础）

[managers/DetectionManager.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/managers/DetectionManager.java)

| 方法 | 说明 |
|------|------|
| `registerDetections()` | 注册多种基础检测模块 |
| `addViolation(player, type)` | 累计违规计数 |
| 达到 `maxViolations` | 调用 `BanManager.banPlayer` 执行封禁 |

#### `AdvancedDetectionManager`（高级）

[managers/AdvancedDetectionManager.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/managers/AdvancedDetectionManager.java)

分层编排高级检测体系：

| 方法 | 说明 |
|------|------|
| `initialize(plugin)` | 分层初始化运动/战斗/蜜罐、行为分析、关联检测、融合决策中心 |
| `runDetection(player)` | 按 L1/L2/L3 触发检测 |
| `calculateRCP(player)` | 调用 RCPComputer 计算实时作弊概率 |
| `enforceDecision(player, action)` | 通过 DecisionActionCenter 执行处罚 |
| `shutdown()` | 关闭所有子模块 |

#### `DatabaseManager`

[managers/DatabaseManager.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/managers/DatabaseManager.java)

| 方法 | 说明 |
|------|------|
| `init()` | 根据 `database.type` 工厂式创建 `DatabaseRepository` |
| `saveBan/queryBan/unbanPlayer` | 异步保存/查询/解封封禁记录 |
| `saveAudit/queryAudits/countAudits` | 异步保存/同步查询/统计审计日志 |

#### `ProfileManager`

[managers/ProfileManager.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/managers/ProfileManager.java)

| 方法 | 说明 |
|------|------|
| `getCachedProfiles()` | 返回画像缓存（LRU 淘汰） |
| `getProfile(uuid)` | 获取或异步加载/创建画像 |
| `shutdown()` | 定时保存与关闭 |

#### `PerformanceMonitor`

[managers/PerformanceMonitor.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/managers/PerformanceMonitor.java)

监控 TPS/CPU/内存，按阈值切换降级模式，控制检测频率与调整任务间隔。

#### `AuditManager` / `AuditRecord` / `AuditQuery`

Web 审计核心，记录所有 Web 操作日志，支持按 `AuditQuery` 查询与统计。

### 4.6 Web 后端（web）

#### `WebServer`

[web/WebServer.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/web/WebServer.java)

内嵌 Javalin，独立 daemon 线程启动：

| 方法 | 说明 |
|------|------|
| `start()` | 读取 `ConfigManager` 的 Web 开关/主机/端口，配置 CORS，注册 `AuthFilter` / `WebSocketHandler` / `WebRouter` |
| `stop()` | 关闭 Javalin 实例 |

提供静态资源服务与 SPA fallback，全局异常处理。

#### `WebRouter`

[web/WebRouter.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/web/WebRouter.java)

集中注册 REST 路由，实例化并挂载以下 Handler 到 Javalin：

`AuthHandler` / `PlayerHandler` / `CaseHandler` / `DashboardHandler` / `AuditHandler` / `NotificationHandler` / `ConfigHandler`。

#### `BukkitBridge`

[web/BukkitBridge.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/web/BukkitBridge.java)

跨线程桥接工具：

| 方法 | 说明 |
|------|------|
| `syncSupply(supplier)` | 同步等待最多 5 秒回主线程取值 |
| `syncRun(runnable)` | 异步 fire-and-forget 回主线程执行 |

所有 Web 线程访问 Bukkit API 必须经过此桥。

#### `WebSocketHandler` / `AlertBroadcaster`

`WebSocketHandler` 处理 WS 连接生命周期；`AlertBroadcaster` 向所有在线管理员推送实时反作弊告警与案件更新。

#### `AuthManager` / `AuthFilter` / `Account` / `Permission`

[web/auth/AuthManager.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/web/auth/AuthManager.java)

| 类 | 说明 |
|----|------|
| `AuthManager` | 从 `ConfigManager.getWebAccounts()` 加载账号（bcrypt hash + role + permissions），`login()` 校验密码签发 token，`verifyToken()` 校验/续期，`startCleaner()` 定时清理过期 session，`reload()` 配置热更 |
| `AuthFilter` | 拦截 `/api/*`（放行 login/register），从 `Authorization: Bearer <token>` 提取并校验，成功则写入 `ctx.session(account/token)` |
| `Account` | 账号实体（username、password hash、role、permissions） |
| `Permission` | 权限解析与匹配（支持 `*` 通配，如 `players:*`） |

#### Handler 体系

[web/handler/](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/web/handler)

| Handler | 路由 | 职责 |
|---------|------|------|
| `AbstractHandler` | - | 基类，统一提供账号、IP、JSON 响应、审计日志写入能力 |
| `AuthHandler` | `POST /api/auth/login`、`POST /api/auth/logout`、`GET /api/auth/me` | 认证 |
| `PlayerHandler` | `/api/players/**` | 玩家列表、详情、画像、封禁 |
| `CaseHandler` | `/api/cases/**` | 案件审理、裁决 |
| `DashboardHandler` | `/api/dashboard/**` | 总览统计、模块状态、服务器状态 |
| `ConfigHandler` | `/api/config/**` | 检测模块配置列表与更新（`ConfigModuleDTO`，经 `BukkitBridge.syncRun` 主线程写配置） |
| `AuditHandler` | `/api/audit/**` | 审计日志分页查询 |
| `NotificationHandler` | `/api/notifications/**` | 通知列表与已读 |

#### 工具类

| 类 | 说明 |
|----|------|
| `JsonMapper` | Gson 封装的 JSON 序列化/反序列化 |
| `PasswordHasher` | 基于 BCrypt 的密码哈希生成与校验 |
| `TokenGenerator` | UUID/Base64URL token 生成 |
| `InetAddressUtil` | 解析客户端 IP（`X-Forwarded-For` / `X-Real-IP` 优先，回退 `Context.ip()`） |

### 4.7 前端工程（web-panel）

#### 技术栈

[web-panel/package.json](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/web-panel/package.json)

- 框架：Vue 3.5 + Vue Router 4 + Pinia 2
- HTTP：Axios
- 图表：ECharts 5 + vue-echarts
- UI：TailwindCSS 3 + lucide-vue-next（图标）
- 构建：Vite 5 + vue-tsc + TypeScript 5

#### 入口

[web-panel/src/main.ts](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/web-panel/src/main.ts)

创建 Vue app，挂载 Pinia + Router，引入全局样式后挂载到 `#app`。

[web-panel/src/App.vue](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/web-panel/src/App.vue)

根组件，处理小屏警告、监听 resize，正常宽度下渲染 `<router-view>`。

#### 路由

[web-panel/src/router/index.ts](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/web-panel/src/router/index.ts)

所有业务路由懒加载，`beforeEach` 全局守卫执行登录态校验与页面标题设置。`requiresAuth: true` 的路由需先登录，否则跳转 `/login`。

| 路由 | 视图 | 标题 |
|------|------|------|
| `/login` | `LoginView` | 登录 |
| `/dashboard` | `DashboardView` | 总览 |
| `/players` | `PlayersView` | 玩家管理 |
| `/players/:id` | `PlayerDetailView` | 玩家详情 |
| `/cases` | `CasesView` | 案件审理 |
| `/cases/:id` | `CaseReviewView` | 案件审理详情 |
| `/live-map` | `LiveMapView` | 实时地图 |
| `/replay` | `ReplayView` | 违规回放 |
| `/ai-lab` | `AILabView` | AI 实验室 |
| `/config` | `ConfigView` | 系统配置 |
| `/audit` | `AuditLogView` | 审计日志 |
| `/alliance` | `AllianceView` | 联盟图谱 |
| `/:pathMatch(.*)*` | `NotFoundView` | 404 |

#### Stores（Pinia）

| Store | 文件 | 职责 |
|-------|------|------|
| auth | [stores/auth.ts](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/web-panel/src/stores/auth.ts) | token/user/登录失败次数/锁定状态，提供 login、logout、restoreSession、hasPerm |
| global | `stores/global.ts` | 全局 UI 状态 |
| notification | `stores/notification.ts` | 通知列表与未读计数 |

#### API 层

[web-panel/src/api/request.ts](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/web-panel/src/api/request.ts)

Axios 实例封装：token 注入、统一响应拦截、错误 toast，导出 `get/post/put/delete`。

| 模块 | 职责 |
|------|------|
| `auth.ts` | 登录、登出、获取当前用户 |
| `dashboard.ts` | 总览统计、模块状态 |
| `players.ts` | 玩家列表、详情、封禁 |
| `cases.ts` | 案件列表、审理、裁决 |
| `audit.ts` | 审计日志查询 |
| `mock/` | 开发环境模拟数据 |

#### 组件层

**布局组件**（layout）：

| 组件 | 说明 |
|------|------|
| [Shell.vue](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/web-panel/src/components/layout/Shell.vue) | 主布局，组合 Sidebar/TopNav/NotificationPanel/CommandPalette，处理全局 Ctrl+K、通知面板、命令面板交互 |
| `Sidebar.vue` | 侧边栏导航 |
| `TopNav.vue` | 顶部导航 |
| `CommandPalette.vue` | 命令面板（Ctrl+K） |
| `NotificationPanel.vue` | 通知面板 |

**通用组件**（common）：

| 组件 | 说明 |
|------|------|
| `DataTable.vue` | 通用数据表格 |
| `Modal.vue` | 模态框 |
| `Drawer.vue` | 抽屉 |
| `StatCard.vue` | 统计卡片 |
| `RiskBadge.vue` / `RiskIndicator.vue` | 风险等级标识 |
| `StatusDot.vue` | 状态点 |
| `LineChart.vue` / `BarChart.vue` / `RingProgress.vue` | ECharts 图表封装 |

#### 视图层

13 个视图对应 13 个路由（见上表），每个视图负责对应业务页面的展示与交互。

#### 工具层

| 文件 | 职责 |
|------|------|
| `utils/ws.ts` | WebSocket 连接与消息分发 |
| `utils/format.ts` | 时间/数字格式化 |
| `utils/risk.ts` | 风险等级计算与颜色映射 |
| `utils/mockDelay.ts` | 模拟网络延迟 |

### 4.8 验证码与赏金系统

#### `CaptchaManager` + `CaptchaWorld`

[captcha/CaptchaManager.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/captcha/CaptchaManager.java)

- 生成并管理验证码任务，新玩家验证码、任务类型选择与 session 创建。
- 内部类 `CaptchaSession` 执行验证码流程：分配任务、倒计时、完成/失败处理、清理资源。

[captcha/CaptchaWorld.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/captcha/CaptchaWorld.java)

创建并销毁验证码专用世界，含自定义 chunk generator 与固定出生点。

#### `BountyManager` + `BountySession`

[bounty/BountyManager.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/bounty/BountyManager.java)

管理漏洞赏金会话：进入、离开、报告、保存证据、清理。

[bounty/BountySession.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/bounty/BountySession.java)

单次会话生命周期，支持六种任务类型（MOVE_BASIC / MOVE_ADVANCED / COMBAT_BASIC / COMBAT_ADVANCED / INVENTORY_CHALLENGE / FREE_TEST），自动评估为 DETECTED / BYPASSED / ZERO_DAY 三种结果。

### 4.9 版本兼容与工具

#### `VersionUtil`

[utils/VersionUtil.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/utils/VersionUtil.java)

解析并缓存服务器版本，提供 `getVersion()` / `isHighVersion()` / `is1_8()` / `is1_19()` 等判断，并通过反射处理低版本缺失字段（如 `Material.COBWEB`）。

#### `CompatManager` + `ChatCompat*`

[compat/CompatManager.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/compat/CompatManager.java)

根据服务器版本动态选择 `ChatCompat1_19` 或 `ChatCompat1_8` 兼容实现。`ChatCompat1_8` 通过反射构造 BungeeCord 消息与 Minecraft 协议包，规避 1.8 API 差异。

#### `ProfileSerializer`

[utils/ProfileSerializer.java](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/src/main/java/com/anticheat/utils/ProfileSerializer.java)

`PlayerProfile` 的序列化与反序列化，用于跨模块/存储传输玩家档案。

---

## 5. 依赖关系

### 5.1 Java 依赖（pom.xml）

[pom.xml](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/pom.xml)

| 依赖 | 版本 | 作用域 | 用途 |
|------|------|--------|------|
| `io.papermc.paper:paper-api` | 1.21.11-R0.1-SNAPSHOT | provided | Paper API（Paper profile） |
| `org.spigotmc:spigot-api` | 1.8.8-R0.1-SNAPSHOT | provided | Spigot API（Spigot profile） |
| `com.h2database:h2` | 2.2.224 | compile | H2 嵌入式数据库 |
| `com.mysql:mysql-connector-j` | 8.3.0 | compile | MySQL 驱动 |
| `org.xerial:sqlite-jdbc` | 3.45.1.0 | compile | SQLite 驱动 |
| `redis.clients:jedis` | 5.1.0 | compile | Redis 客户端 |
| `org.mongodb:mongodb-driver-sync` | 4.11.1 | compile | MongoDB 驱动 |
| `net.md-5:bungeecord-chat` | 1.8-SNAPSHOT | provided | BungeeCord 聊天组件 |
| `com.google.code.gson:gson` | 2.10.1 | compile | JSON 序列化 |
| `io.javalin:javalin` | 6.1.6 | compile | 内嵌 Web 服务器（排除 logback/slf4j-api） |
| `org.slf4j:slf4j-jdk14` | 2.0.13 | compile | SLF4J 桥接至 JDK logging |
| `at.favre.lib:bcrypt` | 0.10.2 | compile | BCrypt 密码哈希 |

### 5.2 Maven Profiles

| Profile | API | 默认 |
|---------|-----|------|
| `paper` | paper-api 1.21.11 | 是（`activeByDefault`） |
| `spigot` | spigot-api 1.8.8 | 否 |

两个 profile 在 GitHub Actions matrix 中并行构建。

### 5.3 Maven Shade 重定位

所有第三方包重定位至 `com.anticheat.libs.*`，避免与宿主插件冲突：

- `com.h2database` → `com.anticheat.libs.h2database`
- `com.mysql` → `com.anticheat.libs.mysql`
- `org.xerial` → `com.anticheat.libs.xerial`
- `redis.clients` → `com.anticheat.libs.redis`
- `org.apache.commons` → `com.anticheat.libs.apache.commons`
- `com.mongodb` → `com.anticheat.libs.mongodb`
- `io.javalin` → `com.anticheat.libs.javalin`
- `org.eclipse.jetty` → `com.anticheat.libs.jetty`
- `jakarta` → `com.anticheat.libs.jakarta`
- `at.favre.lib` → `com.anticheat.libs.bcrypt`
- `org.slf4j` → `com.anticheat.libs.slf4j`

排除项：`paper-api`、`spigot-api`、`bungeecord-chat`（provided）。

### 5.4 前端依赖（package.json）

| 依赖 | 版本 | 用途 |
|------|------|------|
| `vue` | ^3.5.0 | 框架 |
| `vue-router` | ^4.4.0 | 路由 |
| `pinia` | ^2.2.0 | 状态管理 |
| `axios` | ^1.7.0 | HTTP 客户端 |
| `echarts` / `vue-echarts` | ^5.5.0 / ^7.0.0 | 图表 |
| `lucide-vue-next` | ^0.400.0 | 图标库 |
| `vite` | ^5.4.0 | 构建工具 |
| `tailwindcss` | ^3.4.0 | CSS 框架 |
| `typescript` / `vue-tsc` | ^5.5.0 / ^2.0.0 | 类型检查 |

### 5.5 插件软依赖

`plugin.yml` 声明 `softdepend: [BungeeCord, Velocity]`，用于跨服通信。仅高版本启用 BungeeCord 插件消息通道。

### 5.6 模块间依赖关系（核心调用链）

```
AdvancedAntiCheat
  ├─ ConfigManager ── (config.yml)
  ├─ BanManager ──── DatabaseManager ── DatabaseRepository ── SQL/Mongo/Redis
  ├─ ReportManager
  ├─ DetectionManager (基础) ── FlyDetection/SpeedDetection/...
  ├─ AdvancedDetectionManager (高级)
  │     ├─ MovementDetectionModule ── ImpossibleActionDetector
  │     ├─ CombatDetectionModule ── AimbotHardLockDetector / ReachValidator / CPSLimiter
  │     ├─ BehaviorAnalysisEngine ── BehaviorTracker ── PlayerProfile
  │     ├─ AssociationDetector ── AltAccountDetector / TeamCheatingDetector / SocialGraph
  │     ├─ ProbabilityFusionEngine ── BayesianNetwork
  │     ├─ RCPComputer ── AdaptiveLearningSystem
  │     └─ DecisionActionCenter ── (触发 BanManager / CaptchaManager)
  ├─ CheckClientManager / CheckClientConfigManager
  ├─ CaptchaManager ── CaptchaWorld / CaptchaTask
  ├─ BountyManager ── BountySession / BountyWorld
  ├─ ProfileManager ── PlayerProfile
  └─ WebServer ── WebRouter ── Handlers ── BukkitBridge ── (主线程)
                  └─ AuthManager / AuthFilter
                  └─ WebSocketHandler / AlertBroadcaster
                  └─ AuditManager ── DatabaseManager
```

---

## 6. 项目运行方式

### 6.1 环境要求

| 项 | 要求 |
|----|------|
| JDK | 21+（GitHub Actions 强制 Java 21，由 paper-api 1.21.11 要求） |
| Maven | 3.8+ |
| Node.js | v20.11.0（由 frontend-maven-plugin 自动安装） |
| 服务端 | Paper 1.21.11+ 或 Spigot 1.8.8 / FlamePaper 1.8.8 |

### 6.2 本地构建

#### 6.2.1 完整构建（前端 + Java + Shade）

```bash
mvn clean package -Ppaper
# 或
mvn clean package -Pspigot
```

构建流程（由 [pom.xml](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/pom.xml) 的 `frontend-maven-plugin` 与 `maven-resources-plugin` 自动编排）：

1. `generate-resources` 阶段：`frontend-maven-plugin` 安装 Node.js + npm → `npm ci` → `npm run build`（编译 web-panel 至 `web-panel/dist`）。
2. `process-resources` 阶段：`maven-resources-plugin` 将 `web-panel/dist/**` 复制到 `target/classes/web/dist`，打包进 JAR。
3. `package` 阶段：`maven-shade-plugin` 重定位依赖，产出最终 fat-jar。

#### 6.2.2 产物

- `target/AdvancedAntiCheat-2.1.0.jar`：可直接放入服务端 `plugins/` 目录使用，内含前端静态资源。

#### 6.2.3 仅构建前端（开发）

```bash
cd web-panel
npm install
npm run dev      # 开发服务器
npm run build    # 产物至 web-panel/dist
```

### 6.3 部署

1. 将 JAR 放入 `plugins/` 目录。
2. 启动服务端，插件自动生成配置：
   - `plugins/AdvancedAntiCheat/config.yml` — 主配置
   - `plugins/AdvancedAntiCheat/checkclient.yml` — 查端信息配置
   - `plugins/AdvancedAntiCheat/messages.yml` — 消息配置
3. 修改配置后执行 `/ac reload` 生效。

### 6.4 Web 面板访问

`config.yml` 中 `web.enabled: true` 时，插件启动内嵌 HTTP+WS 服务器：

- 默认监听 `0.0.0.0:8080`
- 访问 `http://<server-ip>:<port>/` 打开 SPA 面板
- 默认演示账号（明文密码 = 用户名）：
  - `admin / admin` — 全权限
  - `moderator / moderator` — 玩家/案件/总览/审计/通知
  - `reviewer / reviewer` — 案件审理
  - `observer / observer` — 只读
- 生产部署：游戏内执行 `/ac genpwd <新密码>` 生成 bcrypt 哈希，替换 `config.yml` 中 `web.auth.accounts[].password-hash`，执行 `/ac reload` 生效。

### 6.5 数据库配置

`config.yml` 的 `database.type` 支持 `sqlite`（默认）/ `h2` / `mysql` / `mongodb` / `redis`。

跨服务器部署时，所有节点配置相同的 MySQL/MongoDB/Redis 后端，封禁信息自动同步，BungeeCord/Velocity 代理下的所有链接服务器拒绝被封禁玩家进入。

### 6.6 GitHub Actions CI/CD

[.github/workflows/build.yml](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/.github/workflows/build.yml)

- **触发**：push 到 `main`/`develop`/`feature/**`/`bugfix/**`/`hotfix/**`，PR 到 `main`/`develop`，或手动触发。
- **Matrix**：`paper`（Paper 1.21.11）+ `spigot`（Spigot 1.8.8）并行构建。
- **流程**：Checkout → setup JDK 21（temurin）→ 配置阿里云 Maven 镜像 → 缓存 Maven 与前端依赖 → `mvn validate` → `mvn clean package -DskipTests` → `mvn test` → 生成构建报告 → 上传 JAR 与报告（保留 14 天）。
- **Summary Job**：汇总两个 API 的测试结果，上传 `build-summary` 报告。

[.github/workflows/nightly-release.yml](file:///c:/Users/Jonson/Documents/trae_projects/AntiCheat/.github/workflows/nightly-release.yml)

- 每日北京时间 21:00（UTC 13:00）触发，仅当自上次 release 有新提交时创建 Nightly Release。
- 版本号格式 `v2.0.x-beta`（x 从 2.0.1 递增，禁止 3.0.0+）。
- JAR 命名 `AdvancedAntiCheat-v2.0.x-beta.jar`，Release Notes 含警告、日期、变更列表、构建信息。

### 6.7 关键命令速查

| 命令 | 权限 | 说明 |
|------|------|------|
| `/ac reload` | `anticheat.admin` | 重载配置（含 Web 账号热更） |
| `/ac stats` | `anticheat.admin` | 查看检测统计 |
| `/ac reports` | `anticheat.admin` | 查看待处理举报 |
| `/ac help` | `anticheat.admin` | 帮助 |
| `/ac profile <玩家>` | `anticheat.admin` | 查看玩家档案 GUI |
| `/ac genpwd <密码>` | `anticheat.admin` | 生成 bcrypt 哈希（用于 Web 账号） |
| `/ban <玩家> [时间] [原因]` | `anticheat.ban` | 封禁（默认永久） |
| `/unban <玩家>` | `anticheat.unban` | 解封 |
| `/report <玩家> <原因>` | `anticheat.report` | 举报 |
| `/goto <玩家>` | `anticheat.goto` | 传送（支持跨服） |
| `/checkclient <玩家> <QQ>` | `anticheat.checkclient` | 客户端检查 |
| `/checkdone <玩家>` | `anticheat.checkclient` | 结束检查 |
| `/captcha <玩家\|toggle\|timelimit>` | `anticheat.captcha` | 验证码 |
| `/bounty enter\|leave\|invite\|report\|lb\|start\|complete` | `anticheat.bounty` | 漏洞赏金 |

---

## 附录：项目目录树

```
AntiCheat/
├── .github/workflows/
│   ├── build.yml
│   ├── nightly-release.yml
│   └── test-dispatch.yml
├── src/main/java/com/anticheat/
│   ├── AdvancedAntiCheat.java                 # 主插件类
│   ├── bounty/                                # 漏洞赏金系统
│   ├── captcha/                               # 验证码系统
│   │   └── tasks/
│   ├── commands/                              # 命令执行器（10 个）
│   ├── compat/                                # 版本兼容层
│   ├── detection/                             # 检测系统
│   │   ├── association/                       # 关联与团队作弊
│   │   ├── combat/                            # 战斗检测
│   │   ├── core/                              # 检测抽象基座
│   │   ├── fusion/                            # 概率融合与决策
│   │   ├── inventory/                         # 物品栏检测
│   │   ├── movement/                          # 运动检测
│   │   ├── network/                           # 协议校验
│   │   └── physics/                           # 物理模拟
│   ├── gui/                                   # 游戏内 GUI
│   ├── listeners/                             # 事件监听器（11 个）
│   ├── managers/                              # 管理器
│   │   └── audit/                             # 审计数据结构
│   ├── profiles/                              # 玩家画像
│   ├── repositories/                          # 数据访问层
│   │   └── impl/                              # SQL/Mongo/Redis 实现
│   ├── utils/                                 # 工具类
│   └── web/                                   # Web 后端
│       ├── auth/                              # 认证
│       ├── dto/                               # 数据传输对象（17 个）
│       ├── handler/                           # 路由处理器（8 个）
│       ├── util/                              # Web 工具
│       └── ws/                                # WebSocket
├── src/main/resources/
│   ├── config.yml                             # 主配置
│   ├── checkclient.yml                        # 查端配置
│   └── messages.yml                           # 消息配置
├── web-panel/                                 # Vue 3 前端
│   └── src/
│       ├── api/                               # API 层（含 mock）
│       ├── components/                        # 组件（layout + common）
│       ├── router/                            # 路由
│       ├── stores/                            # Pinia stores
│       ├── styles/                            # 样式
│       ├── types/                             # 类型
│       ├── utils/                             # 工具
│       └── views/                             # 视图（13 个）
├── plugin.yml                                 # 插件元数据
├── pom.xml                                    # Maven 构建
├── spec.md / tasks.md / check_list.md         # 项目规格与任务
├── README.md / LICENSE
└── CODE_WIKI.md                               # 本文档
```

---

*文档生成时间：2026-08-27 · 基于 v2.1.0 源码*
