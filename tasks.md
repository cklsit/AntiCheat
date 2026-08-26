# 反作弊网页面板 · 实施任务分解清单

> 关联文档: spec.md | 总工期估算: 约 5-7 人日 | 任务总数: 7 大阶段 / 28 子任务

---

## 阶段 1 · 项目脚手架 (T01)

| ID | 任务 | 产出文件 | 依赖 | 预计耗时 |
|----|------|----------|------|----------|
| T01-01 | 创建 web-panel 工程目录 | `web-panel/` | - | 10min |
| T01-02 | 初始化 Vite + Vue3 + TS 工程 | `package.json`, `vite.config.ts`, `tsconfig.json` | T01-01 | 20min |
| T01-03 | 安装依赖(Vue Router/Pinia/Tailwind/ECharts/Lucide/Axios) | `node_modules/` | T01-02 | 10min |
| T01-04 | 配置 Tailwind 设计系统 (colors/fonts/spacing/radius) | `tailwind.config.js`, `postcss.config.js` | T01-03 | 30min |
| T01-05 | 全局CSS变量、基础reset、滚动条、状态灯、风险色工具类 | `src/styles/global.css`, `components.css` | T01-04 | 40min |
| T01-06 | 配置路由、Pinia、根组件App.vue、Shell壳 | `src/router/index.ts`, `src/stores/`, `src/App.vue` | T01-03 | 30min |
| T01-07 | 封装 Axios 请求 (统一响应、错误处理) + Mock 延迟模拟 | `src/api/request.ts` + `src/utils/mock.ts` | T01-03 | 30min |
| T01-08 | 公共类型定义 (玩家/案件/风险等级/通知等) | `src/types/index.ts` | T01-03 | 40min |

---

## 阶段 2 · 通用组件库 (T02)

| ID | 任务 | 产出文件 | 依赖 | 预计耗时 |
|----|------|----------|------|----------|
| T02-01 | 状态灯组件 (绿/黄/红 + 光晕动画) | `components/common/StatusDot.vue` | T01 | 20min |
| T02-02 | 风险徽章组件 (根据分值映射颜色 0-100) | `components/common/RiskBadge.vue` + `utils/risk.ts` | T01 | 30min |
| T02-03 | 统计卡片组件 (标题/大数字/趋势/附属标签/环形图模式) | `components/common/StatCard.vue` | T02-02 | 40min |
| T02-04 | 通用 DataTable (可筛选+排序+分页+行悬浮操作) | `components/common/DataTable.vue` | T01 | 60min |
| T02-05 | Modal 弹窗 & 抽屉 Drawer | `components/common/Modal.vue`, `Drawer.vue` | T01 | 40min |
| T02-06 | 基础图表组件封装 (LineChart/BarChart/RingProgress) | `components/charts/*.vue` | T01 | 60min |
| T02-07 | 风险等级圆形指示器组件 (用于顶部导航) | `components/common/RiskIndicator.vue` | T02-02 | 30min |

---

## 阶段 3 · 登录模块 (T03)

| ID | 任务 | 产出文件 | 依赖 | 预计耗时 |
|----|------|----------|------|----------|
| T03-01 | 登录页UI (Logo/标题/用户名/密码/TOTP 8格输入框/登录按钮) | `views/LoginView.vue` | T02 | 90min |
| T03-02 | 密码显示切换 + TOTP输入框自动跳焦 | `views/LoginView.vue` | T03-01 | 30min |
| T03-03 | 登录失败: 红色闪烁边框 + 剩余尝试次数提示 | `views/LoginView.vue` | T03-01 | 20min |
| T03-04 | 锁定逻辑 (3次5min/5次30min) + Auth Store | `stores/auth.ts` | T03-01 | 40min |

---

## 阶段 4 · 主框架 (登录后外壳) (T04)

| ID | 任务 | 产出文件 | 依赖 | 预计耗时 |
|----|------|----------|------|----------|
| T04-01 | 顶部导航栏 (Logo/在线数/风险指示器/搜索框/通知铃铛/头像菜单) | `components/layout/TopNav.vue` | T02-01, T02-07 | 120min |
| T04-02 | 侧边栏 (9个菜单项 + 折叠态 + 底部状态区 + 悬浮tooltip) | `components/layout/Sidebar.vue` | T02-01 | 90min |
| T04-03 | 通知下拉面板 (4等级颜色分类 + 点击跳转 + 标记全部已读) | `components/layout/NotificationPanel.vue` + `stores/notification.ts` | T02 | 90min |
| T04-04 | 全局命令面板 (Ctrl+K、快捷操作列表、实时匹配玩家/命令/配置) | `components/layout/CommandPalette.vue` | T02 | 120min |
| T04-05 | Shell壳组件整合 (路由出口 + 未登录重定向) | `components/layout/Shell.vue` | T04-01~04, T03 | 30min |
| T04-06 | WebSocket连接封装 (重连退避 + 状态灯联动) | `utils/ws.ts` | T01 | 60min |

---

## 阶段 5 · 功能页面 (T05) - 一期交付6个

| ID | 任务 | 产出文件 | 依赖 | 预计耗时 |
|----|------|----------|------|----------|
| T05-01 | **总览仪表盘** : 5个统计卡 + 折线图/柱图 + 实时告警流 | `views/DashboardView.vue` | T02-03, T02-06 | 150min |
| T05-02 | **玩家列表** : 筛选工具栏 + DataTable + 悬浮操作 + Mock数据 | `views/PlayersView.vue` | T02-04 | 90min |
| T05-03 | **玩家详情** : 侧滑抽屉 + 信息卡 + 风险指标 + 违规时间线 + 关联账号 | `views/PlayerDetailView.vue` | T02-05, T05-02 | 150min |
| T05-04 | **案件看板** : 待审/审理中/已完成 三列拖拽看板 | `views/CasesView.vue` | T02 | 150min |
| T05-05 | **案件审理页** : 证据列表(可展开JSON详情) + 判决操作 | `views/CaseReviewView.vue` | T02-05, T05-04 | 120min |
| T05-06 | **配置中心** : 左侧目录 + 右侧表单(检测模块分组开关/阈值) | `views/ConfigView.vue` | T02 | 180min |
| T05-07 | **审计日志** : 筛选器 + DataTable + 详情展开 | `views/AuditLogView.vue` | T02-04 | 90min |

*注：实时地图/回放档案/AI实验室/信誉联盟 因依赖较重，可延至二期实现，本期占位页即可*

---

## 阶段 6 · 辅助页面 (占位) 与 Mock 数据完善 (T06)

| ID | 任务 | 产出文件 | 依赖 | 预计耗时 |
|----|------|----------|------|----------|
| T06-01 | 实时地图 / 回放档案 / AI实验室 / 信誉联盟 占位页 (Coming Soon 风格 + 设计描述) | `views/LiveMapView.vue` 等4个 | T04 | 60min |
| T06-02 | 填充各模块Mock数据 JSON (玩家30条/案件20条/通知10条/审计30条/告警流) | `api/mock/*.json` | T05 | 90min |

---

## 阶段 7 · 集成测试 & 构建验证 (T07)

| ID | 任务 | 说明 | 依赖 | 预计耗时 |
|----|------|------|------|----------|
| T07-01 | 路由全走查 (登录→总览→9个子页→退出) | 确保每个页面可进入不崩溃 | T05, T06 | 20min |
| T07-02 | 交互测试 (通知面板/命令面板Ctrl+K/侧栏折叠/筛选) | 关键快捷键/悬浮行为 | T04 | 20min |
| T07-03 | TypeScript 类型检查 (`vue-tsc --noEmit`) | 零错误 | 全部 | 10min |
| T07-04 | 生产构建 (`npm run build`) | 产物生成、零错误 | 全部 | 10min |
| T07-05 | 视觉走查 | 对照设计系统检查色彩/间距/圆角一致性 | 全部 | 30min |

---

## 建议执行顺序 (串行/并行)

1. **T01 脚手架** — 串行完成 (前置)
2. **T02 通用组件** — 串行，可与T03并行做前半
3. **T03 登录** — 与T02后半并行
4. **T04 主框架** — 依赖T02和T03，必须完成后再进入页面开发
5. **T05 功能页面** — 6个页面可分2-3个批次并行 (T05-01/T05-02/T05-06 第一批次；T05-03/T05-07 第二批次；T05-04/T05-05 第三批次)
6. **T06 占位页 + Mock填充** — 穿插在T05后
7. **T07 验收测试** — 最终，发现小问题则返回修复

---

## 风险与注意事项

- **ECharts 按需引入**：避免全量引入导致包体积过大
- **Tailwind JIT**：确保配置 content 路径覆盖所有 vue/ts 文件
- **Mock数据真实性**：玩家名使用常见Minecraft名字，风险分合理分布
- **设计一致性**：所有自定义颜色/圆角/间距统一走设计系统CSS变量，避免散落在文件中的硬编码
- **路由守卫**：所有业务路由前检查Auth Store是否已登录
- **权限细化**：按钮级权限(审理/封禁)在一期可先做UI显示/禁用，后端校验后置
