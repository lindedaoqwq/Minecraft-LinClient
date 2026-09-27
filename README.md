# LinClient

> 一个开源、实用、**非外挂** 的 Minecraft Java 版客户端 Mod。
> 仅做客户端 HUD 信息显示、性能与视觉优化、配置，**绝不**自动战斗 / 透视 / 飞行 / 加速 / 反击退 / 实体雷达 / 绕过反作弊 / 读取服务端隐藏信息 / 任何服务端作弊。

- **加载器**：Forge
- **MC 版本**：1.20.1
- **Java**：17（构建）/ Gradle 8.5
- **映射**：官方 (official) 映射
- **协议**：MIT

---

## 合规声明（Compliance）

本项目严格遵守「客户端信息显示 / 性能优化 / 配置」的定位：

- 所有数据均来自**客户端已同步的公开信息**（自身状态、可见实体的同步属性、环境、物品等）。
- 不修改任何服务端行为；所有视觉/性能开关（全亮、隐藏天气、云、火焰/水/岩浆覆盖、动态帧率等）仅作用于本地渲染。
- 战斗辅助**仅显示信息**（目标血量、造成伤害/受到伤害数字、连击），不含任何自动操作。
- 不读取服务端未下发的信息，不含反作弊绕过逻辑。

---

## 功能清单

### HUD 信息显示（可拖拽 / 缩放 / 透明度 / 颜色 / 排序 / 显隐）
| 模块 | 内容 |
| --- | --- |
| 自身状态 | 血量 / 吸收 / 护甲 / 饱食 / 氧气 / 经验 / 药水效果与剩余时间 |
| 实体信息 | 可见玩家的 血量/护甲/手持物品/药水；最近生物的 血量；附近生物数量 |
| 输入监控 | WASD / 空格 / Shift / Ctrl 实时状态；左/右键 CPS；鼠标按键状态 |
| 环境信息 | 坐标 / 朝向 / 群系 / 光照 / 游戏内时间 / 真实时间 / FPS / 延迟 / 内存 / 实体数 / 区块数 |
| 物品信息 | 盔甲耐久 / 手持工具耐久 / 箭矢数量 / 弓弩拉弓进度 / 背包预览 / 附近掉落物 |
| 其他 | 速度计 / 移动距离 / AFK 计时 / 死亡坐标记录 / 服务器 IP |
| 战斗辅助（仅信息） | 当前攻击目标 血量/护甲/距离；造成伤害 / 受到伤害 数字；连击计数 |
| Boss 增强 | 列出当前 Boss 及其血量百分比 |

### 性能与视觉优化（均为客户端本地设置）
- **视觉覆盖开关**：全亮 (Fullbright) / 隐藏天气 / 隐藏云 / 隐藏火焰覆盖 / 隐藏水覆盖 / 隐藏岩浆覆盖
- **动态 FPS**：低帧时自动降低渲染距离，恢复后自动还原（纯客户端设置，不影响服务端）
- **保留接口（见下方说明）**：实体剔除 / 方块实体裁剪 / 粒子限制器 / LOD 渲染距离 / 区块更新节流 / 纹理动画限制 / 传送门覆盖 —— 这些需要配套的 coremod/mixin 才能物理改动渲染管线，已在配置中预留开关与集成点

### 界面与配置
- **HUD 编辑器**：游戏中按快捷键打开，拖拽定位、滚轮缩放、快捷键调透明度/显隐/重置/微调（详见下方快捷键）
- **Forge 原生配置**：`config/linclient-client.toml`，可用 [Configured](https://www.curseforge.com/minecraft/mc-mods/configured) 等 Mod 在游戏内打开图形界面，或直接编辑 TOML
- **布局持久化**：各模块的位置/缩放/透明度/颜色/排序/显隐保存在 `config/linclient/layout.json`，由编辑器实时写入
- **中英双语 i18n**：`en_us` / `zh_cn`

---

## 文件树

```
Minecraft-LinClient/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew / gradlew.bat
├── gradle/wrapper/gradle-wrapper.properties
├── .gitignore
├── LICENSE
├── README.md
├── .github/workflows/build.yml
└── src/main/
    ├── java/com/lindedaoqwq/linclient/
    │   ├── LinClient.java                 # @Mod 入口
    │   ├── config/
    │   │   ├── ModConfig.java             # Forge 原生配置（全局/性能/视觉开关）
    │   │   ├── LayoutConfig.java          # 模块布局 JSON（HUD 编辑器实时写入）
    │   │   └── KeyBindings.java           # 快捷键
    │   ├── hud/
    │   │   ├── HudModule.java             # 模块基类（渲染/布局）
    │   │   ├── HudOverlay.java            # 模块调度与排序渲染
    │   │   ├── HudEditorScreen.java       # 运行时 HUD 编辑器
    │   │   └── modules/                   # 8 个信息模块
    │   │       ├── SelfStatusModule.java
    │   │       ├── EntityModule.java
    │   │       ├── InputModule.java
    │   │       ├── EnvironmentModule.java
    │   │       ├── ItemModule.java
    │   │       ├── OtherModule.java
    │   │       ├── CombatModule.java
    │   │       └── BossModule.java
    │   ├── event/ClientEvents.java        # 渲染/刻度/输入/屏幕特效事件 + 视觉与性能开关
    │   ├── state/ClientState.java         # 共享运行时状态（CPS/速度/距离/AFK/连击/死亡坐标）
    │   └── util/                          # RenderUtils / I18n / Format / Rect
    └── resources/
        ├── META-INF/mods.toml
        ├── pack.mcmeta
        └── assets/linclient/lang/{en_us,zh_cn}.json
```

---

## 构建

### 本地构建
需要 **JDK 17** 与 **Gradle 8.5+**（或仓库内的 Gradle Wrapper）。

```bash
# 若已安装 Gradle 8.5+
gradle build

# 或使用 Wrapper（首次需先生成 jar：gradle wrapper --gradle-version 8.5，或直接用上面的 gradle 命令）
./gradlew build
```

构建产物：`build/libs/LinClient-1.0.0.jar`

### 运行 / 调试客户端
```bash
gradle runClient
```

### 放入游戏
将 `build/libs/LinClient-1.0.0.jar` 放进 `.minecraft/mods/` 即可（需安装对应版本 Forge）。

---

## 快捷键（游戏内「控制」中可重新绑定）

| 按键（默认） | 功能 |
| --- | --- |
| `H` | 切换 HUD 显示 |
| `G` | 打开 HUD 编辑器 |
| （自定义） | 启用 / 停用 LinClient |

### HUD 编辑器操作
打开后：
- **拖拽**模块移动位置
- **滚轮**缩放选中模块
- `H` 显隐选中模块
- `R` 重置选中模块到默认布局
- `[` / `]` 调整透明度
- `=` / `-` 调整缩放
- 方向键微调（按住 Shift 步进更大）
- `ESC` 关闭并保存

---

## 配置说明

1. **全局 / 性能 / 视觉开关**：编辑 `config/linclient-client.toml`，或用 Configured 等 Mod 在游戏内打开。
2. **模块布局**：`config/linclient/layout.json` 由各模块 `x/y/scale/opacity/color/order/enabled/alignRight` 组成，直接改文件或用手柄编辑器均可。
3. **语言**：跟随游戏语言设置（en_us / zh_cn）。

> 关于「保留接口」项（实体剔除 / 方块实体裁剪 / 粒子限制 / LOD / 区块节流 / 纹理动画限制 / 传送门覆盖）：这些功能需要向渲染管线注入 coremod/mixin。本项目刻意不包含会破坏可构建性的 mixin 代码；配置开关已预留，开发者可基于这些开关接入配套 coremod 实现物理裁剪/限制。已实装的视觉开关（全亮、天气、云、火焰/水/岩浆覆盖、动态帧率）均为干净、安全的客户端 API 调用。

---

## GitHub 自动化

仓库包含 `.github/workflows/build.yml`：
- `push` / `pull_request` 到 `main` 自动执行 `./gradlew build` 并上传 jar 为 artifact
- 推送 **tag**（`v1.0.0` 等形式）自动构建并创建 **Release**，附上 jar
- 已声明 `permissions: contents: write` 以允许发布 Release

Release 最新版下载地址：

```
https://github.com/lindedaoqwq/Minecraft-LinClient/releases/latest
```

---

## 许可证

[MIT](LICENSE) © lindedaoqwq
