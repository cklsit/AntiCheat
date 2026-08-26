# 反作弊指挥中心 · 网页管理面板 需求规格文档

> 版本: v1.0 | 日期: 2026-08-25 | 状态: 待审批

---

## 1. 项目概述

### 1.1 项目背景

为AdvancedAntiCheat插件(v2.0.0, 支持Minecraft 1.8.x-1.21.x)提供一套现代化的Web管理面板，实现可视化的反作弊监控、玩家管理、案件审理、AI分析和系统配置等功能。

### 1.2 技术选型

| 层面 | 选型 | 说明 |
|------|------|------|
| 前端框架 | Vue 3 + TypeScript | 组件化开发，生态成熟 |
| 构建工具 | Vite | 快速开发构建 |
| 状态管理 | Pinia | 轻量状态管理 |
| 路由 | Vue Router 4 | 单页面路由 |
| UI样式 | Tailwind CSS + 自定义设计系统 | 遵循设计规范0.1-0.4 |
| 图标 | Lucide SVG Icons | 线性图标，统一尺寸 |
| 图表 | ECharts 5 | 数据可视化 |
| WebSocket | 原生 WebSocket API | 实时数据推送 |
| 地图渲染 | Canvas 2D | 实时地图渲染 |

后端集成方式：与Java插件通过WebSocket + REST API通信（由Java插件提供HTTP服务端点）。前端可独立部署为静态站点，也可打包入插件resources。

### 1.3 交付范围

**一期交付**：登录认证、主框架（导航/侧栏/通知/命令面板）、总览仪表盘、玩家管理、案件审理、配置中心、审计日志。共6个功能模块。

---

## 2. 全局设计系统

### 2.1 色彩体系

```css
:root {
  /* 背景 */
  --bg-base:      #0D1117;   /* 深黑蓝，主背景 */
  --bg-card:      #161B22;   /* 略浅，卡片/面板 */
  --bg-hover:     #1C2333;   /* 悬浮/弹窗 */
  --border:       #30363D;   /* 分割线、卡片描边 */
  /* 文字 */
  --text-primary: #E6EDF3;   /* 主标题、重要文字 */
  --text-secondary: #8B949E; /* 描述、标签 */
  --text-muted:   #484F58;   /* 禁用、占位 */
  /* 强调 */
  --accent-cyan:  #00E5FF;   /* 主操作、焦点、在线 */
  --accent-blue:  #388BFD;   /* 信息、链接 */
  --success:      #3FB950;   /* 通过、正常、安全 */
  --warning:      #D29922;   /* 观察中、中等风险 */
  --danger-orange:#FF6D00;   /* 高风险、告警 */
  --danger-red:   #F85149;   /* 极高风险、封禁 */
  --accent-purple:#BC8CFF;   /* AI相关、聚类分析 */
  /* 阴影 */
  --shadow-md: 0 4px 12px rgba(0,0,0,0.4);
  --shadow-lg: 0 8px 24px rgba(0,0,0,0.5);
}
```

### 2.2 字体与排版

```css
:root {
  --font-sans: 'Inter', -apple-system, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', 'Noto Sans SC', sans-serif;
  --font-mono: 'JetBrains Mono', 'SF Mono', Consolas, monospace;
  --fs-page-title: 24px;
  --fs-card-title: 14px;
  --fs-body: 13px;
  --fs-caption: 12px;
  --fs-log: 12px;
  --fw-semibold: 600;
  --fw-regular: 400;
  --lh-body: 1.6;
}
```

### 2.3 间距与圆角

```css
:root {
  --space-1: 8px;
  --space-2: 12px;
  --space-3: 16px;
  --space-4: 24px;
  --space-5: 32px;
  --radius-card: 8px;
  --radius-btn: 6px;
  --radius-tag: 4px;
  --radius-modal: 12px;
}
```

### 2.4 状态灯（在线/风险）

```css
/* 在线状态灯 - 绿色 */
.status-dot.online {
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--success);
  box-shadow: 0 0 0 3px rgba(63, 185, 80, 0.18), 0 0 12px rgba(63, 185, 80, 0.6);
}
/* 离线状态灯 - 红色 */
.status-dot.offline {
  background: var(--danger-red);
  box-shadow: 0 0 0 3px rgba(248, 81, 73, 0.18);
}
/* 连接中 - 黄色 */
.status-dot.reconnecting {
  background: var(--warning);
  animation: pulse 1.2s infinite;
}
```

---

## 3. 功能需求详细规格

### 3.1 登录与安全认证

#### 3.1.1 登录页

**页面结构**：
- 纯色深黑蓝背景 (#0D1117)，无装饰
- 居中卡片 (宽 400px, bg-card, border, radius-12px)
- 顶部：服务器Logo(可配置) + 双行标题
- 表单区域：用户名、密码(带显示切换)、TOTP二次验证码(8位输入框分2行)
- 底部：忘记密码、需要帮助链接

**安全机制**：
- 登录失败3次 → 锁定账户5分钟
- 登录失败5次 → 锁定30分钟 + 管理员通知
- 错误提示：卡片边缘红色闪烁 + "用户名或密码错误，剩余尝试次数：2"

**登录成功后**：
- 右上角显示"上次登录：2026-08-25 14:30 | IP: 192.168.1.100"

### 3.2 主面板框架

#### 3.2.1 顶部导航栏 (高度: 56px)

**从左到右元素**：
1. **Logo区**：插件图标 + 服务器名（可配置，例：MineCraft Server），点击跳转总览
2. **在线数**：●在线 42，点击跳转玩家列表，数字实时更新
3. **风险等级指示器**：
   - 圆形彩色指示器（绿/黄/橙/红）
   - 悬浮弹层显示触发因素列表（例：3个高风险玩家在线）
4. **搜索框**：宽300px → 聚焦时展开500px，占位符"搜索... Ctrl+K"，点击打开命令面板
5. **通知铃铛**：带未读数红色角标，点击下拉通知面板
6. **头像菜单**：圆形头像 + 下拉菜单（账户设置 / 切换服务器 / 退出登录）

#### 3.2.2 通知下拉面板

- 标题：通知中心，右侧"标记全部已读"、关闭按钮
- 通知列表：每条通知包含：
  - 严重程度彩色前缀（🔴🟠🔵🟢）
  - 标题文本
  - 时间 + "点击查看详情 →"
- 底部："查看全部通知"链接

#### 3.2.3 侧边栏 (宽度: 220px / 折叠56px)

**菜单项**（选中项：青3px左边框 + bg-hover背景）：
- 🖥 总览 (默认)
- 👥 玩家
- ⚖️ 案件
- 🗺 实时地图
- 🎬 回放档案
- 🧠 AI实验室
- ⚙️ 配置中心
- 📋 审计日志
- 🌐 信誉联盟

**底部状态区**：
- 连接状态：WebSocket 状态灯 + 文字 + 延迟 (12ms)
- 版本信息：插件版本 v2.4 / 面板版本 v1.7

折叠态：只显示图标，鼠标悬浮显示文字tooltip。

#### 3.2.4 全局命令面板 (Ctrl+K 或点击搜索框)

**结构**：
- 搜索输入框：`🔍 输入命令或搜索... [Esc]`
- 区块1 - 快捷操作（输入为空时显示）：
  - ▸ ban <玩家名> <时长> <原因>
  - ▸ profile <玩家名>
  - ▸ watch <玩家名>
  - ▸ phantom <玩家名>
  - ▸ scan history <天数>
  - ▸ broadcast <消息>
- 区块2 - 实时匹配结果：
  - 🎯 玩家: Steve (在线, 风险72) 右侧[跳转] [封禁]
  - 📋 命令: ban — 封禁指定玩家
  - ⚙️ 配置: 移动检测模块

**交互**：上下键选择，Enter 跳转/执行，Esc 关闭。

---

### 3.3 功能页面

#### 3.3.1 总览仪表盘 (Dashboard)

**顶部工具栏**：
- 页面标题：总览
- 右侧：时间范围下拉 [今日▾] + 刷新按钮 + 自定义按钮

**统计卡片行 (5列)**：

| 卡片 | 主数据 | 辅助信息 |
|------|--------|----------|
| 在线玩家 | 42 | ↗ 5.2% (趋势) |
| 嫌疑玩家 | 7 | ⚠ 3高危 (标签) |
| 今日拦截 | 1,204 | ↘ 12% (趋势) |
| 待审案件 | 3 | 🔴 2紧急 (标签) |
| 审判通过率 | 87% | 环形进度图 |

**图表区域 (2列)**：
- 左：风险趋势 (24h折线图，青线=嫌疑数，红线=高危数)
- 右：检测模块触发排行 (今日，柱状图，横向)
  - 移动检测 45% | 战斗检测 28% | 挖掘检测 15% | 其他 12%

**实时告警流 (底部，1列)**：
- 类日志列表，按时间倒序，最新高亮
- 每行：时间戳 + 风险色条 + 玩家 + 模块 + 评分 + 快捷操作按钮

#### 3.3.2 玩家管理

**筛选工具栏**：
- 搜索玩家名
- 状态筛选：全部/在线/离线/封禁/观察中
- 风险筛选：全部/低/中/高/极高
- 排序：按风险分/最近活跃/在线时长

**玩家列表 (表格)**：
- 表头：复选框 / 头像 / 玩家名 / 状态灯 / 风险分(带色条) / 最近触发 / 在线时长 / 操作
- 行悬浮：高亮 + 操作按钮展开(查看档案/立即观察/临时封禁)

**玩家详情页(侧滑抽屉或新页)**：
- 个人信息区：头像、玩家名、UUID、IP、首玩/最后上线时间
- 风险指标卡片：当前风险分、历史最高、累计违规次数、封禁次数
- 违规历史时间线：时间 + 模块 + 评分 + 处理结果
- 关联账号区：可能小号列表(相似IP/设备指纹)

#### 3.3.3 案件审理

**案件队列 (3列看板)**：
- 待审 / 审理中 / 已完成
- 每张案件卡片：
  - ID: CASE-042
  - 玩家: Steve
  - 最高嫌疑模块: 预测式移动异常 (95%)
  - 证据数量: 12条
  - 收到时间: 2分钟前
  - 快捷按钮: [立即审理]

**案件审理页**：
- 玩家基础信息 + 风险摘要
- 证据列表 (可筛选模块/时间/严重度)
  - 每条证据：时间、模块、评分、详细描述、数据片段(JSON展开)
- 审判操作区：
  - 投票/判定：无罪 / 观察 / 临时封禁 / 永久封禁
  - 备注输入框
  - [提交判决] 按钮

#### 3.3.4 实时地图

- Canvas渲染游戏世界俯视图
- 玩家：小圆点(色=风险等级) + 悬浮显示玩家名+风险分
- 热点：半透明热力图覆盖(红=频繁违规区)
- 工具：缩放/平移、世界切换、时间轴回放、图层开关(玩家/方块/热力图)

#### 3.3.5 回放档案

- 列表：按时间倒序，筛选玩家/日期/严重度
- 每行：回放ID、玩家、时长、触发模块、操作(查看/下载/分享)
- 回放查看器：时间轴 + 多视角切换 + 关键帧标记 + 倍速播放

#### 3.3.6 AI实验室

- 规则提案列表：AI自动生成的新规则建议
  - 提案ID、建议内容、历史数据准确率、置信度、[采纳] [忽略] [修改]
- 聚类分析：玩家行为聚类可视化散点图
- 模型性能：精确率/召回率/F1曲线、每日误报数趋势

#### 3.3.7 配置中心

**左侧目录树 + 右侧表单**：
- 常规设置 (服务器名、面板标题、主题)
- 检测模块 (按模块分组开关/阈值调整)
  - 移动检测 | KillAura | 速度 | 飞行 | 挖掘 | 脚手架...
  - 每项：启用开关 + 触发阈值 + 严重度映射 + 测试按钮
- 处罚规则 (计分规则、自动封禁阈值)
- 通知设置 (邮件、Web Push、Discord Webhook)
- 数据库/缓存 (连接池、TTL)

#### 3.3.8 审计日志

- 筛选：操作类型、操作者、时间范围、结果(成功/失败)
- 表格：时间、操作者、操作类型、目标(玩家/配置)、IP、结果、详情展开
- 详情：请求参数快照、响应结果

#### 3.3.9 信誉联盟

- 联盟概览：成员服务器数、共享封禁数、本周可疑IP
- 共享黑名单：IP/玩家、来源服务器、证据数量、[接入我的黑名单]
- 联盟统计：折线图(跨服作弊趋势)、柱状图(成员贡献排行)

---

## 4. 数据与通信规范

### 4.1 WebSocket实时数据

```typescript
// 连接端点: ws://server:port/ws/anticheat
interface WsMessage<T> {
  type: 'player_update' | 'alert' | 'case_new' | 'stats_ping';
  payload: T;
  timestamp: number;
}
// 重连策略: 指数退避 1s/2s/4s/8s 最大30s
```

### 4.2 REST API 约定

```typescript
// 统一响应
interface ApiResp<T> {
  code: 0 | 400 | 401 | 403 | 500;
  message: string;
  data: T;
  requestId: string;
}
// 分页
interface Page<T> {
  list: T[];
  total: number;
  page: number;
  pageSize: number;
}
```

### 4.3 Mock数据(一期独立运行)

因Java后端接口开发与前端开发并行，前端一期全部使用Mock数据接口(本地JSON + setTimeout模拟延迟)，确保UI/交互完整演示。

---

## 5. 目录结构

```
web-panel/
├── index.html
├── vite.config.ts
├── package.json
├── tsconfig.json
├── tailwind.config.js           # 包含设计系统色值映射
├── postcss.config.js
└── src/
    ├── main.ts
    ├── App.vue
    ├── router/index.ts
    ├── stores/                  # Pinia stores
    │   ├── auth.ts
    │   ├── notification.ts
    │   └── global.ts
    ├── styles/
    │   ├── global.css           # 设计系统CSS变量
    │   └── components.css
    ├── components/
    │   ├── layout/
    │   │   ├── TopNav.vue
    │   │   ├── Sidebar.vue
    │   │   ├── NotificationPanel.vue
    │   │   ├── CommandPalette.vue
    │   │   └── Shell.vue
    │   ├── common/
    │   │   ├── StatusDot.vue
    │   │   ├── RiskBadge.vue
    │   │   ├── StatCard.vue
    │   │   ├── DataTable.vue
    │   │   └── Modal.vue
    │   └── charts/
    │       ├── LineChart.vue
    │       ├── BarChart.vue
    │       └── RingProgress.vue
    ├── views/
    │   ├── LoginView.vue
    │   ├── DashboardView.vue
    │   ├── PlayersView.vue
    │   ├── PlayerDetailView.vue
    │   ├── CasesView.vue
    │   ├── CaseReviewView.vue
    │   ├── LiveMapView.vue
    │   ├── ReplayView.vue
    │   ├── AILabView.vue
    │   ├── ConfigView.vue
    │   ├── AuditLogView.vue
    │   └── AllianceView.vue
    ├── api/
    │   ├── request.ts           # axios封装 + 拦截器
    │   ├── auth.ts
    │   ├── players.ts
    │   ├── cases.ts
    │   └── mock/                # 一期Mock数据
    │       ├── players.json
    │       └── cases.json
    ├── utils/
    │   ├── format.ts
    │   ├── risk.ts              # 风险色映射/等级计算
    │   └── ws.ts
    └── types/
        └── index.ts
```

---

## 6. 非功能性需求

| 项目 | 指标 |
|------|------|
| 首屏加载 | < 2s (3G) |
| 路由切换 | < 200ms |
| 仪表盘实时刷新延迟 | < 1s (WebSocket) |
| 命令面板搜索响应 | < 100ms |
| 浏览器兼容 | Chrome 110+, Edge 110+, Firefox 110+, Safari 16+ |
| 可访问性 | 键盘可操作、对比度WCAG AA级 |
| 响应式 | 最小支持宽度 1280px，低于时显示"请使用更宽的显示器"提示 |
| 权限控制 | 基于角色：超级管理员 / 管理员 / 审理员 / 观察员 |

---

## 7. 验收标准(摘要)

1. 所有色彩、字体、间距、圆角严格匹配设计系统
2. 登录流程包含锁定机制与错误提示
3. 顶部导航/侧边栏/通知/命令面板四项基础框架完整可用
4. 9个功能页面视觉稿完整，填充Mock数据
5. 风险等级配色、状态灯、图表颜色与设计规范一致
6. 侧边栏折叠态正常，命令面板 Ctrl+K 可打开
7. 代码符合TypeScript类型约束，无tsc错误
8. 构建产物无编译错误，`npm run build` 通过
