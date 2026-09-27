# LinClient

> 一个开源、实用、**非外挂** 的 Minecraft Java 版客户端 Mod。
> 仅做客户端 HUD 信息显示、性能与视觉优化、配置，**绝不**自动战斗 / 透视 / 飞行 / 加速 / 反击退 / 实体雷达 / 绕过反作弊 / 读取服务端隐藏信息 / 任何服务端作弊。

- **加载器**：Forge
- **MC 版本**：**1.8.9** 与 **1.12.2**（各自独立的子工程，产出各自的 jar）
- **Java**：8（构建）/ 各子工程自带 Gradle Wrapper
- **构建工具**：ForgeGradle 2.x（1.12.2 用 Gradle 4.9，1.8.9 用 Gradle 2.14.1）
- **映射**：`stable_39`（1.12.2）/ `stable_22`（1.8.9）
- **协议**：MIT

---

## 合规声明（Compliance）

本项目严格遵守「客户端信息显示 / 性能优化 / 配置」的定位：

- 所有数据均来自**客户端已同步的公开信息**（自身状态、可见实体的同步属性、环境、物品等）。
- 不修改任何服务端行为；所有视觉/性能开关（全亮、云、火焰/水覆盖、动态帧率等）仅作用于本地渲染。
- 战斗辅助**仅显示信息**（目标血量、造成伤害/受到伤害数字、连击），不含任何自动操作。
- 不读取服务端未下发的信息，不含反作弊绕过逻辑。

---

## 功能清单

### HUD 信息显示（7 个模块，可在游戏内 ClickGUI 中单独开关）
| 模块 | 内容 |
| --- | --- |
| 自身状态 (Self) | 血量 / 吸收 / 饱食 / 氧气 / 坐标 / 速度 / 移动距离 / 游戏模式 |
| 实体信息 (Entity) | 附近 玩家 / 怪物 / 动物 / 掉落物 数量统计 |
| 输入监控 (Input) | W/A/S/D 键盘图 + 左键(LMB)/右键(RMB)/空格(Space) 按键高亮；左/右 CPS 实时读数 |
| 环境信息 (Environment) | 坐标 / 朝向 / 群系 / 光照 / 游戏内时间 / FPS / 延迟 / 内存 / 实体数 / 区块数 |
| 物品信息 (Item) | 手持物品名称 / 耐久；背包物品概览 |
| 其他 (Other) | 维度 / 朝向 / 难度 / 速度 / 移动距离 / 经验 / 光照 |
| 战斗辅助（仅信息） | 最近攻击目标 血量/距离；造成伤害 / 受到伤害；连击计数 |

> FPS 直接读取与 F3 调试屏相同的 `debugFPS` 来源（反射获取，失败时回退滑动窗口），数值与游戏一致。

### 性能与视觉优化（均为客户端本地设置）
- **视觉覆盖开关**：全亮 (Fullbright) / 隐藏云 / 隐藏火焰覆盖 / 隐藏水覆盖
- **动态 FPS**：低帧时自动降低渲染距离，恢复后自动还原（纯客户端设置，不影响服务端）

### 原版 HUD 元素显隐
在 ClickGUI 的 `vanilla` 分类中可单独隐藏以下原版界面元素（纯本地覆盖渲染，不影响服务端）：血量 / 护甲 / 饱食 / 氧气 / 快捷栏 / 经验条 / 准星 / Boss 血条 / 药水图标 / 暗角 / 传送门 / 头盔 / 跳跃条。
> 注：1.8.9 的 Forge 未提供 药水图标 / 暗角 对应的覆盖事件，这两项仅在 1.12.2 生效；其余 11 项两版本一致。

> 说明：早期计划中的「隐藏天气」开关已移除——1.8.9 / 1.12.2 没有可靠的**纯客户端**天气抑制 API，强行实现要么需要 coremod（有作弊嫌疑），要么行为不可靠，因此舍去以守住「非外挂、可干净构建」的底线。

### 界面与配置
- **ClickGUI（游戏内配置）**：游戏中按 **右 Shift（Right Shift）** 打开。左侧分类 `modules` / `vanilla` / `settings`，右侧逐项开关（原版开关为两列布局）；并提供「语言切换」「HUD 布局」「关闭」按钮。
- **主页面（Home）**：按 **F8** 或从标题界面（主菜单）的 `LinClient` 按钮打开。深蓝科技风背景，面板内含品牌标题、实时 FPS、两列模块开关卡片（ClickGUI 同款样式）与「打开设置 / HUD 布局 / 语言 / 关闭」四个面板风按钮（悬停高亮）。
- **HUD 布局编辑器（HudEditorScreen）**：从 ClickGUI 或主页面点「HUD 布局」进入**专用编辑界面**——所有模块（含已禁用的半透明显示）直接呈现在屏幕上，按住左键拖动即可移动，拖动中的模块有高亮描边；**ESC 保存位置并退出**，全程无需离开界面。
- **统一 UI 逻辑**：任何界面按 `ESC` 即保存并回到游戏；界面间跳转一律整体切换，不会叠加；右 Shift 随时打开 ClickGUI。
- **暂停 / 设置菜单入口**：游戏内暂停菜单（Esc）与设置页（Options）底部新增 `LinClient` 按钮，一键进入 ClickGUI。
- **自动疾跑（Auto Sprint）**：在 `settings` 分类或主页面开启后，向前移动时自动疾跑（复用原版疾跑键状态，非加速外挂）。
- **运行时中英切换**：ClickGUI / 主页面内一键切换 `zh_CN` / `en_US`，即时生效，无需重启游戏。
- **Forge 原生配置**：各版本在 `config/linclient.cfg` 落盘（基于 `net.minecraftforge.common.config.Configuration`），可手动编辑。

---

## 文件树

```
Minecraft-LinClient/
├── 1.12.2/                         # Forge 1.12.2 子工程（Gradle 4.9 + ForgeGradle 2.3）
│   ├── build.gradle
│   ├── gradle.properties
│   ├── gradlew / gradlew.bat
│   ├── gradle/wrapper/
│   ├── .gitignore
│   └── src/main/
│       ├── java/com/lindedaoqwq/linclient/
│       │   ├── LinClient.java                 # @Mod 入口
│       │   ├── config/
│       │   │   ├── ModConfig.java             # Forge 原生配置（全局/性能/视觉/模块开关/位置/语言）
│       │   │   └── KeyBindings.java           # 快捷键（右Shift 开 ClickGUI / F8 开主页面 / H 切 HUD）
│       │   ├── gui/
│       │   │   ├── ClickGuiScreen.java        # 游戏内 ClickGUI 配置界面
│       │   │   ├── HudEditorScreen.java       # HUD 布局编辑器（拖动/ESC 保存）
│       │   │   └── HomeScreen.java            # 独立主页面（蓝色主题）
│       │   ├── hud/
│       │   │   ├── HudModule.java             # 模块基类（渲染/布局/拖动）
│       │   │   ├── HudOverlay.java            # 模块调度与渲染 + 命中测试
│       │   │   └── modules/                   # 7 个信息模块
│       │   │       ├── SelfStatusModule.java
│       │   │       ├── EntityModule.java
│       │   │       ├── InputModule.java
│       │   │       ├── EnvironmentModule.java
│       │   │       ├── ItemModule.java
│       │   │       ├── OtherModule.java
│       │   │       └── CombatModule.java
│       │   ├── event/ClientEvents.java        # 渲染/刻度/输入/菜单注入/屏幕特效 + 视觉与性能/自动疾跑
│       │   ├── state/ClientState.java         # 共享运行时状态（CPS/速度/距离/连击/死亡坐标/FPS）
│       │   └── util/                          # RenderUtils / I18n / Format / Rect
│       └── resources/
│           ├── mcmod.info
│           └── assets/linclient/lang/{en_US,zh_CN}.lang
├── 1.8.9/                          # Forge 1.8.9 子工程（Gradle 2.14.1 + ForgeGradle 2.1），结构同上
├── .github/workflows/build.yml     # CI：双版本构建 + ci-logs 镜像 + tag 发布 Release
├── .gitignore
├── LICENSE
└── README.md
```

> 两个子工程结构一致，仅 Minecraft/ForgeGradle/mappings 版本与少量 API 适配不同（`thePlayer`/`theWorld`/`fontRendererObj` 等）。

---

## 构建

### 本地构建（任选一个版本）
需要 **JDK 8**。进入对应子工程目录，使用其自带的 Gradle Wrapper：

```bash
# 构建 1.12.2
cd 1.12.2
./gradlew build          # Windows: gradlew.bat build

# 构建 1.8.9
cd 1.8.9
./gradlew build
```

构建产物分别在：
- `1.12.2/build/libs/LinClient-1.12.2-1.0.0.jar`
- `1.8.9/build/libs/LinClient-1.8.9-1.0.0.jar`

### 放入游戏
将对应版本的 jar 放进 `.minecraft/mods/` 即可（需安装对应版本的 Forge）。

---

## 快捷键（游戏内「控制」中可重新绑定）

| 按键（默认） | 功能 |
| --- | --- |
| **右 Shift（Right Shift）** | 打开 / 关闭 游戏内 ClickGUI（或退出 HUD 拖动编辑） |
| **F8** | 打开独立主页面（Home） |
| `H` | 切换整个 HUD 的显示 |
| （自定义） | 启用 / 停用 LinClient 总开关 |

### ClickGUI 操作
打开后（左侧分类，右侧开关/按钮）：
- `modules`：逐项开关 7 个 HUD 模块（`linclient.module.*`）。
- `vanilla`：两列排布，逐项隐藏原版 HUD 元素（血量 / 护甲 / 饱食 / 氧气 / 快捷栏 / 经验 / 准星 / Boss / 药水 / 暗角 / 传送门 / 头盔 / 跳跃条）。
- `settings`：自动疾跑等开关，以及「语言切换 / HUD 布局 / 关闭」按钮。
- 点「HUD 布局」打开专用编辑器：拖动模块，`ESC` 保存退出。
- 所有改动实时生效并写入 `config/linclient.cfg`。

---

## 配置说明

1. **所有开关**：游戏内按 **右 Shift** 打开 ClickGUI，或按 **F8** 打开主页面，或用任意文本编辑器编辑 `config/linclient.cfg`。
2. **模块显隐**：在 ClickGUI 的 `modules` 分类逐项开关 7 个 HUD 模块（`linclient.module.*`）。
3. **语言**：在 ClickGUI / 主页面内一键切换，运行时即时生效（en_US / zh_CN），并持久化到 `config/linclient.cfg`。

---

## GitHub 自动化

仓库包含 `.github/workflows/build.yml`：
- `push` / `pull_request` 到 `main` 自动对 **1.12.2** 与 **1.8.9** 分别执行构建，并在 Job Summary 中给出报告。
- 无论成功失败，都会把**完整构建日志**上传为 artifact，并镜像到 `ci-logs` 分支（plain git 可取）。
- 推送 **tag**（`v1.0.0` 等形式）自动构建两个版本并创建 **Release**，附上两个 jar。
- 已声明 `permissions: contents: write` 以允许发布 Release / 推送日志分支。

> 注意：**main 分支的普通 push 只构建 + 出日志，不会自动发 Release**。需发布时再打 `v*` tag。

Release 最新版下载地址：

```
https://github.com/lindedaoqwq/Minecraft-LinClient/releases/latest
```

---

## 许可证

[MIT](LICENSE) © lindedaoqwq
