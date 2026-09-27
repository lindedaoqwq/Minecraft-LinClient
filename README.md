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
- Boss 检测基于**世界中已加载的 Boss 实体**（直接判定 `EntityDragon` / `EntityWither` 类型并读取其显示名），不依赖任何服务端隐藏协议。

---

## 功能清单

### HUD 信息显示（8 个模块，可在游戏内配置菜单中单独开关）
| 模块 | 内容 |
| --- | --- |
| 自身状态 (Self) | 血量 / 吸收 / 护甲 / 饱食 / 氧气 / 经验 / 游戏模式 |
| 实体信息 (Entity) | 附近 玩家 / 怪物 / 动物 / 掉落物 数量统计 |
| 输入监控 (Input) | 移动/潜行/疾跑 实时状态；左/右键 CPS 与按键状态 |
| 环境信息 (Environment) | 坐标 / 朝向 / 群系 / 光照 / 游戏内时间 / 真实时间 / FPS / 延迟 / 内存 / 实体数 / 区块数 |
| 物品信息 (Item) | 手持物品名称 / 耐久；背包物品概览 |
| 其他 (Other) | 维度 / 朝向 / 难度 / 速度 / 移动距离 / AFK 计时 / 死亡坐标记录 |
| 战斗辅助（仅信息） | 最近攻击目标 血量/距离；造成伤害 / 受到伤害；连击计数 |
| Boss 增强 | 列出当前世界中已加载的 Boss 实体 |

### 性能与视觉优化（均为客户端本地设置）
- **视觉覆盖开关**：全亮 (Fullbright) / 隐藏云 / 隐藏火焰覆盖 / 隐藏水覆盖
- **动态 FPS**：低帧时自动降低渲染距离，恢复后自动还原（纯客户端设置，不影响服务端）

> 说明：早期计划中的「隐藏天气」开关已移除——1.8.9 / 1.12.2 没有可靠的**纯客户端**天气抑制 API，强行实现要么需要 coremod（有作弊嫌疑），要么行为不可靠，因此舍去以守住「非外挂、可干净构建」的底线。

### 界面与配置
- **游戏内配置菜单**：游戏中按 **右 Shift（Right Shift）** 打开，集中管理所有总开关与 8 个 HUD 模块的显隐，ESC 关闭并自动保存。
- **Forge 原生配置**：各版本在 `config/linclient.cfg` 落盘（基于 `net.minecraftforge.common.config.Configuration`），可手动编辑。
- **中英双语 i18n**：`en_US` / `zh_CN`，跟随游戏语言设置。

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
│       │   │   ├── ModConfig.java             # Forge 原生配置（全局/性能/视觉/模块开关）
│       │   │   └── KeyBindings.java           # 快捷键（右Shift 开菜单 / H 切 HUD）
│       │   ├── hud/
│       │   │   ├── HudModule.java             # 模块基类（渲染/布局）
│       │   │   ├── HudOverlay.java            # 模块调度与渲染
│       │   │   ├── ConfigMenuScreen.java      # 游戏内配置菜单
│       │   │   └── modules/                   # 8 个信息模块
│       │   │       ├── SelfStatusModule.java
│       │   │       ├── EntityModule.java
│       │   │       ├── InputModule.java
│       │   │       ├── EnvironmentModule.java
│       │   │       ├── ItemModule.java
│       │   │       ├── OtherModule.java
│       │   │       ├── CombatModule.java
│       │   │       └── BossModule.java
│       │   ├── event/ClientEvents.java        # 渲染/刻度/输入/屏幕特效事件 + 视觉与性能开关
│       │   ├── state/ClientState.java         # 共享运行时状态（CPS/速度/距离/AFK/连击/死亡坐标）
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
| **右 Shift（Right Shift）** | 打开 / 关闭 游戏内配置菜单 |
| `H` | 切换整个 HUD 的显示 |
| （自定义） | 启用 / 停用 LinClient 总开关 |

### 配置菜单操作
打开后：
- 点击按钮切换对应总开关 / HUD 模块的显隐
- 按钮文字实时反映当前开关状态（ON / OFF）
- `ESC` 关闭菜单并自动保存配置（写入 `config/linclient.cfg`）

---

## 配置说明

1. **所有开关**：游戏内按 **右 Shift** 打开配置菜单，或用任意文本编辑器编辑 `config/linclient.cfg`。
2. **模块显隐**：在配置菜单中逐项开关 8 个 HUD 模块（`linclient.module.*`）。
3. **语言**：跟随游戏语言设置（en_US / zh_CN）。

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
